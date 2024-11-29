package client;

import javax.swing.*;
import java.net.*;
import java.io.*;

public class Main {
    public static void main(String[] args) {
        Socket sock = null;
        BufferedReader br = null;
        PrintWriter pw = null;
        boolean running = true;
        UserData userdata = new UserData("null", false);

        try {
            // 사용자로부터 IP 입력받기
            String serverIP = JOptionPane.showInputDialog(null,
                    "IP 주소를 입력하세요:",
                    "서버 연결",
                    JOptionPane.QUESTION_MESSAGE);

            if (serverIP == null || serverIP.trim().isEmpty()) {
                JOptionPane.showMessageDialog(null,
                        "서버 IP를 입력하지 않아 프로그램을 종료합니다.",
                        "종료",
                        JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            // 소켓 연결 시도
            sock = new Socket(serverIP, 20000); // 입력받은 IP
            pw = new PrintWriter(new OutputStreamWriter(sock.getOutputStream()));
            br = new BufferedReader(new InputStreamReader(sock.getInputStream()));

            BufferedReader scanner = new BufferedReader(new InputStreamReader(System.in));

            LobbyThread game = new LobbyThread(sock, pw, br, userdata);
            game.start();
            game.join();

        } catch (UnknownHostException e) {
            JOptionPane.showMessageDialog(null,
                    "유효하지 않은 서버 IP입니다.",
                    "연결 실패",
                    JOptionPane.ERROR_MESSAGE);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null,
                    "서버에 연결할 수 없습니다.\n" + e.getMessage(),
                    "연결 실패",
                    JOptionPane.ERROR_MESSAGE);
        } catch (Exception e) {
            if (running) {
                System.out.println(e);
            }
        } finally {
            try {
                if (pw != null) pw.close();
                if (sock != null) sock.close();
            } catch (Exception e) {
                System.out.println(e);
            }
        }
    }
}