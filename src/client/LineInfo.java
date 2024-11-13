package client;

import java.awt.*;

public class LineInfo {
    private Point p;
    private int color;
    public LineInfo(Point input, int selectedColor){
        this.p = input;
        this.color = selectedColor;
    }
    public double getX(){
        return p.getX();
    }
    public double getY(){
        return p.getY();
    }
    public Color getColor(){
        Color[] colors = {Color.RED, Color.BLUE, Color.GREEN, Color.YELLOW, Color.ORANGE,
                Color.PINK, Color.MAGENTA, Color.CYAN, Color.WHITE, Color.GRAY, Color.BLACK};
        return colors[color];
    }
    public String getInfo(){
        String str = "";
        str += (int)p.getX()+"#"+(int)p.getY()+"#"+color;
        return str;
    }
}
