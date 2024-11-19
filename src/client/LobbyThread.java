package client;

import server.GameInfo;

import java.awt.*;
import java.net.*;
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LobbyThread extends Thread{
    private Socket sock = null;
    private PrintWriter pw = null;
    private BufferedReader br = null;
    private UserData userdata = null;
    //private BufferedReader scanner = new BufferedReader(new InputStreamReader(System.in));
    private boolean gameRunning = false;
    private HostUI hostUI;
    private GameInfo gameInfo;

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

                    //GameUI 실행
                    hostUI.startGame();
                }
                if (parseLine[0].equals("DRAW")){
                    if (parseLine[1].equals("NULL")) {
                        hostUI.getGameUI().receiveDrawingUpdates(null);
                    } else if (parseLine[1].equals("CLEAR")) {
                        hostUI.getGameUI().clearDrawingPanel();
                    } else {
                        Point p = new Point(Integer.parseInt(parseLine[1]), Integer.parseInt(parseLine[2]));
                        LineInfo newLine = new LineInfo(p, Integer.parseInt(parseLine[3]));
                        hostUI.getGameUI().receiveDrawingUpdates(newLine);
                    }
                }
                if (parseLine[0].equals("GAME_RUNNING")){
                    System.out.println("게임이 이미 진행 중입니다.");
                    break;
                }
                if (parseLine[0].equals("SUBJECT")) {
                    userdata.setQuizTopic(parseLine[1]);
                    if (hostUI != null) {
                        hostUI.updateQuizTopic(parseLine[1]);  // 새로운 메서드 추가
                    }
                }
                if (parseLine[0].equals("SUBJECT_WORD")) {
                    if (hostUI != null && hostUI.getGameUI() != null) {
                        hostUI.getGameUI().updateWord(parseLine[2]);
                    }
                }
                if (parseLine[0].equals("CORRECT_ANSWER")) {
                    String username = parseLine[1];
                    String score = parseLine[2];
                    if (hostUI != null && hostUI.getGameUI() != null) {
                        hostUI.getGameUI().handlePresenterChange(username);
                        hostUI.getGameUI().updateScores(username, score);
                        hostUI.getGameUI().handleAnswerResult(username, true);
                        hostUI.getGameUI().updateRoundDisplay(Integer.parseInt(parseLine[3])); // 라운드 정보 처리
                        hostUI.getGameUI().revalidate();
                        hostUI.getGameUI().repaint();
                    }
                }
                if (parseLine[0].equals("WRONG_ANSWER")) {
                    if (hostUI != null && hostUI.getGameUI() != null) {
                        hostUI.getGameUI().handleWrongAnswer(parseLine[1]);

                    }
                }
                if (parseLine[0].equals("TIME")) {
                    if (hostUI != null && hostUI.getGameUI() != null) {
                        hostUI.getGameUI().syncTime(Integer.parseInt(parseLine[1]));
                    }
                }
                if (parseLine[0].equals("TIME_UP")) {
                    String newPresenter = parseLine[1];
                    if (hostUI != null && hostUI.getGameUI() != null) {
                        // 시간 초과 메시지를 표시하고 새로운 출제자로 변경
                        hostUI.getGameUI().handlePresenterChange(newPresenter);
                        hostUI.getGameUI().handleTimeUp(newPresenter);
                    }
                }
                if (parseLine[0].equals("GAME_END")) {
                    // 게임 결과 파싱
                    Map<String, Integer> finalScores = new HashMap<>();
                    List<String> winners = new ArrayList<>();

                    int i = 1;
                    while (i < parseLine.length && !parseLine[i].equals("WIN")) {
                        String username = parseLine[i];
                        int score = Integer.parseInt(parseLine[i + 1]);
                        finalScores.put(username, score);
                        i += 2;
                    }

                    // 우승자 목록 파싱
                    i++; // "WIN" 다음부터
                    while (i < parseLine.length) {
                        winners.add(parseLine[i]);
                        i++;
                    }

                    if (hostUI != null && hostUI.getGameUI() != null) {
                        hostUI.getGameUI().showFinalResults(finalScores, winners);
                    }
                }
                if (parseLine[0].equals("SCORE_UP")) {
                    String username = parseLine[1];
                    String score = parseLine[2];
                    if (hostUI != null && hostUI.getGameUI() != null) {
                        hostUI.getGameUI().updateScores(username, score);
                    }
                }
                if (parseLine[0].equals("PRESENTER_DISCONNECTED")) {
                    String disconnectedUser = parseLine[1];
                    if (hostUI != null && hostUI.getGameUI() != null) {
                        hostUI.getGameUI().handlePresenterDisconnected(disconnectedUser);
                    }
                }
                if (parseLine[0].equals("TURN_START")) {
                    String newPresenter = parseLine[1];
                    int currentRound = Integer.parseInt(parseLine[2]); // 서버에서 보낸 현재 라운드 정보
                    if (hostUI != null && hostUI.getGameUI() != null) {
                        hostUI.getGameUI().handlePresenterChange(newPresenter);
                        hostUI.getGameUI().updateRoundDisplay(currentRound); // 현재 라운드로 표시 업데이트
                        hostUI.getGameUI().clearDrawingPanel();
                    }
                }
                if (parseLine[0].equals("UPDATE_ROUND")) {
                    int newRound = Integer.parseInt(parseLine[1]);
                    if (hostUI != null && hostUI.getGameUI() != null) {
                        hostUI.getGameUI().updateRoundDisplay(newRound);
                        hostUI.getGameUI().revalidate();
                        hostUI.getGameUI().repaint();
                    }
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
