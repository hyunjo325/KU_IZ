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
        if (isRoomOwner) {
            sendself("SUBJECT_WORD#" + username + "#" + word);
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
                if (parseLine[0].equals("TURN_START")) {
                    sendGameWord(); // 다음 라운드 시작 시 새로운 제시어 전송
                }
                // 정답 체크 로직
                if (parseLine[0].equals("ANSWER")) {
                    String username = parseLine[1];
                    String answer = parseLine[2].trim();
                    String currentWord = game.getCurrentWord();

                    boolean isCorrect = answer.equals(currentWord);

                    if (isCorrect) {
                        // 정답 맞춤 처리
                        String correctMessage = "CORRECT_ANSWER#" + username+"#10";
                        System.out.println("Correct answer by: " + username);
                        sendall(correctMessage);

                        // 점수 올리기
                        scores.putIfAbsent(username, 0);
                        scores.put(username, scores.get(username)+10);

                        // 권한 바꾸기


                        // 다음 라운드 준비
                        Thread.sleep(2000); // 2초 딜레이
                        sendGameWord(); // 새로운 제시어 전송
                    }
                }

            }
        }
        catch (Exception e) { System.out.println(e); }
        finally {
            synchronized (userVector){
                System.out.println("System: " + username + " 나감.");
                userVector.remove(userpair);
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

