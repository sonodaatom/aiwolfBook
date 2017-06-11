package book;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class LogdataToVector {
	public static void main(String[] args){
		(new LogdataToVector()).mainLoop();
	}

	private void mainLoop(){
		File logdata = new File("/Users/sonodaatom/Desktop/gat");
		for(File dir:logdata.listFiles()){
			System.out.println(dir);
			for(File file:dir.listFiles()){
				String name=file.getName();
				List<String[]> stringList = null;
				try {
					stringList = openFile(file);
				} catch (IOException e) {
					e.printStackTrace();
				}
				outFile("data/"+name+".txt", toString(stringList));
			}
		}
	}
	
	//ログデータの読み込み
	private List<String[]> openFile(File file) throws IOException {
		List<String[]> stringList = new ArrayList<String[]>();
		FileReader filereader = new FileReader(file);
		BufferedReader br = new BufferedReader(filereader);
		String str;
		while((str = br.readLine()) != null){
			str=str.replace(" ", ",");//スペースを,で置き換え
			str=str.replace("Agent[", "");//Agent[をで置き換え
			str=str.replace("]", "");//Agent[をで置き換え
			stringList.add(str.split(","));
		}
		br.close();
		return stringList;
	}
	
	//出力
	private void outFile(String fileName, String str){
		try {
			FileWriter fileWriter = new FileWriter(fileName);
			BufferedWriter bw = new BufferedWriter(fileWriter);
			bw.write(str);
			bw.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	//データの整形
	private String toString(List<String[]> stringList){
		//COしている占い師、霊能者数
		int numCoSeer =0;
		int numCoMedium = 0;
		int[] SeerOrNot = new int[15];
		for(int i=0;i<15;i++){//Seerなら１
			SeerOrNot[i]=0;
		}
		//日にち
		int date =0;
		//プレイヤーごとの特徴量
		List<Integer[][]> infoList = new ArrayList<Integer[][]>();
		Integer[][] info = new Integer[15][12];
		for(int i=0;i<15;i++){
			info[i][0]=0;//人狼なら-1,人間なら1  status
			info[i][1]=0;//生きてるなら０、死んでるなら１  status
			info[i][2]=0;//CO占い師数  すべてのプレイヤーで共通  talk
			info[i][3]=0;//CO霊媒師数　すべてのプレイヤーで共通  talk
			info[i][4]=0;//受けた占い人間判定数  talk
			info[i][5]=0;//受けた占い人狼判定数  talk
			info[i][6]=0;//何番目に占い師CO  talk
			info[i][7]=0;//何番目に霊媒師CO  talk
			info[i][8]=0;//占い師で人間判定を出した数  talk 
			info[i][9]=0;//占い師で人狼判定を出した数  talk
			info[i][10]=0;//VOTE発話から投票変更  talk & vote
			info[i][11]=0;//日にち　すべてのプレイヤーで共通  status
		}
		
		int[][] infoTalkVote = new int[15][2];// talkにおける発話者ごとの投票先、日にち
		int[][] infoRealVote = new int[15][2];// 発話者ごとの実際の投票先、日にち
		for(int i=0;i<15;i++){//初期化
			for(int j=0;j<2;j++){
				infoTalkVote[i][j]=-1;
				infoRealVote[i][j]=-1;
			}
		}
		
		for(String[] strArray : stringList){//ログデータのすべての行について処理
			if(Integer.parseInt(strArray[0])!=date){//日にちが変わったら、前日までのinfoをinfoListに格納
				for(int j=0;j<15;j++){//すべてのプレイヤーで共通
					info[j][2]=numCoSeer;
					info[j][3]=numCoMedium;
					info[j][11]=date;
				}
				Integer[][] tempInfo = new Integer[15][12];
				for(int a=0;a<15;a++){
					for(int b=0;b<12;b++){
						tempInfo[a][b]=info[a][b];
					}
				}
				infoList.add(tempInfo);
				date = Integer.parseInt(strArray[0]);//日にち
			}
			
			if(strArray[1].equals("status")){
				int i = Integer.parseInt(strArray[2])-1;//エージェントのID
				if(date==0){//初日だけ処理
					//人間なら1
					if(strArray[3].equals("VILLAGER")||strArray[3].equals("MEDIUM")||strArray[3].equals("POSSESSED")
							||strArray[3].equals("SEER")||strArray[3].equals("BODYGUARD")){
						info[i][0]=1;//人間
					//人狼なら-1
					}else if(strArray[3].equals("WEREWOLF")){
						info[i][0]=-1;
					}
				}
				
				//生きているなら0
				if(strArray[4].equals("ALIVE")){
					info[i][1]=0;//生きている
				}else if(strArray[4].equals("DEAD")){
					info[i][1]=1;//死んでる
				}
				
			}
			
			if(strArray[1].equals("talk")){
				int i = Integer.parseInt(strArray[4])-1;//エージェントのID//修正
				
				//CO占い師数はその日の終わりに代入
				//何番目に占い師CO
				if(strArray[5].equals("COMINGOUT")&&strArray[7].equals("SEER")){//修正
					SeerOrNot[i]=1;
					numCoSeer++;
					info[i][6]=numCoSeer;
				}
				
				//CO霊媒師数はその日の終わりに代入
				//何番目に霊媒師CO
				if(strArray[5].equals("COMINGOUT")&&strArray[7].equals("MEDIUM")){//修正
					numCoMedium++;
					info[i][7]=numCoMedium;
				}
				
				//受けた占い人間判定数
				//占い師で人間判定を出した数
				if(SeerOrNot[i]==1&&strArray[5].equals("DIVINED")&&strArray[7].equals("HUMAN")){//修正
					info[Integer.parseInt(strArray[6])-1][4]++;//修正
					info[i][8]++;
				}
				
				//受けた占い人狼判定数
				//占い師で人狼判定を出した数
				if(SeerOrNot[i]==1&&strArray[5].equals("DIVINED")&&strArray[7].equals("WEREWOLF")){//修正
					info[Integer.parseInt(strArray[6])-1][5]++;//修正
					info[i][9]++;
				}
				
				//VOTE発話から投票変更
				////VOTE発話の内容保持//発話者ごとの投票先、日にち
				if(strArray[5].equals("VOTE")){//修正
					int voteFor = Integer.parseInt(strArray[6])-1;//投票先エージェントのID//修正
					infoTalkVote[i][0]=voteFor;
					infoTalkVote[i][1]=date;
				}
			}
			
			//VOTE発話から投票変更
			//VOTE発話の内容保持
			if(strArray[1].equals("vote")){
				int i = Integer.parseInt(strArray[2])-1;//エージェントのID
				
				infoRealVote[i][0]=Integer.parseInt(strArray[3])-1;//投票先ID
				infoRealVote[i][1]=date;
				//同じ日にtalkした内容と実際の投票が違ったら1
				if(infoTalkVote[i][1]==infoRealVote[i][1]&&infoTalkVote[i][0]!=infoRealVote[i][0]){
					info[i][10]++;
				}
			}
		}
		
		
		String string = "";
		int counter = 0;
		//情報をlibsvmで使える形でStringにする
		for(Integer[][] intArray2 : infoList){
			for(Integer[] intArray1 : intArray2){
				for(Integer intinfo : intArray1){
					if(counter==0){
						string += intinfo + " 1:";
						counter++;
					}else if(counter==1){
						string += intinfo + " 2:";
						counter++;
					}else if(counter==2){
						string += intinfo + " 3:";
						counter++;
					}else if(counter==3){
						string += intinfo + " 4:";
						counter++;
					}else if(counter==4){
						string += intinfo + " 5:";
						counter++;
					}else if(counter==5){
						string += intinfo + " 6:";
						counter++;
					}else if(counter==6){
						string += intinfo + " 7:";
						counter++;
					}else if(counter==7){
						string += intinfo + " 8:";
						counter++;
					}else if(counter==8){
						string += intinfo + " 9:";
						counter++;
					}else if(counter==9){
						string += intinfo + " 10:";
						counter++;
					}else if(counter==10){
						string += intinfo + " 11:";
						counter++;
					}else{
						string += intinfo;
						counter=0;
						string += "\n";
					}
				}
			}
		}
		return string;
	}
	
}
