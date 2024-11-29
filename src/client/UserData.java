package client;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

public class UserData {
    private String username;
    private boolean isRoomOwner;
    private String quizTopic = null;
    private List<String> playerList = new ArrayList<String>();

    UserData(String username, boolean isRoomOwner) {
        this.username = username;
        this.isRoomOwner = isRoomOwner;
    }

    public String getUsername(){
        return username;
    }

    public void setUsername(String username){
        this.username = username;
    }

    public boolean getIsRoomOwner() {
        return isRoomOwner;
    }

    public void setIsRoomOwner(boolean isRoomOwner) {
        this.isRoomOwner = isRoomOwner;
    }

    public String getQuizTopic() {
        return quizTopic;
    }

    public void setQuizTopic(String quizTopic) {
        this.quizTopic = quizTopic;
    }

    public List<String> getPlayerList() {
        return playerList;
    }

    public void addPlayerToList(String player) {
        playerList.add(player);
    }

    public void removePlayerFromList(String player) {
        playerList.remove(player);
    }

    public void updatePlayerList(List<String> playerList){
        this.playerList = playerList;
    }
}
