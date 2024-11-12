package client;

import java.net.*;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class LobbyThread extends Thread{
    private Socket sock = null;
    private PrintWriter pw = null;
    private BufferedReader br = null;
    private UserData userdata = null;
    //private BufferedReader scanner = new BufferedReader(new InputStreamReader(System.in));
    private boolean gameRunning = false;
    private HostUI hostUI;

    public LobbyThread(Socket sock, PrintWriter pw, BufferedReader br, UserData userdata){
        this.sock = sock;
        this.pw = pw;
        this.br = br;
        this.userdata = userdata;
    }

    public void run() {
        try {

            String line = null;
            while ((line = br.readLine()) != null){
                System.out.println(line);
                String[] parseLine = line.split("#");
                if (parseLine[0].equals("CHECK_CAPTAIN")){                  //방장이 처음으로 들어옴
                    System.out.println("당신은 방장입니다.");
                    userdata.setIsRoomOwner(true);
                    hostUI = new HostUI(userdata.getIsRoomOwner(), sock, pw, br, userdata);
                    //chooseSubject();
                    //startGame();
                }
                if (parseLine[0].equals("NEW_CAPTAIN")){                    //방장이 바뀌었음을 알리고, 게임이 진행 중이 아니라면 방장이 새로 주제 선택 가능
                    System.out.println("당신이 방장이 되었습니다.");
                    userdata.setIsRoomOwner(true);
                    updateRoomOwner();
                    if (!gameRunning){
                        hostUI.dispose();
                        hostUI = new HostUI(userdata.getIsRoomOwner(), sock, pw, br, userdata);
                    }
                }
                if (parseLine[0].equals("ENTER_ROOM")){                     //방에 들어왔음을 알림
                    userdata.setUsername("player " + parseLine[1]);
                    if (!userdata.getIsRoomOwner()){
                        hostUI = new HostUI(userdata.getIsRoomOwner(), sock, pw, br, userdata);
                    }
                }
                if (parseLine[0].equals("PLAYER_UPDATE")){                  //새로운 참가자가 들어왔을 때 list update
                    List<String> playerList = new ArrayList<String>();
                    for (int i = 1; i < parseLine.length; i++){
                        playerList.add(parseLine[i]);
                    }
                    userdata.updatePlayerList(playerList);
                    hostUI.updatePlayerList();
                }

                if (parseLine[0].equals("SUBJECT")){                        //방장이 주제 선택
                    userdata.setQuizTopic(parseLine[1]);
                }



                if (parseLine[0].equals("GAME_DENIED")){                    //아무도 없어서 게임을 시작하지 않음
                    //다른 참가자가 없어서 게임을 시작하지 않음을 알리는 글 추가?
                }
                if (parseLine[0].equals("GAME_STARTED")){                   //게임 시작
                    gameRunning = true;
                    //GameThread 실행 및 join

                    //GameUI 실행
                    hostUI.startGame();
                }
                if (parseLine[0].equals("GAME_RUNNING")){
                    System.out.println("게임이 이미 진행 중입니다.");
                    break;
                }

            }
        }
        catch (Exception e) {System.out.println(e);}
        finally {
            try{
                if (br != null) {br.close();}
                if (sock != null) {sock.close();}
            }
            catch (Exception e) {System.out.println(e);}
        }
    }


    public void updateRoomOwner(){
        pw.println("NEW_ROOM_OWNER");
        pw.flush();
    }

}
