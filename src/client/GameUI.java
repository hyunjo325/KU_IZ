package client;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.net.*;
import java.io.*;

public class GameUI extends JPanel {
    private int currentRound;
    private int timeLeft;
    private Socket sock = null;
    private PrintWriter pw = null;
    private BufferedReader br = null;
    private String quizTopic;
    private boolean isPresenter;
    private JLabel timerLabel;
    private JLabel scoreLabel;
    private JLabel roundLabel;
    private JLabel wordLabel;  // 제시어
    private JTextField answerInput;
    private DrawingPanel drawingPanel;  // 그림 영역
    private Map<String, Integer> scoreMap;
    private List<String> players;
    private Timer roundTimer;
    private int selectedColor = 10;

    public GameUI(String quizTopic, List<String> players, boolean isPresenter, Socket sock, PrintWriter pw, BufferedReader br) {
        this.quizTopic = quizTopic;
        this.players = players;
        this.isPresenter = isPresenter; // 출제자 여부 설정
        this.scoreMap = new HashMap<>();
        this.sock = sock;
        this.pw = pw;
        this.br = br;
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

        // 제시어 라벨 추가
        wordLabel = new JLabel("", SwingConstants.CENTER);
        wordLabel.setFont(new Font("Arial", Font.BOLD, 16));
        if (!isPresenter) {
            wordLabel.setText("");
        }
        JPanel wordPanel = new JPanel();
        wordPanel.add(wordLabel);

        timerLabel = new JLabel("남은 시간 : "+ timeLeft + "초");
        scoreLabel = new JLabel("점수: 0점");
        JPanel infoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        infoPanel.add(timerLabel);
        infoPanel.add(scoreLabel);

        topPanel.add(roundPanel);
        topPanel.add(wordPanel);
        topPanel.add(infoPanel);

        add(topPanel, BorderLayout.NORTH);

        drawingPanel = new DrawingPanel(isPresenter, sock, pw, br);
        drawingPanel.setBorder(BorderFactory.createLineBorder(new Color(0x3B5998), 2));
        add(drawingPanel, BorderLayout.CENTER);

        JPanel answerPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        answerInput = new JTextField("", 20);
        answerInput.setPreferredSize(new Dimension(100, 40));
        answerInput.setForeground(Color.BLACK);

        // 색상 선택 버튼 생성
        JButton colorBtn = new JButton("색상 선택");
        colorBtn.setForeground(Color.WHITE);
        colorBtn.setPreferredSize(new Dimension(100, 40));
        colorBtn.setBackground(new Color(0x3B5998));

        // 팝업 메뉴 생성
        JPopupMenu colorMenu = new JPopupMenu();
        Color[] colors = {Color.RED, Color.BLUE, Color.GREEN, Color.YELLOW, Color.ORANGE,
                Color.PINK, Color.MAGENTA, Color.CYAN, Color.WHITE, Color.GRAY, Color.BLACK};
        // 색상 항목 추가
        for (int i = 0; i< colors.length; i++) {
            JMenuItem colorItem = new JMenuItem();
            colorItem.setBackground(colors[i]);
            colorItem.setPreferredSize(new Dimension(40, 20));
            int finalI = i;
            colorItem.addActionListener(e -> {
                if(isPresenter) {
                    selectedColor = finalI;
                    drawingPanel.setLineColor(selectedColor);
                    Border border = new LineBorder(colors[selectedColor], 2);
                    colorBtn.setBorder(border);
                }
            });
            colorMenu.add(colorItem);
        }

        // 버튼 클릭 시 팝업 메뉴 표시
        colorBtn.addActionListener(e -> colorMenu.show(colorBtn, 0, -colorMenu.getPreferredSize().height));



        JButton submitBtn = new JButton("OK");
        submitBtn.setPreferredSize(new Dimension(80, 40));
        submitBtn.setBackground(new Color(0x3B5998));
        submitBtn.setForeground(Color.WHITE);
        submitBtn.setFocusPainted(false);

        submitBtn.addActionListener(e -> {
            // 정답 제출 로직
            if (checkAnswer()) {
                roundTimer.stop();
                currentRound++;
                startRound();
            }
        });

        // 지우기 버튼 생성
        JButton clearBtn = new JButton("지우기");
        clearBtn.setForeground(Color.WHITE);
        clearBtn.setPreferredSize(new Dimension(80, 40));
        clearBtn.setBackground(new Color(0x3B5998));
        clearBtn.addActionListener(e -> {
            if(isPresenter)
                synchronized (pw) {
                    pw.println("DRAW#CLEAR");
                    pw.flush();
                }
        });

        answerPanel.add(colorBtn);
        answerPanel.add(answerInput);
        answerPanel.add(submitBtn);
        answerPanel.add(clearBtn);

        add(answerPanel, BorderLayout.SOUTH);

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

    public void receiveDrawingUpdates(LineInfo newLine) {
        drawingPanel.addLineInfo(newLine);
    }
    public void clearDrawingPanel() {
        drawingPanel.clearVector();
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

    public void updateWord(String word) {
        if (isPresenter) {
            wordLabel.setText("제시어: " + word);
        }
    }

}
