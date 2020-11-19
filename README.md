## 使用启发式搜索玩单人游戏的agent

- 自己实现的算法位于：`NJUAI-HS\src\tracks\controllers\methods\DHcontroller\`，共有`Agent.java`,`AStarNode.java`两个文件。前者用于实现Astar搜索，后者为Astar搜索的节点。

- 算法基于$A^*$框架，在每一轮决策的过程中逐渐展开临近状态，最终挑选启发式函数+路径代价之和最小的叶子结点对应的父辈动作作为本次动作返回。

- 启发式函数部分如下，首先确保自己尽可能不要输掉游戏，其次直接考虑游戏得分作为启发依据，最后在value上加入一个小的随机数以防不同value对应同一个value而产生归纳偏好。

  ```java
  int score_cost = -1000;
  // 启发式函数
  public double h(StateObservation ob) {
  	// 0.确保尽量不让自己死掉
  	if (ob.isGameOver() && ob.getGameWinner()== WINNER.PLAYER_LOSES)
          return Double.MAX_VALUE/2;
  		double value = 0;
  	// 1.游戏得分直接计入
  	value += score_cost * ob.getGameScore();
  	return rand.nextInt(10) + value;
  }
  ```

- 由于使用的启发较为简单，如果无法产生分数改变则无法对不同动作产生区分，最终的效果在很多情况下与随机算法等效。等到时间充沛时也许会继续完善这些算法。