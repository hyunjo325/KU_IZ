package server;

import java.util.*;
import java.util.stream.Collectors;

public class GameInfo {
    private String subject;
    private boolean running = false;
    private Map<String, List<String>> wordsBySubject;
    private String currentWord;
    private Random random = new Random();
    private Set<String> usedWords; // 이미 사용된 단어
    private String currentPresenter;
    private int timeLeft = 120; // 2분
    private Timer gameTimer;
    private Vector<UserPair> userVector;
    private int currentRound = 1;

    public GameInfo(Vector<UserPair> userVector){
        this.subject = "";
        this.running = false;
        this.usedWords = new HashSet<>();
        this.userVector = userVector;
        initializeWords();
        setupTimer();
    }

    private void initializeWords() {
        wordsBySubject = new HashMap<>();

        // 건국대학교 관련 제시어
        List<String> konkukWords = Arrays.asList(
                "상허문", "일감호", "청심대", "새천년관", "학생회관",
                "중앙도서관","공과대학", "황소상","건국대학교병원" , "건구스"
        );

        // 컴퓨터 공학 관련 제시어
        List<String> computerWords = Arrays.asList(
                "알고리즘", "데이터베이스", "운영체제", "자료구조", "프로그래밍",
                "네트워크", "인공지능", "컴파일러", "보안", "소프트웨어공학",
                "웹개발", "모바일앱", "클라우드", "빅데이터", "머신러닝"
        );

        wordsBySubject.put("건국대학교", konkukWords);
        wordsBySubject.put("컴퓨터 공학", computerWords);
    }

    public String getRandomWord() {
        List<String> words = wordsBySubject.get(subject);
        if (words == null || words.isEmpty()) {
            return "ERROR: No words available";
        }

        // 아직 사용되지 않은 단어들만 선택
        List<String> availableWords = words.stream()
                .filter(word -> !usedWords.contains(word))
                .collect(Collectors.toList());

        if (availableWords.isEmpty()) {
            // 모든 단어를 사용했다면 초기화
            usedWords.clear();
            availableWords = new ArrayList<>(words);
        }

        currentWord = availableWords.get(random.nextInt(availableWords.size()));
        usedWords.add(currentWord);
        return currentWord;
    }

    public void setupTimer() {
        if (gameTimer != null) {
            gameTimer.cancel();
            gameTimer.purge();
        }

        gameTimer = new Timer();
        timeLeft = 120; // 타이머 리셋

        gameTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (running && timeLeft >= 0) {
                    // 모든 클라이언트에게 현재 시간 전송
                    synchronized(userVector) {
                        for (UserPair user : userVector) {
                            user.getPw().println("TIME#" + timeLeft);
                            user.getPw().flush();
                        }
                    }

                    if (timeLeft == 0) {
                        // 라운드 종료 처리
                        String nextPresenter = selectRandomPresenter();
                        setCurrentPresenter(nextPresenter);
                        String newWord = getRandomWord();
                        // 모든 클라이언트에게 라운드 종료와 새로운 출제자 알림
                        synchronized(userVector) {
                            for (UserPair user : userVector) {
                                user.getPw().println("TIME_UP#" + currentPresenter);
                                user.getPw().flush();
                            }
                        }
                        synchronized(userVector) {
                            for (UserPair user : userVector) {
                                if (user.getUsername().equals(currentPresenter)) {
                                    user.getPw().println("SUBJECT_WORD#" + currentPresenter + "#" + newWord);
                                    user.getPw().flush();
                                }
                            }
                        }
                        timeLeft = 120; // 다음 라운드를 위해 리셋
                    }
                    timeLeft--;
                }
            }
        }, 0, 1000); // 1초마다 실행
    }

    // 랜덤으로 다음 출제자 선택
    public String selectRandomPresenter() {
        if (userVector == null || userVector.isEmpty()) {
            return null;
        }

        // 현재 출제자를 제외한 다른 사용자 중에서 선택
        List<String> availablePresenters = userVector.stream()
                .map(UserPair::getUsername)
                .filter(username -> !username.equals(currentPresenter))
                .collect(Collectors.toList());

        return availablePresenters.get(random.nextInt(availablePresenters.size()));
    }

    public String getCurrentWord() {
        return currentWord;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public boolean isRunning() {
        return running;
    }

    public void setRunning(boolean running) {
        this.running = running;
    }

    public String getCurrentPresenter() {
        return currentPresenter;
    }

    public void setCurrentPresenter(String currentPresenter) {
        this.currentPresenter = currentPresenter;
    }
}
