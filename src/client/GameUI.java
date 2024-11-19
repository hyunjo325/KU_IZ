package client;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.ActionListener;
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
    private UserData userdata;
    private JButton colorBtn;
    private JButton submitBtn;
    private JButton clearBtn;


    public GameUI(String quizTopic, List<String> players, boolean isPresenter, Socket sock, PrintWriter pw, BufferedReader br, UserData userdata) {
        this.quizTopic = quizTopic;
        this.players = players;
        this.isPresenter = isPresenter; // 출제자 여부 설정
        this.scoreMap = new HashMap<>();
        this.sock = sock;
        this.pw = pw;
        this.br = br;
        this.userdata = userdata;
        this.scoreMap = new HashMap<>();
        for(String player : players){
            scoreMap.put(player,0);
        }
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

        wordLabel = new JLabel("", SwingConstants.CENTER);
        wordLabel.setFont(new Font("default", Font.BOLD, 16));
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
        colorBtn = new JButton("색상 선택");
        colorBtn.setForeground(Color.WHITE);
        colorBtn.setPreferredSize(new Dimension(100, 40));
        colorBtn.setBackground(new Color(0x3B5998));

        JPopupMenu colorMenu = new JPopupMenu();
        Color[] colors = {Color.RED, Color.BLUE, Color.GREEN, Color.YELLOW, Color.ORANGE,
                Color.PINK, Color.MAGENTA, Color.CYAN, Color.WHITE, Color.GRAY, Color.BLACK};

        // 색상 항목 추가
        for (int i = 0; i < colors.length; i++) {
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
        colorBtn.addActionListener(e -> {
            if(isPresenter) {
                colorMenu.show(colorBtn, 0, -colorMenu.getPreferredSize().height);
            }
        });

        // 지우기 버튼 생성
        clearBtn = new JButton("지우기");
        clearBtn.setForeground(Color.WHITE);
        clearBtn.setPreferredSize(new Dimension(80, 40));
        clearBtn.setBackground(new Color(0x3B5998));

        // 지우기 버튼에 리스너 추가
        clearBtn.addActionListener(e -> {
            if(isPresenter) {
                synchronized (pw) {
                    pw.println("DRAW#CLEAR");
                    pw.flush();
                }
            }
        });

        submitBtn = new JButton("OK");
        submitBtn.setPreferredSize(new Dimension(80, 40));
        submitBtn.setBackground(new Color(0x3B5998));
        submitBtn.setForeground(Color.BLACK);
        submitBtn.setFocusPainted(false);

        // 제시어를 맞추는 사람만 정답 제출 가능
        if (isPresenter) {
            answerInput.setEditable(false);
            submitBtn.setEnabled(false);
            answerInput.setText("당신은 출제자입니다");

            colorBtn.setEnabled(true);
            colorBtn.setVisible(true);
            clearBtn.setEnabled(true);
            clearBtn.setVisible(true);
        } else {
            answerInput.setEditable(true);
            submitBtn.setEnabled(true);

            colorBtn.setEnabled(false);
            colorBtn.setVisible(false);
            clearBtn.setEnabled(false);
            clearBtn.setVisible(false);
        }

        submitBtn.addActionListener(e -> {
            if (!isPresenter && checkAnswer()) {
                currentRound++;
                startRound();
            }
        });

        answerPanel.add(colorBtn);
        answerPanel.add(answerInput);
        answerPanel.add(submitBtn);
        answerPanel.add(clearBtn);

        add(answerPanel, BorderLayout.SOUTH);
    }

    private boolean checkAnswer() {
        // 입력값이 비어있는지 확인
        if (answerInput.getText().trim().isEmpty()) {
            return false;
        }

        String answer = answerInput.getText().trim();

        // 서버에 정답 확인 요청 전송
        synchronized (pw) {
            pw.println("ANSWER#" + userdata.getUsername() + "#" + answer);
            pw.flush();
        }

        // 입력창 초기화 및 비활성화
        answerInput.setText("");

        return false; // 서버의 응답을 기다리므로 항상 false 반환
    }

    // 서버로부터 정답 결과를 받았을 때 호출될 메서드
    public void handleAnswerResult(String username, boolean isCorrect) {
        SwingUtilities.invokeLater(() -> {
            answerInput.setEnabled(true);

            if (isCorrect) {
                JOptionPane.showMessageDialog(this,
                        username + "님이 정답을 맞추셨습니다!",
                        "정답 알림",
                        JOptionPane.INFORMATION_MESSAGE);

                // 정답자가 된 경우에도 UI 갱신
                if (username.equals(userdata.getUsername())) {
                    answerInput.setEditable(false);
                    submitBtn.setEnabled(false);
                    answerInput.setText("당신은 출제자입니다");

                    // 그리기 관련 버튼들 활성화
                    colorBtn.setEnabled(true);
                    colorBtn.setVisible(true);
                    clearBtn.setEnabled(true);
                    clearBtn.setVisible(true);
                }

                // 10라운드 체크
                if (currentRound >= 10) {
                    synchronized (pw) {
                        pw.println("GAME_END");
                        pw.flush();
                    }
                }
                clearDrawingPanel();
            }

            // UI 갱신
            revalidate();
            repaint();
        });
    }

    public void startGame() {
        currentRound = 1;
        startRound();
    }

    private void startRound() {
        // 라운드 표시 업데이트
        roundLabel.setText("ROUND " + currentRound);

        // 제한 시간 설정
        timeLeft = 120;
        timerLabel.setText("남은 시간 : " + timeLeft + "초");

    }

    // 서버로부터 시간 동기화 메시지를 받았을 때 호출될 메소드
    public void syncTime(int serverTime) {
        SwingUtilities.invokeLater(() -> {
            timeLeft = serverTime;
            timerLabel.setText("남은 시간 : " + timeLeft + "초");

            // 시간이 0이 되면 라운드 종료
            if (timeLeft <= 0) {
                currentRound++;
                startRound();
            }
        });
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
        topLabel.setFont(new Font("defualt", Font.BOLD, 18));
        topLabel.setPreferredSize(new Dimension(600, 40));
    }

    public void updateScores(String username, String score){
        int newScore = scoreMap.get(username);
        newScore += Integer.parseInt(score);
        scoreMap.put(username, newScore);
        // scoreLabel 업데이트 (자신의 점수만 표시)
        if (username.equals(userdata.getUsername())) {
            scoreLabel.setText("점수: " + newScore + "점");
        }
    }

    public void handlePresenterChange(String username) {
        isPresenter = userdata.getUsername().equals(username);
        drawingPanel.setPresenter(isPresenter);

        SwingUtilities.invokeLater(() -> {
            if (isPresenter) {
                // 출제자가 된 경우
                answerInput.setEditable(false);
                submitBtn.setEnabled(false);
                answerInput.setText("당신은 출제자입니다");

                // 그리기 관련 버튼들 활성화
                colorBtn.setEnabled(true);
                colorBtn.setVisible(true);
                clearBtn.setEnabled(true);
                clearBtn.setVisible(true);

                // 색상 초기화
                colorBtn.setBorder(null);
                selectedColor = 10;
                drawingPanel.setLineColor(selectedColor);

            } else {
                // 참가자가 된 경우
                answerInput.setEditable(true);
                submitBtn.setEnabled(true);
                answerInput.setText("");
                wordLabel.setText("");

                // 그리기 관련 버튼들 비활성화
                colorBtn.setEnabled(false);
                colorBtn.setVisible(false);
                clearBtn.setEnabled(false);
                clearBtn.setVisible(false);
            }

            revalidate();
            repaint();
        });
    }

    public void updateWord(String word) {
        SwingUtilities.invokeLater(() -> {
            if (isPresenter) {
                wordLabel.setText("제시어: " + word);
            } else {
                wordLabel.setText(""); // 참가자는 제시어를 볼 수 없음
            }
        });
    }

    public void handleTimeUp(String newPresenter) {
        SwingUtilities.invokeLater(() -> {
            // 시간 초과 메시지 표시
            JOptionPane.showMessageDialog(this,
                    "시간이 초과되었습니다!\n다음 출제자: " + newPresenter,
                    "라운드 종료",
                    JOptionPane.INFORMATION_MESSAGE);

            currentRound++;

            // 10라운드 체크
            if (currentRound > 10) {
                // 서버에 게임 종료 요청
                synchronized (pw) {
                    pw.println("GAME_END");
                    pw.flush();
                }
            } else {
                startRound();
                // 새로운 출제자 설정
                handlePresenterChange(newPresenter);
                clearDrawingPanel();
            }
        });
    }
    public void showFinalResults(Map<String, Integer> finalScores, List<String> winners) {
        SwingUtilities.invokeLater(() -> {
            // 게임 UI 클리어
            removeAll();
            setLayout(new BorderLayout());

            // 결과 패널 생성
            JPanel resultsPanel = new JPanel();
            resultsPanel.setLayout(new BoxLayout(resultsPanel, BoxLayout.Y_AXIS));
            resultsPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

            // 우승자 표시
            JLabel winnerLabel = new JLabel("우승자: " + String.join(", ", winners), SwingConstants.CENTER);
            wordLabel.setFont(new Font("default", Font.BOLD, 20));
            winnerLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            resultsPanel.add(winnerLabel);
            resultsPanel.add(Box.createVerticalStrut(20));

            // 모든 플레이어 점수 표시
            JPanel scoresPanel = new JPanel(new GridLayout(0, 1, 5, 5));
            scoresPanel.setBorder(BorderFactory.createTitledBorder("최종 점수"));

            // 점수 순으로 정렬
            finalScores.entrySet().stream()
                    .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                    .forEach(entry -> {
                        JLabel scoreLabel = new JLabel(
                                entry.getKey() + ": " + entry.getValue() + "점",
                                SwingConstants.CENTER
                        );
                        scoreLabel.setFont(new Font("default", Font.PLAIN, 18));
                        if (winners.contains(entry.getKey())) {
                            scoreLabel.setForeground(new Color(0x3B5998));
                            scoreLabel.setFont(scoreLabel.getFont().deriveFont(Font.BOLD));
                        }
                        scoresPanel.add(scoreLabel);
                    });

            resultsPanel.add(scoresPanel);
            add(resultsPanel, BorderLayout.CENTER);

            revalidate();
            repaint();
        });
    }
    public void handlePresenterDisconnected(String disconnectedUser) {
        SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(this,
                    disconnectedUser + "님의 연결이 끊어졌습니다.",
                    "출제자 연결 종료",
                    JOptionPane.INFORMATION_MESSAGE);

            // 그리기 패널 초기화
            clearDrawingPanel();
        });
    }

    public void handleWrongAnswer(String player) {
        if (!player.equals(userdata.getUsername())) {
            return;  // 본인이 틀린 경우에만 처리
        }

        SwingUtilities.invokeLater(() -> {
            // 원래 배경색 저장
            Color originalColor = answerInput.getBackground();

            Timer blinkTimer = new Timer(200, null);
            final int[] blinkCount = {0};

            blinkTimer.addActionListener(e -> {
                if (blinkCount[0] >= 6) {
                    blinkTimer.stop();
                    answerInput.setBackground(originalColor);
                    answerInput.setEnabled(true);
                    return;
                }

                if (blinkCount[0] % 2 == 0) {
                    answerInput.setBackground(new Color(255, 200, 200));
                } else {
                    answerInput.setBackground(originalColor);
                }
                blinkCount[0]++;
            });

            blinkTimer.start();
        });
    }
    public void updateRoundDisplay(int round) {
        SwingUtilities.invokeLater(() -> {
            roundLabel.setText("ROUND " + round);
        });
    }
}
