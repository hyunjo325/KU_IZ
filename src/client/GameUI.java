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
import java.util.Vector;

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
    private JLabel playerInfoLabel; // 참가자 이름
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
    private UserData userData;
    private Vector<Image> savedDrawings = new Vector<>();
    private Vector<String> savedAnswers = new Vector<>();

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
        scoreLabel = new JLabel("/ 점수: 0점");
        playerInfoLabel = new JLabel("/ 플레이어: "+ userdata.getUsername());

        JPanel infoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        infoPanel.add(timerLabel);
        infoPanel.add(scoreLabel);
        infoPanel.add(playerInfoLabel);

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
        colorBtn.setForeground(Color.BLACK);
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
        clearBtn.setForeground(Color.BLACK);
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
                //currentRound++;
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

            savedDrawings.add(drawingPanel.getImg_buffer());
            clearDrawingPanel();
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
            }

            // UI 갱신
            revalidate();
            repaint();
        });
    }

    public void startGame() {
        startRound();
    }

    private void startRound() {
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
        topLabel.setFont(new Font("default", Font.BOLD, 18));
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
            savedAnswers.add(word);
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

            savedDrawings.add(drawingPanel.getImg_buffer());
            startRound();
            // 새로운 출제자 설정
            handlePresenterChange(newPresenter);
            clearDrawingPanel();

        });
    }

    public void showFinalResults(Map<String, Integer> finalScores, List<String> winners, int finishedRound) {
        SwingUtilities.invokeLater(() -> {
            // 게임 UI 클리어
            removeAll();
            setLayout(new BorderLayout());

            // 결과 패널 생성
            JPanel resultsPanel = new JPanel(new BorderLayout());
            resultsPanel.setBackground(Color.WHITE);
            resultsPanel.setBorder(BorderFactory.createEmptyBorder(5, 0, 30, 0));

            // 상단 "최종 순위" 라벨
            JLabel topLabel = new JLabel("최종 순위", SwingConstants.CENTER);
            styleTopPanel(topLabel);
            resultsPanel.add(topLabel, BorderLayout.NORTH);

            // 중앙 결과 패널
            JPanel centerPanel = new JPanel();
            centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
            centerPanel.setBackground(Color.WHITE);

            // 우승자 표시
            JLabel winnerLabel = new JLabel("우승자: " + String.join(", ", winners), SwingConstants.CENTER);
            winnerLabel.setFont(new Font("default", Font.BOLD, 24));
            winnerLabel.setForeground(Color.BLACK);
            winnerLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            centerPanel.add(winnerLabel);

            // 간격 추가
            centerPanel.add(Box.createVerticalStrut(30));

            // 모든 플레이어 점수 표시
            JPanel scoresPanel = new JPanel(new GridLayout(4, 2, 10, 10));
            scoresPanel.setBackground(Color.WHITE);

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

            centerPanel.add(scoresPanel);

            // '그린 그림 보기' 버튼
            JButton viewDrawingsBtn = new JButton("그린 그림 보기");
            viewDrawingsBtn.setPreferredSize(new Dimension(100, 40));
            viewDrawingsBtn.setBackground(new Color(0x3B5998));
            viewDrawingsBtn.setForeground(Color.BLACK);
            viewDrawingsBtn.setFocusPainted(false);
            viewDrawingsBtn.addActionListener(e -> {
                removeAll();
                add(showDrawingGalleryPanel(finishedRound), BorderLayout.CENTER);
                revalidate();
                repaint();
            });

//            centerPanel.add(Box.createVerticalStrut(20));
            JPanel btnPanel = new JPanel();
            btnPanel.setBackground(Color.WHITE);
            btnPanel.add(viewDrawingsBtn);
            resultsPanel.add(btnPanel, BorderLayout.SOUTH);

            // 메인 패널에 중앙 패널 추가
            resultsPanel.add(centerPanel, BorderLayout.CENTER);
            add(resultsPanel);

            // UI 갱신
            revalidate();
            repaint();
        });
    }

    private JPanel showDrawingGalleryPanel(int finishedRound) {
        // 메인 패널 생성
        JPanel galleryPanel = new JPanel(new BorderLayout());
        galleryPanel.setBorder(BorderFactory.createEmptyBorder(5, 0, 20, 0));
        galleryPanel.setBackground(Color.WHITE);

        // 상단 "최종 순위" 라벨
        JLabel topLabel = new JLabel("그린 그림 목록", SwingConstants.CENTER);
        styleTopPanel(topLabel);
        galleryPanel.add(topLabel, BorderLayout.NORTH);

        // 그림 목록 패널 생성 (2행 5열)
        JPanel drawingListPanel = new JPanel(new GridLayout(2, 5, 10, 10));
        drawingListPanel.setBackground(Color.WHITE);
        System.out.println("savedAnswers"+savedAnswers);
        // 임시로 그림 데이터 채워넣음
        for (int i = 0; i < finishedRound; i++) {
            JPanel drawingPanel = new JPanel(new BorderLayout());
            drawingPanel.setBackground(Color.WHITE);
            drawingPanel.setBorder(BorderFactory.createTitledBorder(savedAnswers.get(i)));
            drawingPanel.setPreferredSize(new Dimension(100, 100));
            JLabel imageLabel = new JLabel(new ImageIcon(savedDrawings.get(i)));
            drawingPanel.add(imageLabel, BorderLayout.CENTER);
            /*JLabel placeholder = new JLabel("그림 " + i, SwingConstants.CENTER);
            placeholder.setPreferredSize(new Dimension(80, 80));
            placeholder.setOpaque(true);
            placeholder.setBackground(new Color(200, 200, 200));

            drawingPanel.add(placeholder);*/
            drawingListPanel.add(drawingPanel);
        }

        JButton exitBtn = new JButton("EXIT");

        exitBtn.setPreferredSize(new Dimension(100, 40));
        exitBtn.setBackground(new Color(0x3B5998));
        exitBtn.setForeground(Color.BLACK);
        exitBtn.setFocusPainted(false);

        exitBtn.addActionListener(e -> {
            SwingUtilities.invokeLater(() -> {
            // 프로그램 종료
            System.exit(0);
            });
        });

        // 버튼 패널 생성
        JPanel buttonPanel = new JPanel();
        buttonPanel.setBackground(Color.WHITE);
        buttonPanel.add(exitBtn);

        galleryPanel.add(drawingListPanel, BorderLayout.CENTER);
        galleryPanel.add(buttonPanel, BorderLayout.SOUTH);

        return galleryPanel;
    }
    public void showDrawings(){
        JPanel panel = new JPanel(new GridLayout(2, 5, 10, 10)); // 2줄 5칸, 간격 10px

        // 이미지와 이름 추가
        for (int i = 0; i < savedDrawings.size(); i++) {
            // 이미지와 이름을 포함할 JPanel
            JPanel imagePanel = new JPanel(new BorderLayout());

            // 이미지 추가
            JLabel imageLabel = new JLabel(new ImageIcon(savedDrawings.get(i)));
            imagePanel.add(imageLabel, BorderLayout.CENTER);

            // 이름 추가
            JLabel nameLabel = new JLabel(savedAnswers.get(i), SwingConstants.CENTER);
            imagePanel.add(nameLabel, BorderLayout.SOUTH);

            panel.add(imagePanel);
        }

        // 패널을 프레임에 추가
        add(panel, BorderLayout.CENTER);
        revalidate();
        repaint();
    }

    public void handlePresenterDisconnected(String disconnectedUser) {
        SwingUtilities.invokeLater(() -> {
            synchronized (savedAnswers) {
                savedAnswers.remove(savedAnswers.lastElement());
            }
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
            currentRound = round;
        });
    }
}
