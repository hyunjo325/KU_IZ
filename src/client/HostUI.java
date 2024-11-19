package client;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.net.*;
import java.io.*;

public class HostUI extends JFrame {

    private Socket sock = null;
    private PrintWriter pw = null;
    private BufferedReader br = null;
    private UserData userdata = null;
    private final boolean isHost;
    private CardLayout cardLayout;
    private JPanel mainPanel;
    private JPanel playerListPanel;
    private GameUI gameUI;


    public HostUI(boolean isHost, Socket sock, PrintWriter pw, BufferedReader br, UserData userdata) {
        this.isHost = isHost;
        this.sock = sock;
        this.pw = pw;
        this.br = br;
        this.userdata = userdata;

        setTitle("Team6");
        setSize(600, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);

        if (isHost) {
            JPanel createRoom = createRoom();
            mainPanel.add(createRoom, "CreateRoom");
        }

        JPanel waitingRoom = waitingRoom();
        mainPanel.add(waitingRoom, "WaitingRoom");

        add(mainPanel);
        setVisible(true);

        // 일반 참가자 -> 대기실
        if (!isHost) {
            cardLayout.show(mainPanel, "WaitingRoom");
        }
    }

    private JPanel createRoom(){
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);

        JLabel topLabel = new JLabel("방 개설", SwingConstants.CENTER);
        styleTopPanel(topLabel);
        panel.add(topLabel, BorderLayout.NORTH);

        JPanel btnPanel = new JPanel();
        btnPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 80, 80));

        JButton btn1 = new JButton("건국대학교");
        JButton btn2 = new JButton("컴퓨터 공학");

        btn1.setPreferredSize(new Dimension(130, 130));
        btn2.setPreferredSize(new Dimension(130, 130));

        btnPanel.add(btn1);
        btnPanel.add(btn2);
        panel.add(btnPanel, BorderLayout.CENTER);

        JButton okBtn = new JButton("OK");

        okBtn.setPreferredSize(new Dimension(100, 40));
        okBtn.setBackground(new Color(0x3B5998));
        okBtn.setForeground(Color.BLACK);
        okBtn.setFocusPainted(false);

        btn1.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                userdata.setQuizTopic("건국대학교");
            }
        });

        btn2.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                userdata.setQuizTopic("컴퓨터 공학");
            }
        });

        okBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (userdata.getQuizTopic() != null) {
                    cardLayout.show(mainPanel, "WaitingRoom");
                    sendQuizTopic();
                    updatePlayerList();
                } else {
                    JOptionPane.showMessageDialog(panel, "주제를 선택해 주세요.");
                }
            }
        });

        JPanel okPanel = new JPanel();
        okPanel.add(okBtn);
        panel.add(okPanel, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel waitingRoom(){
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);

        JLabel topLabel;
        if (isHost) {
            topLabel = new JLabel("대기방", SwingConstants.CENTER);
        } else {
            if (userdata.getQuizTopic() != null) {
                topLabel = new JLabel("퀴즈 주제: " + userdata.getQuizTopic(), SwingConstants.CENTER);
            }
            else{
                topLabel = new JLabel("퀴즈 주제: ", SwingConstants.CENTER);
            }
        }

        styleTopPanel(topLabel);
        panel.add(topLabel, BorderLayout.NORTH);

        playerListPanel = new JPanel(new GridLayout(2, 3, 20, 20));
        playerListPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(new Color(0x3B5998))));
        panel.add(playerListPanel, BorderLayout.CENTER);

        if (isHost) {
            // 방장 화면
            JButton startGameBtn = new JButton("게임 시작");

            startGameBtn.setPreferredSize(new Dimension(100, 40));
            startGameBtn.setBackground(new Color(0x3B5998));
            startGameBtn.setForeground(Color.BLACK);
            startGameBtn.setFocusPainted(false);
            startGameBtn.addActionListener(e -> sendStart());

            JPanel bottomPanel = new JPanel();
            bottomPanel.add(startGameBtn);
            panel.add(bottomPanel, BorderLayout.SOUTH);
        } else {
            // 참가자 화면
            JLabel waitingLabel = new JLabel("방장이 게임 시작하기를 기다리는 중", SwingConstants.CENTER);
            waitingLabel.setForeground(Color.GRAY);

            JPanel bottomPanel = new JPanel();
            bottomPanel.setBackground(Color.WHITE);
            bottomPanel.add(waitingLabel);
            panel.add(bottomPanel, BorderLayout.SOUTH);
        }

        return panel;
    }

    public void startGame() {
        gameUI = new GameUI(userdata.getQuizTopic(), userdata.getPlayerList(),userdata.getIsRoomOwner(),sock, pw, br, userdata);
        mainPanel.add(gameUI, "GameUI");
        cardLayout.show(mainPanel, "GameUI");
        gameUI.startGame();
    }

    // 플레이어 목록 업데이트 메서드
    public void updatePlayerList() {
        List<String> players = userdata.getPlayerList();
        playerListPanel.removeAll();
        for (String player : players) {
            JLabel playerLabel = new JLabel(player, SwingConstants.CENTER);
            playerLabel.setFont(new Font("default", Font.BOLD, 14));
            playerListPanel.add(playerLabel);
        }
        playerListPanel.revalidate();
        playerListPanel.repaint();
    }
    public GameUI getGameUI(){
        return gameUI;
    }

    private void styleTopPanel(JLabel topLabel) {
        topLabel.setOpaque(true);
        topLabel.setBackground(new Color(0x3B5998));
        topLabel.setForeground(Color.WHITE);
        topLabel.setFont(new Font("default", Font.BOLD, 18));
        topLabel.setPreferredSize(new Dimension(600, 40));
    }

    private void sendQuizTopic(){
        pw.println("SUBJECT#" + userdata.getQuizTopic());
        pw.flush();
    }

    private void sendStart(){
        pw.println("START_GAME");
        pw.flush();
    }
    public void updateQuizTopic(String topic) {
        if (!isHost) {  // 방장이 아닌 경우에만 업데이트
            JLabel topLabel = (JLabel) ((JPanel)mainPanel.getComponent(0)).getComponent(0);
            topLabel.setText("퀴즈 주제: " + topic);
            topLabel.revalidate();
            topLabel.repaint();
        }
    }
}
