package server;

import client.LineInfo;

import java.awt.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

public class ClientThread extends Thread {
    private Socket sock;
    private String username;
    private BufferedReader br;
    private PrintWriter pw;
    private Vector<UserPair> userVector;
    private boolean running = false;
    private boolean isRoomOwner = false;
    private UserPair userpair = null;
    private GameInfo game = null;
    private static Map<String, Integer> scores = new HashMap<>(); // 플레이어별 점수 저장
    private final Vector<LineInfo> drawVector = new Vector<>();

    public ClientThread(Socket sock, Vector userVector, int usernum, GameInfo game){
        this.sock = sock;
        this.userVector = userVector;
        this.game = game;

        try {
            pw = new PrintWriter(new OutputStreamWriter(sock.getOutputStream()));
            br = new BufferedReader(new InputStreamReader((sock.getInputStream())));

            this.username = "player " + usernum;
            this.userpair = new UserPair(username, pw);
            System.out.println("System: " + username + " 접속.");
            synchronized (userVector){
                if (userVector.isEmpty()) {
                    game.setCurrentPresenter(username);  // 첫 번째 플레이어가 초기 출제자
                }
                userVector.add(userpair);
                if (userVector.size() == 1){
                    isRoomOwner = true;
                    alertRoomOpen();
                    System.out.println("System: " + username + " 방장.");
                }
                alertRoomEnter(usernum);
                alertPlayerUpdate();

                // 이미 주제가 선택되어 있다면 새로 접속한 사용자에게 전송
                if (game.getSubject() != null && !game.getSubject().isEmpty()) {
                    sendself("SUBJECT#" + game.getSubject());
                }
            }
            running = true;

        }
        catch (Exception e) {System.out.println(e);}

    }

    private void sendGameWord() {
        String word = game.getRandomWord();
        for (UserPair user : userVector) {
            user.getPw().println("SUBJECT_WORD#" + username + "#" + word);
            user.getPw().flush();
        }
        /*// 방장(그림 그리는 사람)에게만 제시어 전송
        if(userpair.getUsername().equals(game.getCurrentPresenter())){
            userpair.getPw().println("SUBJECT_WORD#" + username + "#" + word);
            userpair.getPw().flush();
        }*/
    }

    public void run(){
        try{
            String line = null;
            while ((line = br.readLine()) != null){
                String[] parseLine = line.split("#");
                if (line.equals("/quit")){
                    break;
                }

                System.out.println(line);
                if (parseLine[0].equals("NEW_ROOM_OWNER")){
                    isRoomOwner = true;
                    System.out.println("System: 방장이 바뀌었습니다: " + userpair.getUsername());
                }

                if (parseLine[0].equals("SUBJECT")){
                    game.setSubject(parseLine[1]);
                    System.out.println("System: 게임 주제 설정 - " + parseLine[1]);
                    sendall("SUBJECT#" + parseLine[1]);
                }

                if (parseLine[0].equals("START_GAME")){
                    if (userVector.size() == 1){
                        String msg = "다른 참가자가 없습니다. 게임을 시작하지 않습니다.";
                        System.out.println("System: " + msg);
                        sendself(msg);
                        sendself("GAME_DENIED");

                    }
                    else if (userVector.size() > 1) {
                        String msg = "게임을 시작합니다.";
                        game.setRunning(true);
                        game.initializeGameScores(); // 게임 시작 시 초기 플레이어 목록과 점수 초기화
                        System.out.println("System: " + msg);
                        sendall("GAME_STARTED");
                        game.setCurrentPresenter(username);
                        game.setupTimer(); // 타이머 동기화 시작
                        sendall(msg);
                        sendGameWord(); // 게임 시작 시 제시어 전송
                    }
                }
                if (parseLine[0].equals("DRAW")){
                    synchronized (drawVector){
                        if(parseLine[1].equals("NULL")){
                            drawVector.add(null);
                        }
                        else if(parseLine[1].equals("CLEAR")){
                            drawVector.clear();
                        }
                        else{
                            Point p = new Point(Integer.parseInt(parseLine[1]),Integer.parseInt(parseLine[2]));
                            LineInfo newLine = new LineInfo(p,Integer.parseInt(parseLine[3]));
                            drawVector.add(newLine);
                        }
                        sendall(line);
                    }
                }
                // 정답 체크 로직
                if (parseLine[0].equals("ANSWER")) {
                    String username = parseLine[1];
                    String answer = parseLine[2].trim();
                    String currentWord = game.getCurrentWord();

                    boolean isCorrect = answer.equals(currentWord);

                    if (isCorrect) {
                        // 이전 출제자(그림 그린 사람) 저장
                        String previousPresenter = game.getCurrentPresenter();

                        // 새 제시어 준비
                        // String word = game.getRandomWord();

                        // 점수 처리
                        game.updateScore(username, 10);       // 정답자 점수
                        game.updateScore(previousPresenter, 10); // 출제자 점수

                        // 점수 업데이트 메시지 전송
                        String presenterScoreMessage = "SCORE_UP#" + previousPresenter + "#10";
                        sendall(presenterScoreMessage);

                        // 출제자 변경
                        game.setCurrentPresenter(username);

                        // 정답 맞춤 처리 (라운드 정보 포함)
                        String correctMessage = "CORRECT_ANSWER#" + username + "#10#" + game.getCurrentRound();
                        sendall(correctMessage);

                        game.updateRound();

                        if(game.getCurrentRound() > 10) {
                            // 최종 게임 결과 메시지 생성 및 전송
                            String resultMessage = game.generateGameEndMessage();
                            sendall(resultMessage);
                            game.setRunning(false);
                            game.reset();
                            scores.clear();
                        } else {
                            // 새 제시어 준비
                            String newWord = game.getRandomWord();

                            // 출제자 변경
                            game.setCurrentPresenter(username);

                            // 새로운 제시어 전송
                            String subjectMessage = "SUBJECT_WORD#" + username + "#" + newWord;
                            for (UserPair user : userVector) {
                                user.getPw().println(subjectMessage);
                                user.getPw().flush();
                                /*if (user.getUsername().equals(username)) {
                                    user.getPw().println(subjectMessage);
                                    user.getPw().flush();
                                }*/
                            }


                            // 타이머 리셋
                            game.setupTimer();

                        }
                    } else {
                        sendself("WRONG_ANSWER#" + username);
                    }
                }

                if (parseLine[0].equals("GAME_END")){
                    String resultMessage = game.generateGameEndMessage();
                    sendall(resultMessage);
                    game.setRunning(false);
                    game.reset();
                    scores.clear();
                }

            }
        }
        catch (Exception e) { System.out.println(e); }
        finally {
            synchronized (userVector){
                System.out.println("System: " + username + " 나감.");
                userVector.remove(userpair);

                // 게임이 진행 중일 때 플레이어가 나가는 경우
                if (game.isRunning()) {
                    if (userVector.size() <= 1) {
                        // 1명 이하가 되면 게임 종료
                        if (game.getGameTimer() != null) {
                            game.getGameTimer().cancel();
                        }
                        String resultMessage = game.generateGameEndMessage();
                        sendall(resultMessage);
                        game.setRunning(false);
                        game.reset();
                        scores.clear();
                    }
                    else if (username.equals(game.getCurrentPresenter())) {
                        // 출제자가 나간 경우
                        String newPresenter = game.selectRandomPresenter();
                        game.setCurrentPresenter(newPresenter);
                        String newWord = game.getRandomWord();

                        // 1. 모든 클라이언트에게 출제자가 나갔다는 알림
                        sendall("PRESENTER_DISCONNECTED#" + username);

                        // 2. 모든 클라이언트에게 새로운 턴 시작 알림
                        sendall("TURN_START#" + newPresenter+"#"+game.getCurrentRound());

                        // 3. 새로운 출제자에게 제시어 전송 (마지막에 실행)
                        synchronized(userVector) {
                            for (UserPair user : userVector) {
                                if (user.getUsername().equals(newPresenter)) {
                                    user.getPw().println("SUBJECT_WORD#" + newPresenter + "#" + newWord);
                                    user.getPw().flush();
                                }
                            }
                        }

                        // 4. 타이머 리셋
                        game.setupTimer();
                    }
                }

                if (isRoomOwner && !userVector.isEmpty()){
                    newRoomOwner();
                }
                alertPlayerUpdate();
            }
            try{
                if (sock != null){
                    sock.close();
                }
            }
            catch (Exception e) {System.out.println(e);}
        }
    }

    public void alertRoomEnter(int usernum){
        PrintWriter pwr = userpair.getPw();
        pwr.println("ENTER_ROOM#" + usernum);
        pwr.flush();
    }

    public void alertPlayerUpdate(){
        String players = "PLAYER_UPDATE";
        for (int i = 0; i < userVector.size(); i++){
            players = players + "#" + userVector.get(i).getUsername();
        }
        sendall(players);
    }

    public void alertRoomOpen(){
        PrintWriter pwr = userpair.getPw();
        pwr.println("CHECK_CAPTAIN");
        pwr.flush();
    }

    public void newRoomOwner(){
        PrintWriter pwr = userVector.get(0).getPw();
        pwr.println("NEW_CAPTAIN");
        pwr.flush();
    }

    public void sendself(String msg){
        PrintWriter pwr = userpair.getPw();
        pwr.println(msg);
        pwr.flush();
    }

    public void sendmsg(String msg){
        for (int i = 0; i < userVector.size(); i++) {
            if (!userVector.get(i).equals(userpair)) {
                PrintWriter pwr = userVector.get(i).getPw();
                pwr.println(msg);
                pwr.flush();
            }
        }
    }

    public void sendall(String msg){
        for (int i = 0; i < userVector.size(); i++) {
            PrintWriter pwr = userVector.get(i).getPw();
            pwr.println(msg);
            pwr.flush();
        }
    }

}

