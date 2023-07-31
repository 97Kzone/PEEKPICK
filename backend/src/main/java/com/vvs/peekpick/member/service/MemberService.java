package com.vvs.peekpick.member.service;

import com.vvs.peekpick.entity.*;
import com.vvs.peekpick.global.auth.Token;
import com.vvs.peekpick.member.dto.AvatarDto;
import com.vvs.peekpick.member.dto.SignUpDto;

import java.util.List;


public interface MemberService {
    Token signup(SignUpDto signUpDto);
    Emoji RandomEmoji();
    Prefix RandomPrefix();

    List<World> RandomWorld();

    Member getMemberInfo(Long memberId);

    List<String> categoryList();

    List<Category> detailCategoryList(String categoryLarge);

    AvatarDto getAvatarInfo(Long avatarId);
}
