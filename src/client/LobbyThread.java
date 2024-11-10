package client;

import java.net.*;
import java.io.*;

public class LobbyThread extends Thread{
    private Socket sock = null;
    private PrintWriter pw = null;
    private BufferedReader br = null;
    private UserData userdata = null;
    private BufferedReader scanner = new BufferedReader(new InputStreamReader(System.in));
    private boolean gameRunning = false;

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
                if (parseLine[0].equals("CHECK_CAPTAIN")){
                    System.out.println("당신은 방장입니다.");
                    userdata.setIsRoomOwner(true);
                    chooseSubject();
                    startGame();
                }
                if (parseLine[0].equals("NEW_CAPTAIN")){
                    System.out.println("당신이 방장이 되었습니다.");
                    userdata.setIsRoomOwner(true);
                    updateRoomOwner();
                    if (!gameRunning){
                        chooseSubject();
                        startGame();
                    }
                }
                if (parseLine[0].equals("ENTER_ROOM")){
                    userdata.setUsername("player " + parseLine[1]);
                }

                if (parseLine[0].equals("GAME_DENIED")){
                    startGame();
                }
                if (parseLine[0].equals("GAME_STARTED")){
                    gameRunning = true;
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

    public void chooseSubject(){
        System.out.print("주제를 선택하세요: ");
        String subject = null;
        try {
            subject = scanner.readLine();
        } catch (IOException e) {
            System.out.println(e);
        }

        pw.println("SUBJECT#" + subject);
        pw.flush();
    }

    public void startGame(){
        System.out.print("게임을 시작하고 싶을 때 아무거나 입력하세요: ");
        String start = null;
        try {
            start = scanner.readLine();
        } catch (IOException e) {
            System.out.println(e);
        }

        pw.println("START_GAME");
        pw.flush();
    }

    public void updateRoomOwner(){
        pw.println("NEW_ROOM_OWNER");
        pw.flush();
    }

}
