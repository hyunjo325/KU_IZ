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
            }
            running = true;

        }
        catch (Exception e) {System.out.println(e);}

    }

    private void sendGameWord() {
        String word = game.getRandomWord();
        // 방장(그림 그리는 사람)에게만 제시어 전송
        if(userpair.getUsername().equals(game.getCurrentPresenter())){
            userpair.getPw().println("SUBJECT_WORD#" + username + "#" + word);
            userpair.getPw().flush();
        }
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
                        String word = game.getRandomWord();

                        // 출제자 변경 전에 점수 업데이트
                        scores.putIfAbsent(username, 0);  // 정답 맞춘 사람
                        scores.putIfAbsent(previousPresenter, 0);  // 이전 출제자

                        // 각각 10점씩 부여
                        scores.put(username, scores.get(username) + 10);  // 정답 맞춘 사람 +10
                        scores.put(previousPresenter, scores.get(previousPresenter) + 10);  // 그림 그린 사람 +10

                        // 점수 업데이트 메시지 전송
                        String presenterScoreMessage = "SCORE_UP#" + previousPresenter + "#10";  // 그림 그린 사람
                        sendall(presenterScoreMessage);

                        // 이제 출제자 변경
                        game.setCurrentPresenter(username);

                        // 정답 맞춤 처리
                        String correctMessage = "CORRECT_ANSWER#" + username + "#10";
                        System.out.println("Correct answer by: " + username);
                        sendall(correctMessage);

                        // 새로운 제시어 전송
                        String subjectMessage = "SUBJECT_WORD#" + username + "#" + word;
                        for (UserPair user : userVector) {
                            if (user.getUsername().equals(username)) {
                                user.getPw().println(subjectMessage);
                                user.getPw().flush();
                            }
                        }

                        // 타이머 리셋
                        game.setupTimer();
                    }
                }
                if (parseLine[0].equals("GAME_END")){
                    // 최종 점수를 담을 메시지 생성
                    StringBuilder resultMessage = new StringBuilder("GAME_END");

                    // 가장 높은 점수 찾기
                    int maxScore = -1;
                    synchronized (userVector) {
                        for (UserPair user : userVector) {
                            username = user.getUsername();
                            int score = scores.getOrDefault(username, 0);
                            resultMessage.append("#").append(username).append("#").append(score);
                            maxScore = Math.max(maxScore, score);
                        }
                    }

                    // 승자 찾기 (최고 점수를 가진 모든 플레이어)
                    resultMessage.append("#WIN");
                    synchronized (userVector) {
                        for (UserPair user : userVector) {
                            username = user.getUsername();
                            if (scores.getOrDefault(username, 0) == maxScore) {
                                resultMessage.append("#").append(username);
                            }
                        }
                    }

                    // 모든 클라이언트에게 결과 전송
                    sendall(resultMessage.toString());
                    game.setRunning(false);
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
                        game.setRunning(false);
                        if (game.getGameTimer() != null) {
                            game.getGameTimer().cancel();
                        }

                        // 남은 플레이어에게 게임 종료 메시지 전송
                        StringBuilder resultMessage = new StringBuilder("GAME_END");
                        // 점수 정보 추가
                        synchronized (userVector) {
                            for (UserPair user : userVector) {
                                String playerName = user.getUsername();
                                int score = scores.getOrDefault(playerName, 0);
                                resultMessage.append("#").append(playerName).append("#").append(score);
                            }
                        }
                        // 남은 플레이어를 승자로 지정
                        if (!userVector.isEmpty()) {
                            resultMessage.append("#WIN#").append(userVector.get(0).getUsername());
                        }
                        sendall(resultMessage.toString());
                    }
                    else if (username.equals(game.getCurrentPresenter())) {
                        // 출제자가 나간 경우
                        String newPresenter = game.selectRandomPresenter();
                        game.setCurrentPresenter(newPresenter);
                        String newWord = game.getRandomWord();

                        // 1. 모든 클라이언트에게 출제자가 나갔다는 알림
                        sendall("PRESENTER_DISCONNECTED#" + username);

                        // 2. 모든 클라이언트에게 새로운 턴 시작 알림
                        sendall("TURN_START#" + newPresenter);

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

