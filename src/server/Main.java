package server;

import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicInteger;

public class Main {
    public static void main(String[] args) {
        Vector<UserPair> userVector = new Vector<>();
        GameInfo game = new GameInfo(userVector);
        AtomicInteger usernum = new AtomicInteger(1);

        try {
            ServerSocket server = new ServerSocket(20000);
            System.out.println("Waiting Client");

            Thread monitorThread = new Thread(() -> {
                while (true) {
                    try {
                        Thread.sleep(1000); // 1초마다 체크
                        if (userVector.isEmpty() && game.isRunning()) {
                            // 게임 정보 초기화
                            game.reset();
                            usernum.set(1);
                            System.out.println("모든 사용자가 나갔습니다. 게임 정보가 초기화되었습니다.");
                        }
                        else if (userVector.isEmpty()){
                            usernum.set(1);
                        }
                    } catch (InterruptedException e) {
                        System.out.println("Monitor thread interrupted: " + e);
                    }
                }
            });
            monitorThread.setDaemon(true); // 메인 스레드 종료시 같이 종료되도록 설정
            monitorThread.start();

            while (true) {
                Socket sock = server.accept();
                if (!game.isRunning()) {
                    ClientThread clientthread = new ClientThread(sock, userVector, usernum.get(), game);
                    clientthread.start();
                    usernum.incrementAndGet();
                } else {
                    PrintWriter pw = new PrintWriter(new OutputStreamWriter(sock.getOutputStream()));
                    pw.println("GAME_RUNNING");
                    pw.flush();
                    pw.close();
                    sock.close();
                }
            }
        } catch (Exception e) {
            System.out.println(e);
        }
    }
}