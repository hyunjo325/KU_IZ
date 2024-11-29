package client;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.BufferedReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Vector;

public class DrawingPanel extends JPanel implements MouseListener, MouseMotionListener, ItemListener, ActionListener {

    public Graphics Graphics_buffer; //더블버퍼링
    public Image Img_buffer;
    private final Vector<LineInfo> drawVector = new Vector<>();
    private int lineColor = 10;
    private Socket sock = null;
    private PrintWriter pw = null;
    private BufferedReader br = null;
    private boolean isPresenter = false;

    private final Color[] colors = {Color.RED, Color.BLUE, Color.GREEN, Color.YELLOW, Color.ORANGE,
            Color.PINK, Color.MAGENTA, Color.CYAN, Color.WHITE, Color.GRAY, Color.BLACK};
    public DrawingPanel(boolean isPresenter, Socket sock, PrintWriter pw, BufferedReader br){
        this.sock = sock;
        this.pw = pw;
        this.br = br;
        this.isPresenter = isPresenter;
        this.addMouseListener(this);
        this.addMouseMotionListener(this);
        this.setVisible(true);
    }
    public void paint(Graphics g){
        //더블 버퍼링을 위한 설정
        Img_buffer = createImage(getWidth(),getHeight());
        Graphics_buffer = Img_buffer.getGraphics();
        update(g);
    }
    public void update(Graphics g) {
        Graphics_buffer.clearRect(0, 0, this.getWidth(), this.getHeight());
        ((Graphics2D) Graphics_buffer).setStroke(new BasicStroke(2, BasicStroke.CAP_ROUND, 0));
        for (int i = 1; i < drawVector.size(); i++) {
            if (drawVector.get(i - 1) == null)
                continue;
            else if (drawVector.get(i) == null)
                continue;
            else{
                Graphics_buffer.setColor(drawVector.get(i).getColor());
                Graphics_buffer.drawLine((int) drawVector.get(i - 1).getX(), (int) drawVector.get(i - 1).getY(),(int) drawVector.get(i).getX(), (int) drawVector.get(i).getY());
            }
        }
        g.drawImage(Img_buffer, 0, 0, this);
    }
    public void setLineColor(int color){
        lineColor = color;
    }
    public void clearVector(){
        synchronized (drawVector) {
            drawVector.clear();
        }
        repaint();
    }
    public void setPresenter(boolean tf){
        isPresenter = tf;
    }
    public void addLineInfo(LineInfo newLine){
        synchronized (drawVector) {
            drawVector.add(newLine);
        }
        repaint();
    }
    public Image getImg_buffer(){
        return Img_buffer.getScaledInstance(getWidth()/5, getHeight()/5, Image.SCALE_SMOOTH);
    }
    @Override
    public void actionPerformed(ActionEvent e) {

    }

    @Override
    public void itemStateChanged(ItemEvent e) {

    }

    @Override
    public void mouseClicked(MouseEvent e) {
    }

    @Override
    public void mousePressed(MouseEvent e) {
        if(isPresenter){
            synchronized (pw) {
                pw.println("DRAW#NULL");
                //drawVector.add(null);
                LineInfo newLine = new LineInfo(e.getPoint(), lineColor);
                //drawVector.add(newLine);
                pw.println("DRAW#" + newLine.getInfo());
                pw.flush();
            }
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {

    }

    @Override
    public void mouseEntered(MouseEvent e) {

    }

    @Override
    public void mouseExited(MouseEvent e) {

    }

    @Override
    public void mouseDragged(MouseEvent e) {
        if(isPresenter){
            synchronized (pw) {
                LineInfo newLine = new LineInfo(e.getPoint(), lineColor);
                //drawVector.add(newLine);
                pw.println("DRAW#" + newLine.getInfo());
                pw.flush();
            }
        }
    }

    @Override
    public void mouseMoved(MouseEvent e) {

    }
}
