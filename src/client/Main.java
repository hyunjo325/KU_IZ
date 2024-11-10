package client;

import java.net.*;
import java.io.*;

public class Main {
    public static void main(String[] args) {
        Socket sock = null;
        BufferedReader br = null;
        PrintWriter pw = null;
        boolean running = true;
        UserData userdata = new UserData("null", false);

        try{
            sock = new Socket("localhost", 20000);
            pw = new PrintWriter(new OutputStreamWriter(sock.getOutputStream()));
            br = new BufferedReader(new InputStreamReader(sock.getInputStream()));

            BufferedReader scanner = new BufferedReader(new InputStreamReader(System.in));

            LobbyThread game = new LobbyThread(sock, pw, br, userdata);
            game.start();
            game.join();

            /*String line = null;
            while(true){
                //종료 조건
            }
            while ((line = scanner.readLine()) != null){
                pw.println(line);
                pw.flush();

                if (line.equals("/quit")){
                    running = false;
                    break;
                }
            }*/
        }
        catch (Exception e) {
            if (running){
                System.out.println(e);
            }
        }
        finally {
            try {
                if (pw != null) {pw.close();}
                if (sock != null) {sock.close();}
            }
            catch (Exception e) {System.out.println(e);}
        }

    }
}
