package servlet;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class BingoGame implements Serializable {
    private static final long serialVersionUID = 1L;

    private String gameId;
    private List<Integer> drawnNumbers;
    private List<PlayerResult> bingoPlayers;
    private List<PlayerResult> reachPlayers;
    private Date expireTime;
    private Date lastBingoTime;
    private int playerCount = 0;

    // コンストラクタ（渡された4桁の部屋IDをそのままセットする）
    public BingoGame(String gameId, int validDays) {
        this.gameId = gameId;
        this.drawnNumbers = new ArrayList<>();
        this.bingoPlayers = new ArrayList<>();
        this.reachPlayers = new ArrayList<>();
        this.lastBingoTime = null;

        // 有効期限の設定
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, validDays);
        this.expireTime = cal.getTime();
    }

    // 🎲 ランダムに数字（1〜75）を1つ引く処理
    public void drawNumber() {
        if (drawnNumbers.size() >= 75) return;

        List<Integer> pool = new ArrayList<>();
        for (int i = 1; i <= 75; i++) {
            if (!drawnNumbers.contains(i)) {
                pool.add(i);
            }
        }
        Collections.shuffle(pool);
        drawnNumbers.add(pool.get(0));
    }

    // 8日間の期限切れチェック
    public boolean isExpired() {
        return new Date().after(this.expireTime);
    }

    // 最後のビンゴから2時間経過したかのチェック
    public boolean isPast2HoursFromLastBingo() {
        if (lastBingoTime == null) return false;
        long twoHoursInMs = 2L * 60 * 60 * 1000;
        long timePassed = new Date().getTime() - lastBingoTime.getTime();
        return timePassed > twoHoursInMs;
    }

    // 🏆 ビンゴ達成者を記録する（最新が先頭[0番目]に入る仕様）
    public void addBingoPlayer(String name, int number) {
        // 重複チェック
        for (PlayerResult p : bingoPlayers) {
            if (p.getPlayerName().equals(name)) return;
        }
        // 先頭に追加することで最新を上にする
        bingoPlayers.add(0, new PlayerResult(name, number));
        this.lastBingoTime = new Date(); // タイマー基準を更新
    }

    // 🔥 リーチの人を記録する
    public void addReachPlayer(String name) {
        for (PlayerResult p : reachPlayers) {
            if (p.getPlayerName().equals(name)) return;
        }
        reachPlayers.add(new PlayerResult(name, 0));
    }

    public void removeReachPlayer(String name) {
        reachPlayers.removeIf(p -> p.getPlayerName().equals(name));
    }

    // ゲッター・セッター類
    public String getGameId() { return gameId; }
    public List<Integer> getDrawnNumbers() { return drawnNumbers; }
    public List<PlayerResult> getBingoPlayers() { return bingoPlayers; }
    public List<PlayerResult> getReachPlayers() { return reachPlayers; }
    
    public int getPlayerCount() { return playerCount; }
    public void setPlayerCount(int playerCount) { this.playerCount = playerCount; }
    
    // ダミーメソッド（コンパイルエラー防止用：必要に応じて中身を実装してください）
    public int getWaitNumbers(String playerName) { return 1; }
}
