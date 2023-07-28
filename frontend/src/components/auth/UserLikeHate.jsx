import axios from 'axios';

import { useEffect } from 'react';
import { useSelector } from 'react-redux';
import { useNavigate } from 'react-router-dom';

import common from './style/Common.module.css';
import classes from './style/UserLikeHate.module.css';

const UserLikeHate = () => {
  // 페이지 렌더링 시
  const userInfo = useSelector(state => state.auth);

  useEffect(() => {
    console.log(userInfo)
  }, [userInfo]);

  // 함수 생성
  const navigate = useNavigate();

  const moveToUserLike = () => {
    navigate('/userlike')
  }

  const moveToUserHate = () => {
    navigate('/userhate')
  }

  const moveToWelcome = () => {
    const dataToSend = {
      id: userInfo.id,
      name: userInfo.name,
      email: userInfo.email,
      phone: userInfo.phone,
      birthday: userInfo.birthday,
      gender: userInfo.gender,
      emojiId: userInfo.emojiId,
      prefixId: userInfo.prefixId,
      nickname: userInfo.nickname,
      likes: userInfo.likes,
      dislikes: userInfo.dislikes,
    }
  
  axios.post('http://192.168.31.26:8081/member/signup', dataToSend)
    .then(response => {
      console.log(response)
    })
    .catch(error => {
      console.log(error)
    })

    navigate('/welcome')
  }

  return (
    <div>
      <div>
        <h1>취향 PICK</h1>
      </div>
      <div>
        <p>
          무엇을 좋아하시나요?
          <br />
          또, 무엇을 싫어하시나요?
        </p>
      </div>
      <div className={common.line}></div>
      <div>
        <h3>좋아하는 것</h3>
        <div>
          {userInfo.likes}
        </div>
        <button onClick={moveToUserLike} className={classes.addButton}>+추가</button>
      </div>
      <div>
        <h3>싫어하는 것</h3>
        <div>
          {userInfo.dislikes}
        </div>
        <button onClick={moveToUserHate} className={classes.addButton}>+추가</button>
      </div>
      <div>
        <button onClick={moveToWelcome} className={common.next}>다음으로</button>
      </div>
    </div>
  );
};

export default UserLikeHate;