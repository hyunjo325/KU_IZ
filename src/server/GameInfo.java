package server;

public class GameInfo {
    private int subject;
    private boolean running = false;

    public GameInfo(){
        this.subject = 0;
        this.running = false;
    }

    public int getSubject() {
        return subject;
    }

    public void setSubject(int subject) {
        this.subject = subject;
    }

    public boolean isRunning() {
        return running;
    }

    public void setRunning(boolean running) {
        this.running = running;
    }
}
