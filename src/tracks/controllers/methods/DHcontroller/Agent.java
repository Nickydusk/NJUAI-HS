package tracks.controllers.methods.DHcontroller;

import core.game.Observation;
import core.game.StateObservation;
import core.player.AbstractPlayer;
import tools.ElapsedCpuTimer;
import tools.Vector2d;
import java.util.ArrayList;
import java.util.Random;
import ontology.Types.ACTIONS;
import ontology.Types.WINNER;
import java.util.Comparator;


public class Agent extends AbstractPlayer {
	ArrayList<AStarNode> frontier;
	ArrayList<AStarNode> searched;
	AStarNode root;
	double stepCost=100;
	int blocksz;
	ArrayList<ACTIONS> actions;
	Random rand;
	int width,height;

	// 创建agent
	public Agent(StateObservation stateObs, ElapsedCpuTimer elapsedTimer) {
		blocksz = stateObs.getBlockSize();
		actions = stateObs.getAvailableActions();
		rand = new Random(2020);
		width = stateObs.getObservationGrid().length;
		height = stateObs.getObservationGrid()[0].length;
		for (Observation obi :stateObs.getImmovablePositions()[0]) {
			int x = (int)obi.position.x/blocksz;
			int y = (int)obi.position.y/blocksz;
			while (y>=height) y--;
			while (y<0) y++;
			while (x>=width) x--;
			while (x<0) x++;
		}
	}

	// 执行一次行动
	@Override
	public ACTIONS act(StateObservation stateObs, ElapsedCpuTimer elapsedTimer) {
		frontier = new ArrayList<>();
		searched = new ArrayList<>();


		root = new AStarNode(stateObs,null, actions.get(rand.nextInt(actions.size())) ,0,0);
		frontier.add(root);


		// 搜索过程
		double avgTimeTaken = 0;
		double acumTimeTaken = 0;
		double worstTimeTaken = 0;
		long remaining = elapsedTimer.remainingTimeMillis();
		int numIters = 0;

		while(remaining > 2*avgTimeTaken && remaining > worstTimeTaken){
			ElapsedCpuTimer elapsedTimerIteration = new ElapsedCpuTimer();


			// 从前线选择最佳节点
			// 当遍历完全则提前结束
			if (frontier.size()==0) {
//                System.out.println(id+"  early stop");
				break;
			}
			frontier.sort(Comparator.comparingDouble(n -> n.f_value));
			AStarNode pick = frontier.remove(0);


			// 扩展节点
			for (ACTIONS a:actions) {
				StateObservation ob = pick.state.copy();

				ob.advance(a);


				// 没动的情况(TODO:排除由于对方撞墙导致自己不能动，在getOppAct中实现)
				if (eqState(ob,stateObs,false)) {
//                    System.out.println("No move");
				}
				// 节点已经搜索过
				else if (DiscoveredState(ob,searched)!=null) {
//                    System.out.println("Searched");
				}
				else {
					// 节点已被加入前线
					AStarNode node = DiscoveredState(ob, frontier);
					if (node!=null) {
//                      // 新f_value比原来小，更新父节点
						if (node.f_value > pick.g_value + stepCost + h(ob)) {
							node.update_father(pick,a, pick.g_value+stepCost,h(ob));
						}
						// 新f_Value不比原来小，则什么都不用做
					}
					// 全新节点，加入前线
					else {
						AStarNode tmpNode = new AStarNode(ob, pick, a, pick.g_value + stepCost, h(ob));
						frontier.add(tmpNode);
					}
				}

			}

			searched.add(pick);

			// 时间统计
			numIters++;
			acumTimeTaken += (elapsedTimerIteration.elapsedMillis());
			avgTimeTaken  = acumTimeTaken/numIters;
			remaining = elapsedTimer.remainingTimeMillis();
			if (elapsedTimerIteration.elapsedMillis()>worstTimeTaken)
				worstTimeTaken = elapsedTimerIteration.elapsedMillis();
		}

		System.out.println("Iterated:  "+numIters);


		// 决策过程
		ACTIONS best_act;
		if (frontier.size()>0) {
			best_act = getBestAct(frontier);
		}
		else if (searched.size()>0){
			best_act = getBestAct(searched);
		}
		else {
			best_act = actions.get(rand.nextInt(actions.size()));
			System.out.println(" RandomAct "+best_act.name());
		}

		return best_act;
	}

	int score_cost = -1000;
	// 启发式函数
	public double h(StateObservation ob) {
		// 0.确保尽量不让自己死掉
		if (ob.isGameOver() && ob.getGameWinner()== WINNER.PLAYER_LOSES) return Double.MAX_VALUE/2;
		double value = 0;
		// 1.游戏得分直接计入
		value += score_cost * ob.getGameScore();
		return rand.nextInt(10) + value;
	}

	// 获取nnpc对应编号的ghost与id对应编号Avatar的距离
	private int getNPCdis(StateObservation ob, int xAvatar, int yAvatar, int mindis, int nnpc) {
		int xnpc = (int) ob.getNPCPositions()[nnpc].get(0).position.x / blocksz;
		int ynpc = (int) ob.getNPCPositions()[nnpc].get(0).position.y / blocksz;
		int distance = Math.min(Math.abs(xAvatar - xnpc),width-Math.abs(xAvatar-xnpc)) + Math.min(Math.abs(yAvatar - ynpc),height-Math.abs(yAvatar-ynpc));
		if (distance < mindis)
			mindis = distance;
		return mindis;
	}

	// 从某一列表中获得最佳行动
	private ACTIONS getBestAct(ArrayList<AStarNode> list) {
		ACTIONS best_act;
		list.sort(Comparator.comparingDouble(n -> n.f_value));
		AStarNode tmpNode = list.get(0);
		best_act = tmpNode.act;
		while (tmpNode.father != null) {
			best_act = tmpNode.act;
			tmpNode = tmpNode.father;
		}
		System.out.println("BestAct "+best_act.name());
		return best_act;
	}

	// 判断两个状态是否相同，若player为-1则判断完整状态，若传递player则只看他的位置是否变化
	public boolean eqState(StateObservation ob1, StateObservation ob2, boolean local) {
		// 判断全局是否变化
		if (!local) {
			if (ob1.getImmovablePositions().length!=ob2.getImmovablePositions().length) return false;
			else
				for (int j=0;j<ob1.getImmovablePositions().length;j++) {
					if (ob1.getImmovablePositions()[j].size() != ob2.getImmovablePositions()[j].size()) {
						return false;
					}
				}
			Vector2d pos1 = ob1.getAvatarPosition();
			Vector2d pos2 = ob2.getAvatarPosition();
			return pos1.equals(pos2);
		}
		// 判断某个Avatar位置单步是否改变
		else {
			Vector2d pos1 = ob1.getAvatarPosition();
			Vector2d pos2 = ob2.getAvatarPosition();
			return pos1.equals(pos2);
		}
	}

	// 判断某个状态是否已经在某个list中
	public AStarNode DiscoveredState(StateObservation ob, ArrayList<AStarNode> l) {
		for (AStarNode node :l) {
			if (eqState(node.state,ob,false)) {
				return node;
			}
		}
		return null;
	}

	// 打印状态的某些信息
	public void printState(StateObservation ob) {
//        System.out.println(stateObs.getAvatarType(0)+" "+stateObs.getAvatarType(1));

//        System.out.println(id+": "+stateObs.getPortalsPositions()[0]); // 奇怪的位置（推测为不存在的“门”）
//        System.out.println(id+": "+stateObs.getMovablePositions()); // 始终为null
//        System.out.println(id+": "+stateObs.getResourcesPositions()); // 始终为null
//        System.out.println(id+": "+stateObs.getFromAvatarSpritesPositions()); // 始终为null
//        System.out.println(id+": "+stateObs.getGameState()); // 始终为null


//        System.out.println(id+": "+stateObs.getImmovablePositions()[0]); // 墙体
//        System.out.println(id+": "+stateObs.getImmovablePositions()[1]); // 地板
//        System.out.println(id+": "+stateObs.getImmovablePositions()[2]); // fruit
//        System.out.println(id+": "+stateObs.getImmovablePositions()[3]); // pellet
//        System.out.println(id+": "+stateObs.getImmovablePositions()[4]); // power

//        System.out.println(id+": "+stateObs.getNPCPositions().length); // 初次访问为空，之后为4个ghost的位置
//        System.out.println(stateObs.getAvatarPosition(id)); // 对应id Avatar位置，缺省为0号Avatar


//        stateObs.getObservationGrid();
		System.out.print("("+(int)ob.getAvatarPosition().x/blocksz+","+(int)ob.getAvatarPosition().y/blocksz+") ");
	}
}