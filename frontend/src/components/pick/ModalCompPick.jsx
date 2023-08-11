import Modal from "react-modal";
import { useSelector, useDispatch } from "react-redux";
import { modalActions } from "../../store/modalSlice";
import { chatActions } from "../../store/chatSlice";
import classes from "./ModalComp.module.css";
import { customAxios } from "../../api/customAxios";

const ModalComp = () => {
  //유져 정보 모달용
  const dispatch = useDispatch();
  const isModalState = useSelector((state) => state.modal.isOpen);
  const isSelectedEmoji = useSelector((state) => state.modal.selectedEmoji);

  const handleCloseModal = () => {
    dispatch(modalActions.closeModal());
    dispatch(chatActions.updateURL(isSelectedEmoji));
  };
  //채팅요청
  const plzChat = () => {
    customAxios.get(`/picker/chat-request/${isSelectedEmoji.avatarId}`).then((response) => {
      console.log(response.data.message);
    });
  };
  return (
    <>
      {isModalState && isSelectedEmoji && (
        <Modal
          isOpen={isModalState}
          onRequestClose={() => handleCloseModal()} // 모달 바깥을 클릭하거나 ESC 키를 누르면 모달을 닫음
          contentLabel="Selected Emoji Modal"
          className={classes.modalMain}
        >
          {/* 모달 내용에 선택된 avatarId를 표시 */}
          <div className={classes.modalHead}>
            <img src={isSelectedEmoji.emoji.animatedImageUrl} alt="프로필" className={classes.profileImg} />
            <div className={classes.modalHeadText}>
              <span className={classes.nickname}>
                {isSelectedEmoji.prefix.content} {isSelectedEmoji.nickname}
              </span>
              <span style={{ marginRight: "0.2rem" }}>PICK</span>
              <span style={{ color: "#7d00ff", fontWeight: "700" }}>100</span>
              <span style={{ marginLeft: "0.2rem" }}>회</span>
              {isSelectedEmoji.bio && isSelectedEmoji.bio.trim() !== "" ? (
                <p className={classes.intro}>{isSelectedEmoji.bio}</p>
              ) : (
                <p className={classes.intro}>내용이 없습니다.</p>
              )}
            </div>
          </div>
          <div className={classes.divider}></div>
          <div className={classes.modalBody}>
            <div>
              <p className={classes.like}>좋아!</p>
              <p className={classes.itemWrap}>
                {isSelectedEmoji.likes.map((like, index) => (
                  <div key={index} className={classes.items}>
                    #{like.middle}
                  </div>
                ))}
              </p>
            </div>
            <div>
              <p className={classes.disLike}>싫어!</p>
              <p className={classes.itemWrap}>
                {isSelectedEmoji.disLikes.map((disLikes, index) => (
                  <span key={index} className={classes.items}>
                    #{disLikes.middle}
                  </span>
                ))}
              </p>
            </div>
          </div>
          <div className={classes.pickWrap}>
            <button className={classes.pick} onClick={() => plzChat()}>
              PICK
            </button>
          </div>
        </Modal>
      )}
    </>
  );
};

export default ModalComp;
