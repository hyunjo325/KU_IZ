package server;

public class GameInfo {
    private String subject;
    private boolean running = false;

    public GameInfo(){
        this.subject = "";
        this.running = false;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public boolean isRunning() {
        return running;
    }

    public void setRunning(boolean running) {
        this.running = running;
    }
}
