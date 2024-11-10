package server;

import java.io.PrintWriter;

public class UserPair {
    private String username;
    private PrintWriter pw;

    UserPair(String username, PrintWriter pw) {
        this.username = username;
        this.pw = pw;
    }

    public String getUsername(){
        return username;
    }

    public PrintWriter getPw(){
        return pw;
    }

    public void setUsername(String username){
        this.username = username;
    }

    public void setPw(PrintWriter pw) {
        this.pw = pw;
    }

}
