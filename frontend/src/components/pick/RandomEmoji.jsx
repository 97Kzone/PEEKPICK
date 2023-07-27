import classes from "./RandomEmoji.module.css";

function RandomEmoji({ EmojiData, Loading }) {
  if (Loading) {
    return <div>Loading...</div>;
  }

  if (EmojiData === null) {
    return <div>��...!!!</div>;
  }

  const gridSize = 7; // ���ο� ���ο� 10ĭ�� �� 100ĭ
  const cellSize = `${100 / gridSize}%`;

  const getRandomIndexes = (max, count) => {
    const indexes = Array.from({ length: max }, (_, i) => i);
    const shuffledIndexes = shuffleArray(indexes);
    return shuffledIndexes.slice(0, count);
  };

  const randomIndexes = getRandomIndexes(gridSize * gridSize, 10);

  return (
    <div
      className={classes.emojiArea}
      style={{ display: "grid", gridTemplateColumns: `repeat(${gridSize}, ${cellSize})` }}
    >
      {/* 100ĭ���� ���ҵ� div �ȿ� ������ 5�� ������ ��ġ */}
      {Array.from({ length: gridSize * gridSize }).map((_, index) => (
        <div key={index}>
          {/* ������ 5���� ������ limitedData.id ��ġ */}
          {randomIndexes.includes(index) ? EmojiData[Math.floor(Math.random() * EmojiData.length)].id : null}
        </div>
      ))}
    </div>
  );
}
// Fisher-Yates (Knuth) Shufflem
const shuffleArray = (array) => {
  for (let i = array.length - 1; i > 0; i--) {
    const j = Math.floor(Math.random() * (i + 1));
    [array[i], array[j]] = [array[j], array[i]];
  }
  return array;
};
export default RandomEmoji;
