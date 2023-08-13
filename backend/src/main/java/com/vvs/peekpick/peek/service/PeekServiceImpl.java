package com.vvs.peekpick.peek.service;

import com.vvs.peekpick.entity.*;
import com.vvs.peekpick.exception.CustomException;
import com.vvs.peekpick.exception.ExceptionStatus;
import com.vvs.peekpick.member.service.MemberServiceImpl;
import com.vvs.peekpick.peek.dto.*;
import com.vvs.peekpick.peek.repository.PeekRepository;
import com.vvs.peekpick.response.CommonResponse;
import com.vvs.peekpick.response.DataResponse;
import com.vvs.peekpick.response.ResponseService;
import com.vvs.peekpick.response.ResponseStatus;
import com.vvs.peekpick.wordFilter.BadWordFiltering;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.geo.*;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static com.vvs.peekpick.peek.service.DistanceCalService.calculateDistance;

@Slf4j
@Service
@RequiredArgsConstructor
public class PeekServiceImpl implements PeekService {
    private final int MAX_PEEK = 8; // 화면 단에 전닿해주는 Peek 수 (이벤트 코드)
    //private final int MAX_PEEK = 10; // 화면 단에 전닿해주는 Peek 수
    private final int PEEK_ORIGIN_TIME = 1440; // PEEK 기본 지속 시간 (분) (이벤트 코드)
    //private final int PEEK_ORIGIN_TIME = 60; // PEEK 기본 지속 시간 (분)
    private final int PEEK_REACTION_TIME = 10; // 좋아요, 싫어요 시 증가되는 시간 (분)
    private final int PEEK_MAX_HOUR = 72; // Peek 최대 지속 시간 (시간) (이벤트 코드)
    //private final int PEEK_MAX_HOUR = 24; // Peek 최대 지속 시간 (시간)
    private final Random random = new Random();
    private final ResponseService responseService;
    private final PeekMemberService peekMemberService;
    private final PeekRdbService peekRdbService;
    private final PeekAvatarService peekAvatarService;
    private final PeekRedisService peekRedisService;
    private final  MemberServiceImpl memberService;
    private final BadWordFiltering filtering = new BadWordFiltering("♡");

    @Override
    public DataResponse findNearPeek(Long memberId, RequestSearchPeekDto requestSearchPeekDto) {
        try {
            // 반경 m로 원 생성
            Circle circle = new Circle(requestSearchPeekDto.getPoint(), new Distance(requestSearchPeekDto.getDistance()));

            // 해당 원 안에 위치하는 PeekLocation 값들
            List<PeekNearSearchDto> nearPeeks = peekRedisService.getNearLocation(circle.getCenter(), circle.getRadius().getValue());

            // 모든 값 가져온 뒤
            List<ResponsePeekListDto> allPeeks = new ArrayList<>();
            for (PeekNearSearchDto nearPeek: nearPeeks) {
                Long peekId = Long.parseLong(nearPeek.getPeekId());
                int distance = nearPeek.getDistance();
                PeekRedisDto peekRedisDto = peekRedisService.getPeekValueOps(peekId);
                boolean isViewed = peekRedisService.getViewdByMember(memberId, peekId);
                // 개발 완료 시 아래 로직을 여기 감싸줄 예정
//                if(peekRedisDto.isSpecial() | !isViewed) {
//
//                }
                ResponsePeekListDto responsePeekListDto = ResponsePeekListDto.builder()
                        .peekId(peekRedisDto.getPeekId())
                        .distance(distance)
                        .special(peekRedisDto.isSpecial())
                        .viewed(isViewed)
                        .admin(peekRedisDto.getMemberId()==1L)
                        .build();
                allPeeks.add(responsePeekListDto);
            }
            System.out.println(allPeeks);
            if(allPeeks.size() == 0 ) return responseService.successDataResponse(ResponseStatus.LOADING_PEEK_LIST_SUCCESS_NO_PEEK, allPeeks);


            // 랜덤 추출 (max 보다 적게 있는 경우 있는대로만 가져옴)
            List<ResponsePeekListDto> randomPeeks = new ArrayList<>();

            // 관리자 이벤트 Peek 3개 무조건 추가 (이벤트 코드)
            List<Peek> adminPeeks = peekRdbService.findPeeksByMemberId(1L);
            for(Peek peek : adminPeeks) {
                Point peekPoint = peekRedisService.getPeekLocation(peek.getPeekId());
                ResponsePeekListDto responsePeekListDto = ResponsePeekListDto.builder()
                        .peekId(peek.getPeekId())
                        .distance(1)//(int) calculateDistance(peekPoint.getX(), peekPoint.getY(), requestSearchPeekDto.getPoint().getX(), requestSearchPeekDto.getPoint().getY()))
                        .special(false)
                        .viewed(false)
                        .admin(true)
                        .build();
                randomPeeks.add(responsePeekListDto);
            }

            while(randomPeeks.size()==MAX_PEEK) {
                int randomIndex = random.nextInt(allPeeks.size());
                if(peekRedisService.getPeek(allPeeks.get(randomIndex).getPeekId()).getMemberId()!=1L)
                {
                    randomPeeks.add(allPeeks.get(randomIndex));
                    allPeeks.remove(randomIndex);
                }
            }

            // allPeeks에서 관리자가 작성자가 아닌 것들만 필터링하고 랜덤으로 섞기
//            List<ResponsePeekListDto> nonAdminPeeks = allPeeks.stream()
//                    .filter(peek -> !peek.isAdmin())
//                    .collect(Collectors.toList());
//            Collections.shuffle(nonAdminPeeks);
//
//            // randomPeeks에 추가할 수 있는 남은 개수 계산
//            int remainingPeeks = MAX_PEEK - randomPeeks.size();
//
//            // 남은 개수만큼 randomPeeks에 추가
//            for (int i = 0; i < Math.min(remainingPeeks, nonAdminPeeks.size()); i++) {
//                randomPeeks.add(nonAdminPeeks.get(i));
//            }

//            if(allPeeks.size() <= MAX_PEEK){
//                randomPeeks = allPeeks;
//            } else {
//                randomPeeks = new ArrayList<>();
//                for (int i = 0; i < MAX_PEEK; i++) {
//                    int randomIndex = random.nextInt(allPeeks.size());
//                    randomPeeks.add(allPeeks.get(randomIndex));
//                    allPeeks.remove(randomIndex);
//                }
//            }


            System.out.println(randomPeeks);
            return responseService.successDataResponse(ResponseStatus.LOADING_PEEK_LIST_SUCCESS, randomPeeks);
        }
        catch (Exception e) {
            log.info("내 주변 Peek 예외 처리 : {}", e);
            return responseService.failureDataResponse(ResponseStatus.PEEK_FAILURE, null);
        }
    }

    @Override
    public CommonResponse addPeek(Long memberId, RequestPeekDto requestPeekDto, String imageUrl) {
        try {
            Member writer = peekMemberService.findMember(memberId);
            String afterFiltering = filtering.changeAll(requestPeekDto.getContent());
            LocalDateTime time = LocalDateTime.now();
            //RDB에 저장하기 위한 Peek 객체
            Peek peek = Peek.builder()
                    .member(writer)
                    .content(afterFiltering)
                    .disLikeCount(0)
                    .likeCount(0)
                    .imageUrl(imageUrl)
                    .writeTime(time)
                    .build();


            //RDB에 Peek 저장 후 id 값 받아옴
            Long peekId = peekRdbService.savePeek(peek);


            //redis에 저장하기 위한 Peek 객체 생성
            PeekRedisDto peekRedisDto = PeekRedisDto.builder()
                    .peekId(peekId)
                    .memberId(writer.getMemberId())
                    .content(afterFiltering)
                    .imageUrl(imageUrl)
                    .likeCount(0)
                    .disLikeCount(0)
                    .writeTime(time)
                    .finishTime(time.plusMinutes(PEEK_ORIGIN_TIME))
                    .special(false)
                    .viewed(false)
                    .build();
            log.info("작성 시간 : {}", time);
            log.info("위도 {}", requestPeekDto.getLongitude());
            log.info("경도 {}",requestPeekDto.getLatitude());
            //redis에 Peek Location 값 저장
            peekRedisService.setPeekLocation(requestPeekDto.getLongitude(), requestPeekDto.getLatitude(), peekId);
            //redis에 Peek 저장 & ttl 설정
            peekRedisService.setPeek(peekRedisDto, peekId, PEEK_ORIGIN_TIME);

            return responseService.successCommonResponse(ResponseStatus.ADD_SUCCESS);
        }
        catch (Exception e) {
            log.info("입력 예외 처리 : {}", e.getMessage());
            return responseService.failureCommonResponse(ResponseStatus.PEEK_FAILURE);
        }
    }


    @Override
    public DataResponse getPeek(Long avatarId, Long memberId, Long peekId, int distance) {
        try{
            // Redis에서 Peek 가져오기
            PeekRedisDto peekRedisDto = peekRedisService.getPeek(peekId);

            // 해당 Peek가 만료되었을 때
            if(peekRedisDto==null) return responseService.failureDataResponse(ResponseStatus.PEEK_EXPIRED, null);


            // 현재 사용자가 해당 Peek을 본 것으로 처리
            peekRedisService.setViewedByMember(memberId, peekId);

            // 현재 사용자의 해당 Peek의 좋아요 / 싫어요 여부 판별
            boolean isLiked= peekRedisService.getReactionMember(memberId, true, peekId);
            boolean isDisLiked = peekRedisService.getReactionMember(memberId, false, peekId);

            // 응답해줄 Peek 객체
            PeekDetailDto peekDetailDto = PeekDetailDto.builder()
                    .peekId(peekRedisDto.getPeekId())
                    .content(peekRedisDto.getContent())
                    .imageUrl(peekRedisDto.getImageUrl())
                    .likeCount(peekRedisDto.getLikeCount())
                    .disLikeCount(peekRedisDto.getDisLikeCount())
                    .finishTime(peekRedisDto.getFinishTime())
                    .liked(isLiked)
                    .disLiked(isDisLiked)
                    .distance(distance)
                    .build();
            Member writer = peekMemberService.findMember(peekRedisDto.getMemberId());

            Avatar avatar = peekAvatarService.findAvatar(writer.getAvatar().getAvatarId());
            PeekAvatarDto peekAvatarDto = PeekAvatarDto.builder()
                    .avatarId(avatarId)
                    .nickname(avatar.getNickname())
                    .bio(avatar.getBio())
                    .emoji(avatar.getEmoji())
                    .prefix(avatar.getPrefix())
                    .build();

            ResponsePeekDto responsePeekDto = ResponsePeekDto.builder()
                    .peekDetailDto(peekDetailDto)
                    .peekAvatarDto(peekAvatarDto)
                    .build();

            return responseService.successDataResponse(ResponseStatus.LOADING_PEEK_SUCCESS, responsePeekDto);
        }
        catch (Exception e) {
            e.printStackTrace();
            return responseService.failureDataResponse(ResponseStatus.PEEK_FAILURE, null);
        }
    }

    @Override
    public CommonResponse deletePeek(Long memberId, Long peekId) {
        try {
            Long writerId = peekRdbService.findPeek(peekId).getMember().getMemberId();

            // writerId와 memberId가 동일한지 확인
            if(writerId != memberId) {
                return responseService.failureCommonResponse(ResponseStatus.DELETE_FAILURE);
            }

            // redis에서 Peek & 관련 Key 삭제 / 좋아요, 싫어요 수 가져오기
            PeekReactionCntDto peekReactionCntDto = peekRedisService.deletePeek(peekId);
            memberService.updateLikeDisLikeCount(memberId, peekReactionCntDto.getDisLikeCnt(), peekReactionCntDto.getDisLikeCnt());
            // rdb에 좋아요, 싫어요 수 update
            peekRdbService.updatePeek(peekId, peekReactionCntDto.getLikeCnt(), peekReactionCntDto.getDisLikeCnt());

            return responseService.successCommonResponse(ResponseStatus.DELETE_SUCCESS);
        }
        catch (Exception e) {
            return responseService.failureCommonResponse(ResponseStatus.PEEK_FAILURE);
        }
    }

    @Override
    public CommonResponse deletePeekExpired(Long peekId) {
        try {
            Long writerId = peekRdbService.findPeek(peekId).getMember().getMemberId();

            // redis에서 Peek & 관련 Key 삭제 / 좋아요, 싫어요 수 가져오기
            PeekReactionCntDto peekReactionCntDto = peekRedisService.deletePeek(peekId);
            memberService.updateLikeDisLikeCount(writerId, peekReactionCntDto.getDisLikeCnt(), peekReactionCntDto.getDisLikeCnt());

            // rdb에 좋아요, 싫어요 수 update
            peekRdbService.updatePeek(peekId, peekReactionCntDto.getLikeCnt(), peekReactionCntDto.getDisLikeCnt());
            
            return responseService.successCommonResponse(ResponseStatus.DELETE_SUCCESS);
        }
        catch (Exception e) {
            return responseService.failureCommonResponse(ResponseStatus.PEEK_FAILURE);
        }
    }

    @Override
    public CommonResponse addReaction(Long memberId, Long peekId, boolean like) {
        try {
            // Redis에서 Peek 가져오기
            PeekRedisDto peekRedisDto = peekRedisService.getPeek(peekId);

            // 해당 Peek가 만료되었을 때
            if(peekRedisDto==null) return responseService.failureDataResponse(ResponseStatus.PEEK_EXPIRED, null);

            LocalDateTime updatedFinishTime = peekRedisDto.getFinishTime(); //해당 Peek의 종료 시간
            boolean special = peekRedisDto.isSpecial(); //Hot Peek 여부

            int likeCnt = peekRedisDto.getLikeCount();
            int disLikeCnt = peekRedisDto.getDisLikeCount();


            //사용가 해당 Peek의 react를 On -> Off
            if (peekRedisService.getReactionMember(memberId, like, peekId)) {
                peekRedisService.setPeekReactionOff(memberId, like, peekId);
                if (peekRedisDto.getFinishTime().minusMinutes(PEEK_REACTION_TIME).isAfter(LocalDateTime.now().plusMinutes(1))) {
                    updatedFinishTime = peekRedisDto.getFinishTime().minusMinutes(PEEK_REACTION_TIME);
                }
                if(like) likeCnt--;
                else disLikeCnt--;
            }
            //사용가 해당 Peek의 react를 Off -> On
            else {
                peekRedisService.setPeekReactionOn(memberId, like, peekId);
                    updatedFinishTime = peekRedisDto.getFinishTime().plusMinutes(PEEK_REACTION_TIME);
                if(like) likeCnt++;
                else disLikeCnt++;
            }

//            // (원래 기획) Peek 지속시간을 24시간으로 제한, 24시간 설정된 Peek은 Hot Peek으로
//            if (special || Duration.between(peekRedisDto.getWriteTime(), updatedFinishTime).toHours() >= PEEK_MAX_HOUR) {
//                updatedFinishTime = peekRedisDto.getWriteTime().plusHours(PEEK_MAX_HOUR);
//                special = true;
//            }

            // (임시) Peek 지속시간을 24시간으로 제한,
            // 지속 시간 80분으로(좋아요 싫어요 총 2개 이상) 설정된 Peek은 Hot Peek으로
            if (Duration.between(peekRedisDto.getWriteTime(), updatedFinishTime).toMinutes() >= 80) {
                special = true;
            }
            if (Duration.between(peekRedisDto.getWriteTime(), updatedFinishTime).toHours() >= PEEK_MAX_HOUR) {
                updatedFinishTime = peekRedisDto.getWriteTime().plusHours(PEEK_MAX_HOUR);
            }

            //Redis에 Peek Update
            PeekRedisDto updatedPeekRedisDto = peekRedisDto.toBuilder()
                    .likeCount(likeCnt)
                    .disLikeCount(disLikeCnt)
                    .finishTime(updatedFinishTime)
                    .special(special)
                    .build();
            Long ttl = peekRedisService.getPeekTtl(peekId);  // 기존 TTL 가져오기
            if (ttl != null && ttl > 0) {
                ttl += 60*PEEK_REACTION_TIME;  //초 단위로 변경
            }
            peekRedisService.setPeekValueOps(peekId, updatedPeekRedisDto, ttl);
            return responseService.successCommonResponse(ResponseStatus.ADD_REACTION_SUCCESS);
        } catch (Exception e) {
            return responseService.failureCommonResponse(ResponseStatus.PEEK_FAILURE);
        }
    }

}
