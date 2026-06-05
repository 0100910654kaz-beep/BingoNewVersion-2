package servlet;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ConcurrentHashMap;

public class BingoGame implements Serializable {
    private static final long serialVersionUID = 1L;

    private String gameId;                             // 部屋番号（ゲームID）
    private List<Integer> drawnNumbers;                // 当選番号の履歴
    private List<PlayerResult> bingoPlayers;           // ビンゴ達成者のリスト（★古い順に保持します）
    private List<PlayerResult> reachPlayers;           // リーチ達成者のリスト
    private List<String> allPlayers;                   // 全参加者の名前リスト
    private Date expireTime;                           // この部屋の有効期限
    private Date lastBingoTime;                        // 最後にビンゴが出た時刻
    private int anonymousCount = 0;                    // 名前空欄の人用のカウンター

    // 各プレイヤーのカードデータをサーバー側でも管理・自動スキャンするための箱
    private ConcurrentHashMap<String, List<List<String>>> playerCards = new ConcurrentHashMap<>();
    // 各プレイヤーの「待ち数字（ビンゴする番号）」を記憶する箱
    private ConcurrentHashMap<String, List<String>> playerWaitNumbers = new ConcurrentHashMap<>();

    public BingoGame(String gameId, int validDays) {
        this.gameId = gameId;
        this.drawnNumbers = new CopyOnWriteArrayList<>();
        this.bingoPlayers = new CopyOnWriteArrayList<>();
        this.reachPlayers = new CopyOnWriteArrayList<>();
        this.allPlayers = new CopyOnWriteArrayList<>();
        
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, validDays);
        this.expireTime = cal.getTime();
        this.lastBingoTime = new Date();
    }

    // 司会者が数字を引く処理
    public int drawNumber() {
        if (drawnNumbers.size() >= 75) return -1;
        List<Integer> pool = new ArrayList<>();
        for (int i = 1; i <= 75; i++) {
            if (!drawnNumbers.contains(i)) pool.add(i);
        }
        int index = (int) (Math.random() * pool.size());
        int num = pool.get(index);
        drawnNumbers.add(num);
        
        // 新しい数字が出たので、全員のカードを自動再スキャンしてリーチ・ビンゴを自動判定
        scanAllCards(num);
        
        return num;
    }

    // プレイヤーが参加した時にカードを登録する処理
    public void setPlayerCard(String name, List<List<String>> card) {
        if (name == null || name.trim().isEmpty()) return;
        playerCards.put(name, card);
        if (!allPlayers.contains(name)) {
            allPlayers.add(name);
        }
        // 登録時に、今まで出たすべての数字を使って一発スキャンする
        List<String> waitNums = scanSingleCard(card);
        playerWaitNumbers.put(name, waitNums);
        
        if (waitNums.isEmpty()) {
            addBingoPlayer(name, drawnNumbers.isEmpty() ? 0 : drawnNumbers.get(drawnNumbers.size() - 1));
        } else if (waitNums.size() == 1) {
            addReachPlayer(name);
        }
    }

    // 全員のカードを自動スキャンする内部処理
    private void scanAllCards(int currentDrawnNumber) {
        for (String name : playerCards.keySet()) {
            if (allPlayers.contains(name)) {
                // すでにビンゴ済みの人はスキャンをスキップ
                boolean alreadyBingo = false;
                for (PlayerResult p : bingoPlayers) {
                    if (p.getPlayerName().equals(name)) {
                        alreadyBingo = true;
                        break;
                    }
                }
                if (alreadyBingo) continue;

                List<List<String>> card = playerCards.get(name);
                List<String> waitNums = scanSingleCard(card);
                playerWaitNumbers.put(name, waitNums);

                if (waitNums.isEmpty()) {
                    addBingoPlayer(name, currentDrawnNumber);
                } else if (waitNums.size() == 1) {
                    addReachPlayer(name);
                } else {
                    removeReachPlayer(name);
                }
            }
        }
    }

    // カード1枚のリーチ・ビンゴ判定ロジック
    private List<String> scanSingleCard(List<List<String>> card) {
        List<String> waitNumbers = new ArrayList<>();
        boolean[][] marked = new boolean[5][5];

        for (int r = 0; r < 5; r++) {
            for (int c = 0; c < 5; c++) {
                String val = card.get(r).get(c);
                if (val.equals("0") || val.equalsIgnoreCase("FREE")) {
                    marked[r][c] = true;
                } else {
                    int num = Integer.parseInt(val);
                    if (drawnNumbers.contains(num)) {
                        marked[r][c] = true;
                    }
                }
            }
        }

        // 横5行のチェック
        for (int r = 0; r < 5; r++) {
            int missingCount = 0;
            String missingVal = "";
            for (int c = 0; c < 5; c++) {
                if (!marked[r][c]) { missingCount++; missingVal = card.get(r).get(c); }
            }
            if (missingCount == 1 && !waitNumbers.contains(missingVal)) waitNumbers.add(missingVal);
        }

        // 縦5列のチェック
        for (int c = 0; c < 5; c++) {
            int missingCount = 0;
            String missingVal = "";
            for (int r = 0; r < 5; r++) {
                if (!marked[r][c]) { missingCount++; missingVal = card.get(r).get(c); }
            }
            if (missingCount == 1 && !waitNumbers.contains(missingVal)) waitNumbers.add(missingVal);
        }

        // 斜め（左上から右下）
        int missingCountD1 = 0;
        String missingValD1 = "";
        for (int i = 0; i < 5; i++) {
            if (!marked[i][i]) { missingCountD1++; missingValD1 = card.get(i).get(i); }
        }
        if (missingCountD1 == 1 && !waitNumbers.contains(missingValD1)) waitNumbers.add(missingValD1);

        // 斜め（右上から左下）
        int realD2Count = 0;
        String realD2Val = "";
        for (int i = 0; i < 5; i++) {
            if (!marked[i][4 - i]) { realD2Count++; realD2Val = card.get(i).get(4 - i); }
        }
        if (realD2Count == 1 && !waitNumbers.contains(realD2Val)) waitNumbers.add(realD2Val);

        // 完全ビンゴのチェック
        boolean hasBingoLine = false;
        for(int r=0; r<5; r++) { if(marked[r][0] && marked[r][1] && marked[r][2] && marked[r][3] && marked[r][4]) hasBingoLine = true; }
        for(int c=0; c<5; c++) { if(marked[0][c] && marked[1][c] && marked[2][c] && marked[3][c] && marked[4][c]) hasBingoLine = true; }
        if(marked[0][0] && marked[1][1] && marked[2][2] && marked[3][3] && marked[4][4]) hasBingoLine = true;
        if(marked[0][4] && marked[1][3] && marked[2][2] && marked[3][1] && marked[4][0]) hasBingoLine = true;

        if (hasBingoLine) {
            return new ArrayList<>();
        }
        return waitNumbers;
    }

    // ビンゴ達成者の登録処理（一番後ろに追加して古い順にします）
    private void addBingoPlayer(String name, int currentDrawnNumber) {
        for (PlayerResult p : bingoPlayers) {
            if (p.getPlayerName().equals(name)) return;
        }
        Date now = new Date();
        bingoPlayers.add(new PlayerResult(name, now, currentDrawnNumber));
        this.lastBingoTime = now;
        removeReachPlayer(name);
    }

    // リーチ登録
    private void addReachPlayer(String name) {
        for (PlayerResult p : reachPlayers) {
            if (p.getPlayerName().equals(name)) return;
        }
        reachPlayers.add(0, new PlayerResult(name, new Date(), 0));
    }

    // リーチ解除
    public void removeReachPlayer(String name) {
        reachPlayers.removeIf(p -> p.getPlayerName().equals(name));
    }

    // リーチの人の「待ち数字」を司会者画面に渡す部品
    public List<String> getWaitNumbers(String name) {
        return playerWaitNumbers.getOrDefault(name, new ArrayList<>());
    }

    public boolean isExpired() { return new Date().after(this.expireTime); }
    public boolean isPast2HoursFromLastBingo() {
        if (bingoPlayers.isEmpty()) return false;
        long twoHoursInMilliseconds = 2L * 60 * 60 * 1000;
        long timePassed = new Date().getTime() - lastBingoTime.getTime();
        return timePassed > twoHoursInMilliseconds;
    }

    public String getGameId() { return gameId; }
    public List<Integer> getDrawnNumbers() { return drawnNumbers; }
    public List<PlayerResult> getBingoPlayers() { return bingoPlayers; }
    public List<PlayerResult> getReachPlayers() { return reachPlayers; }
    public synchronized String getNextAnonymousName() {
        anonymousCount++;
        return "ゲスト" + anonymousCount;
    }
}
