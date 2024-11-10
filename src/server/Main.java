package server;

import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Vector;

public class Main {
    public static void main(String[] args) {
        GameInfo game = new GameInfo();
        Vector<UserPair> userVector = new Vector();
        int usernum = 1;

        try{
            ServerSocket server = new ServerSocket(20000);
            System.out.println("Waiting Client");
            while (true){
                Socket sock = server.accept();
                if (!game.isRunning()) {
                    ClientThread clientthread = new ClientThread(sock, userVector, usernum, game);
                    clientthread.start();
                    usernum++;
                }
                else {
                    PrintWriter pw = new PrintWriter(new OutputStreamWriter(sock.getOutputStream()));
                    pw.println("GAME_RUNNING");
                    pw.flush();
                    pw.close();
                    sock.close();
                }
            }
        }
        catch (Exception e) {System.out.println(e);}

    }
}