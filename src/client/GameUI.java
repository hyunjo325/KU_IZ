package client;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.net.*;
import java.io.*;

public class GameUI extends JPanel {
    private int currentRound;
    private int timeLeft;
    private String quizTopic;
    private boolean isPresenter;
    private JLabel timerLabel;
    private JLabel scoreLabel;
    private JLabel roundLabel;
    private JTextField answerInput;
    private JPanel drawingPanel;  // 그림 영역
    private Map<String, Integer> scoreMap;
    private List<String> players;
    private Timer roundTimer;

    public GameUI(String quizTopic, List<String> players) {
        this.quizTopic = quizTopic;
        this.players = players;
        this.isPresenter = isPresenter; // 출제자 여부 설정
        this.scoreMap = new HashMap<>();

        setLayout(new BorderLayout());
        initGameUI();
    }

    private void initGameUI() {
        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));

        roundLabel = new JLabel("ROUND 1", SwingConstants.CENTER);
        styleTopPanel(roundLabel);
        JPanel roundPanel = new JPanel();
        roundPanel.add(roundLabel);

        timerLabel = new JLabel("남은 시간 : 120초");
        scoreLabel = new JLabel("점수: 0점");
        JPanel infoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        infoPanel.add(timerLabel);
        infoPanel.add(scoreLabel);

        topPanel.add(roundPanel);
        topPanel.add(infoPanel);

        add(topPanel, BorderLayout.NORTH);

        drawingPanel = new JPanel();
        drawingPanel.setBorder(BorderFactory.createLineBorder(new Color(0x3B5998), 2));
        add(drawingPanel, BorderLayout.CENTER);

        JPanel answerPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        answerInput = new JTextField("", 20);
        answerInput.setPreferredSize(new Dimension(100, 40));
        answerInput.setForeground(Color.BLACK);

        JButton submitBtn = new JButton("OK");
        submitBtn.setPreferredSize(new Dimension(100, 40));
        submitBtn.setBackground(new Color(0x3B5998));
        submitBtn.setForeground(Color.BLACK);
        submitBtn.setFocusPainted(false);

        submitBtn.addActionListener(e -> {
            // 정답 제출 로직
            if (checkAnswer()) {
                roundTimer.stop();
                currentRound++;
                startRound();
            }
        });

        answerPanel.add(answerInput);
        answerPanel.add(submitBtn);

        add(answerPanel, BorderLayout.SOUTH);

        // 소켓 통신을 통해 그림을 업데이트 받는 메서드 호출
        if (!isPresenter) {
            receiveDrawingUpdates();
        }
    }

    private boolean checkAnswer() {
        // 정답 확인 로직 -> 맞췄다고 가정
        String answer = answerInput.getText().trim();
        return answer.equalsIgnoreCase("정답");
    }

    public void startGame() {
        currentRound = 1;
        startRound();
    }

    private void startRound() {
        if (currentRound > 10) {
            showFinalScores();
            return;
        }

        // 라운드 표시 업데이트
        roundLabel.setText("ROUND " + currentRound);

        // 제한 시간 설정
        timeLeft = 120;
        timerLabel.setText("남은 시간 : " + timeLeft + "초");

        // 타이머 설정
        roundTimer = new Timer(1000, e -> {
            timeLeft--;
            timerLabel.setText("남은 시간 : " + timeLeft + "초");
            if (timeLeft <= 0) {
                roundTimer.stop();
                currentRound++;
                startRound();
            }
        });
        roundTimer.start();
    }

    private void receiveDrawingUpdates() {
        // 소켓 통신으로 출제자의 그림 보는 로직
    }

    private void showFinalScores() {
        removeAll();
        JLabel finalLabel = new JLabel("게임 종료! 최종 점수 표시", SwingConstants.CENTER);
        add(finalLabel, BorderLayout.CENTER);
        revalidate();
        repaint();
    }

    private void styleTopPanel(JLabel topLabel) {
        topLabel.setOpaque(true);
        topLabel.setBackground(new Color(0x3B5998));
        topLabel.setForeground(Color.WHITE);
        topLabel.setFont(new Font("Arial", Font.BOLD, 18));
        topLabel.setPreferredSize(new Dimension(600, 40));
    }
}
