package server;

import java.util.*;

public class GameInfo {
    private String subject;
    private boolean running = false;
    private Map<String, List<String>> wordsBySubject;
    private String currentWord;
    private Random random = new Random();

    public GameInfo(){
        this.subject = "";
        this.running = false;
        initializeWords();
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
        currentWord = words.get(random.nextInt(words.size()));
        System.out.println(currentWord);
        return currentWord;
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
}
