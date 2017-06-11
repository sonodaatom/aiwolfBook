package book;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.aiwolf.client.lib.Content;
import org.aiwolf.client.lib.Topic;
import org.aiwolf.common.data.Agent;
import org.aiwolf.common.data.Role;
import org.aiwolf.common.data.Species;
import org.aiwolf.common.data.Status;
import org.aiwolf.common.data.Talk;
import org.aiwolf.common.net.GameInfo;
import org.aiwolf.common.net.GameSetting;
import org.aiwolf.sample.lib.AbstractVillager;

public class villager extends AbstractVillager {

	@Override
	public void dayStart() {
		//特徴量の取得
		getInfo();
		//対戦相手の判別
		if (latestGameInfo.getDay() > 0) {
			try {
				setSVM();
				for(Agent agent:svmMap.keySet()){
					System.out.println(String.valueOf(agent)+":"+String.valueOf(svmMap.get(agent)));
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		// 各エージェントの生死の更新
		setDeadOrAlive();
		// 初期化
		readTalkNum = 0;
		votingMap = new HashMap<Agent, Agent>();
		for (Agent agt : latestGameInfo.getAliveAgentList()) {
			votingMap.put(agt, null);
		}

	}

	@Override
	public void finish() {
		

	}

	@Override
	public String getName() {
		
		return null;
	}

	@Override
	public void initialize(GameInfo gameInfo, GameSetting gameSetting) {
		try {
			//学習したモデルの読み込み
			setModel();
		} catch (IOException e) {
			e.printStackTrace();
		}
		latestGameInfo = gameInfo;
		initDeadOrAlive();

	}

	@Override
	public String talk() {
		
		return null;
	}

	@Override
	public void update(GameInfo gameInfo) {
		// 計測したい処理を記述
		latestGameInfo = gameInfo;
		// 今日のログを取得
		List<Talk> talkList = latestGameInfo.getTalkList();
		// voting,votedMapの更新
		setVoteMap(talkList);
		// 各エージェントのCOの更新
		setCO(talkList);
		// 各CO占い師,霊媒師による人狼、人間mapの更新
		setWolfHumanMap(talkList);
		// readTalkNumの更新
		readTalkNum = talkList.size();
	
		

	}

	@Override
	public Agent vote() {
		
		return null;
	}
	
	//ゲーム情報
		public GameInfo latestGameInfo;
		// エージェントごとの情報データ
		Map<Agent, Integer[]> infoMap = new HashMap<>();
		// エージェントごとに-1（人狼）判定された回数を保存
		Map<Agent, Integer> svmMap = new HashMap<>();
		// 投票予定map
		Map<Agent, Agent> votingMap;
		private void getInfo() {
			if (latestGameInfo.getDay() > 0) {

				for (Agent agent : latestGameInfo.getAgentList()) {

					Integer[] info = new Integer[11];
					//日にち　すべてのiで共通 status
					info[10] = latestGameInfo.getDay() - 1;
					//生きてるなら０、死んでるなら１ status
					if (AliveOrDeadMap.get(agent) == Status.ALIVE) {
						info[0] = 0;
					} else {
						info[0] = 1;
					}
					//CO占い師数 すべてのiで共通 talk
					info[1] = SeerCOAgent.size();
					//CO霊媒師数　すべてのiで共通 talk
					info[2] = MediumCOAgent.size();
					//受けた占い人間判定数 talk
					info[3] = 0;
					//受けた占い人狼判定数 talk
					info[4] = 0;
					for (Agent seer : SeerCOAgent) {
						if (humanAgentMap.get(seer).contains(agent)) {
							info[3]++;
						}
						if (wolfAgentMap.get(seer).contains(agent)) {
							info[4]++;
						}
					}
					//VOTE発話から投票変更 talk & vote
					if (!latestGameInfo.getVoteList().isEmpty()) {
						if (AliveOrDeadMap.get(agent) == Status.ALIVE) {
							for (int i = 0; i < latestGameInfo.getVoteList().size(); i++) {
								if (latestGameInfo.getVoteList().get(i).getAgent() == agent) {
									//投票変更したなら
									if (latestGameInfo.getVoteList().get(i)
											.getTarget() != votingMap.get(agent)) {
										info[9] = infoMap.get(agent)[9] + 1;
									} else {
										info[9] = infoMap.get(agent)[9];
									}
								}
							}
						} else {
							info[9] = infoMap.get(agent)[9];
						}

					} else {
						info[9] = 0;
					}


					//何番目に占い師CO talk
					info[5] = 0;
					//何番目に霊媒師CO talk
					info[6] = 0;
					//占い師で人間判定を出した数 talk
					info[7] = 0;
					//占い師で人狼判定を出した数 talk
					info[8] = 0;
					if (SeerCOAgent.contains(agent)) {
						info[5] = orderSeerCO.get(agent);
						info[7] = humanAgentMap.get(agent).size();
						info[8] = wolfAgentMap.get(agent).size();
					}

					if (MediumCOAgent.contains(agent)) {
						info[6] = orderMediumCO.get(agent);
					}
					//infoMapを更新
					infoMap.put(agent, info);
				}

			}
		}

		// モデルのロード
		svm_model model;
		private void setModel() throws IOException {
			model = svm.svm_load_model("./log.model");
		}

		//対戦相手の判別
		private void setSVM() throws Exception {

			for (Agent agt : latestGameInfo.getAgentList()) {

				int node = 11;

				// 判別対象にしたいデータ
				svm_node[] input = new svm_node[node];
				for (int i = 0; i < node; i++) {
					input[i] = new svm_node();
				}
				// ラベルをセット
				for (int i = 0; i < node; i++) {
					input[i].index = i + 1;
				}

				// 値をセット
				for (int j = 0; j < node; j++) {
					input[j].value = infoMap.get(agt)[j];
					System.out.println("成功"+input[j].value);
				}

				// 判別の実行
				double v=0;
				try {
					v = svm.svm_predict(model, input);
				} catch (Exception e) {
					System.out.println("判別エラー");
				}


				if (v == -1) {
					System.out.println("人狼判定");
					Integer in = svmMap.get(agt);
					int i;
					if (in == null) {
						i = 0;
					} else {
						i = in.intValue();
					}
					i++;
					in = Integer.valueOf(i);
					svmMap.put(agt, in);
				}
			}

		}

		// 各エージェントの生死
		Map<Agent, Status> AliveOrDeadMap = new HashMap<Agent, Status>();
		private void initDeadOrAlive() {
			for (Agent agt : latestGameInfo.getAgentList()) {
				AliveOrDeadMap.put(agt, Status.ALIVE);
			}
		}
		private void setDeadOrAlive() {
			Agent excutedAgent = latestGameInfo.getExecutedAgent();
			if (excutedAgent != null) {
				AliveOrDeadMap.put(excutedAgent, Status.DEAD);
			}
			List<Agent> deadAgentList = latestGameInfo.getLastDeadAgentList();//修正
			for( Agent agt : deadAgentList ){
				AliveOrDeadMap.put(agt, Status.DEAD);
			}
		}

		// カミングアウトしているプレイヤーのリスト
		Set<Agent> SeerCOAgent = new HashSet<Agent>();
		Set<Agent> MediumCOAgent = new HashSet<Agent>();
		// 占い師COエージェントごとの人狼・人間判定
		Map<Agent, Set<Agent>> wolfAgentMap = new HashMap<Agent, Set<Agent>>();
		Map<Agent, Set<Agent>> humanAgentMap = new HashMap<Agent, Set<Agent>>();
		Map<Agent, Set<Agent>> wolfAgentMapByMedium = new HashMap<Agent, Set<Agent>>();
		Map<Agent, Set<Agent>> humanAgentMapByMedium = new HashMap<Agent, Set<Agent>>();
		// 占い師、霊媒師COした順番
		Map<Agent, Integer> orderSeerCO = new HashMap<>();
		Map<Agent, Integer> orderMediumCO = new HashMap<>();
		// その日のログの何番目まで読み込んだか
		int readTalkNum;

		private void setCO(List<Talk> talkList) {
			//自分が生きていれば
			if (latestGameInfo.getAliveAgentList().contains(
					latestGameInfo.getAgent())) {
				for (int i = readTalkNum; i < talkList.size(); i++) {
					Talk talk = talkList.get(i);
					// 発話をパース
					Content content = new Content(talk.getText());//修正

					Agent subject = talk.getAgent();
					Agent target = content.getTarget();//修正
					Role COrole = content.getRole();//修正
					Set<Agent> wolf = new HashSet<Agent>();
					Set<Agent> human = new HashSet<Agent>();

					switch (content.getTopic()) {//修正
					case COMINGOUT:
						// targetとsubjectが異なっていたら、そのCOは意味がない
						if (target == subject) {
							switch (COrole) {
							case SEER:
								SeerCOAgent.add(target);
								wolfAgentMap.put(subject, wolf);
								humanAgentMap.put(subject, human);
								orderSeerCO.put(subject, SeerCOAgent.size());
								break;
							case MEDIUM:
								MediumCOAgent.add(target);
								wolfAgentMapByMedium.put(subject, wolf);
								humanAgentMapByMedium.put(subject, human);
								orderMediumCO.put(subject, MediumCOAgent.size());
								break;
							}
						}
						break;
						// COしていなくても、発言からCOと見なせるときはCOとみなす
					case DIVINED:
						SeerCOAgent.add(subject);
						humanAgentMap.put(subject, human);
						wolfAgentMap.put(subject, wolf);
						break;
					case IDENTIFIED://修正
						MediumCOAgent.add(subject);
						wolfAgentMapByMedium.put(subject, wolf);
						humanAgentMapByMedium.put(subject, human);
						break;
					}
				}
			}
		}

		//判定結果の更新
		private void setWolfHumanMap(List<Talk> talkList) {

			if (latestGameInfo.getDay() > 0
					&& latestGameInfo.getAliveAgentList().contains(
							latestGameInfo.getAgent())) {
				for (int i = readTalkNum; i < talkList.size(); i++) {
					Talk talk = talkList.get(i);
					// 発話をパース
					Content content = new Content(talk.getText());//修正

					Agent subject = talk.getAgent();
					Agent target = content.getTarget();//修正
					Species species = content.getResult();//修正

					if (content.getTopic() == Topic.DIVINED) {//修正

						if (species == Species.WEREWOLF) {
							Set<Agent> wolfAgent = wolfAgentMap.get(subject);
							wolfAgent.add(target);
							wolfAgentMap.put(subject, wolfAgent);
						} else if (species == Species.HUMAN) {
							Set<Agent> humanAgent = humanAgentMap.get(subject);
							humanAgent.add(target);
							humanAgentMap.put(subject, humanAgent);
						}

					} else if (content.getTopic() == Topic.IDENTIFIED) {//修正

						if (species == Species.WEREWOLF) {
							Set<Agent> wolfAgent = wolfAgentMapByMedium
									.get(subject);
							wolfAgent.add(target);
							wolfAgentMapByMedium.put(subject, wolfAgent);
						} else if (species == Species.HUMAN) {
							Set<Agent> humanAgent = humanAgentMapByMedium
									.get(subject);
							humanAgent.add(target);
							humanAgentMapByMedium.put(subject, humanAgent);
						}

					}
				}
			}
		}

		//votingMap(投票予定先)の更新
		private void setVoteMap(List<Talk> talkList) {
			if (latestGameInfo.getAliveAgentList().contains(
					latestGameInfo.getAgent())) {
				for (int i = readTalkNum; i < talkList.size(); i++) {
					Talk talk = talkList.get(i);
					// 発話をパース
					Content content = new Content(talk.getText());//修正

					Agent subject = talk.getAgent();
					Agent target = content.getTarget();//修正

					if (content.getTopic() == Topic.VOTE) {//修正
						if (latestGameInfo.getAliveAgentList().contains(target)) {// vote対象が生きていないなら意味ない
							votingMap.put(subject, target);
						}
					}
				}
			}
		}

}
