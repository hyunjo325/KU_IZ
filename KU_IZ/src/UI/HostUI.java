package UI;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

public class HostUI extends JFrame {

    private final boolean isHost;
    private CardLayout cardLayout;
    private JPanel mainPanel;
    private JPanel playerListPanel;
    private String quizTopic;
    private GameUI gameUI;

    public HostUI(boolean isHost) {
        this.isHost = isHost;
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
                quizTopic = "건국대학교";
            }
        });

        btn2.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                quizTopic = "컴퓨터 공학";
            }
        });

        okBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (quizTopic != null) {
                    cardLayout.show(mainPanel, "WaitingRoom");
                    updatePlayerList(getPlayerListFromServer());
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
            if (quizTopic != null) {
                topLabel = new JLabel("퀴즈 주제: " + quizTopic, SwingConstants.CENTER);
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
        startGameBtn.addActionListener(e -> startGame());

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

    private void startGame() {
        gameUI = new GameUI(quizTopic, getPlayerListFromServer());
        mainPanel.add(gameUI, "GameUI");
        cardLayout.show(mainPanel, "GameUI");
        gameUI.startGame();
    }

    // 플레이어 목록 업데이트 메서드
    private void updatePlayerList(List<String> players) {
        playerListPanel.removeAll();
        for (String player : players) {
            JLabel playerLabel = new JLabel(player, SwingConstants.CENTER);
            playerLabel.setFont(new Font("Arial", Font.BOLD, 14));
            playerListPanel.add(playerLabel);
        }
        playerListPanel.revalidate();
        playerListPanel.repaint();
    }

     //서버에서 플레이어 목록을 가져오는 메서드 (임시)
    private List<String> getPlayerListFromServer() {
        // 실제 서버와 통신하여 플레이어 목록을 받아와야 함 -> 우선 임시로 지정
        return List.of("PLAYER 1", "PLAYER 2", "PLAYER 3", "PLAYER 4", "PLAYER 5", "PLAYER 6", "PLAYER 8");
    }


    private void styleTopPanel(JLabel topLabel) {
        topLabel.setOpaque(true);
        topLabel.setBackground(new Color(0x3B5998));
        topLabel.setForeground(Color.WHITE);
        topLabel.setFont(new Font("Arial", Font.BOLD, 18));
        topLabel.setPreferredSize(new Dimension(600, 40));
    }

    public static void main(String[] args) {
       new HostUI(true); // 방장
       // new HostUI(false); // 참가자
    }
}
