package client;

import java.io.PrintWriter;

public class UserData {
    private String username;
    private boolean isRoomOwner;

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
}
