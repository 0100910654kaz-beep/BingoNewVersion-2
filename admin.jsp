<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="servlet.BingoGame" %>
<%@ page import="servlet.PlayerResult" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.ArrayList" %>
<%@ page import="java.util.Collections" %>
<%
    BingoGame game = (BingoGame) request.getAttribute("game");
    String gameId = (game != null) ? game.getGameId() : "まだ開始していません";

    List<Integer> reverseDrawnNumbers = new ArrayList<>();
    int ballCount = 0;
    if (game != null) {
        reverseDrawnNumbers.addAll(game.getDrawnNumbers());
        ballCount = reverseDrawnNumbers.size();
        Collections.reverse(reverseDrawnNumbers);
    }
%>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>ビンゴ大会 - 司会者画面</title>
    <style>
        body { font-family: Arial, sans-serif; background-color: #eef2f3; padding: 20px; text-align: center; }
        .admin-container { max-width: 800px; margin: 0 auto; background: white; padding: 30px; border-radius: 12px; box-shadow: 0 4px 15px rgba(0,0,0,0.1); }
        h1 { color: #2b3a42; margin-bottom: 20px; }
        .info-bar { display: flex; justify-content: space-around; background: #f7f9fa; padding: 15px; border-radius: 8px; margin-bottom: 25px; font-size: 18px; font-weight: bold; border: 1px solid #e3e8eb; }
        .btn { display: inline-block; padding: 12px 30px; font-size: 18px; font-weight: bold; color: white; background-color: #007bff; border: none; border-radius: 6px; cursor: pointer; text-decoration: none; transition: background 0.2s; margin: 10px; }
        .btn:hover { background-color: #0056b3; }
        .btn-danger { background-color: #dc3545; }
        .btn-danger:hover { background-color: #bd2130; }
        .grid-container { display: grid; grid-template-columns: 1fr 1fr; gap: 20px; margin-top: 30px; text-align: left; }
        .panel { background: #fafafa; padding: 20px; border-radius: 8px; border: 1px solid #eee; }
        .panel h3 { margin-top: 0; color: #333; border-bottom: 2px solid #007bff; padding-bottom: 8px; }
        .history-grid { display: grid; grid-template-columns: repeat(5, 1fr); gap: 8px; margin-top: 15px; }
        .history-cell { background: #e0e0e0; padding: 10px; text-align: center; border-radius: 4px; font-weight: bold; font-size: 16px; }
        .history-cell.newest { background: #ff6b6b; color: white; animation: pulse 1s infinite alternate; }
        @keyframes pulse { from { transform: scale(1); } to { transform: scale(1.05); } }
        ul { padding-left: 20px; margin: 0; }
        li { margin-bottom: 10px; font-size: 16px; }
    </style>
    <script>
        // 5秒ごとに自動更新して最新の状態を保つ
        setInterval(function() {
            window.location.href = "BingoServlet?userType=admin";
        }, 5000);
    </script>
</head>
<body>

<div class="admin-container">
    <h1>📢 ビンゴ大会 管理者（司会者）パネル</h1>

    <% if (game == null) { %>
        <div class="panel" style="text-align: center; padding: 40px;">
            <p style="font-size: 18px; color: #555;">現在、有効なビンゴゲームがありません。</p>
            <form action="BingoServlet" method="get">
                <input type="hidden" name="action" value="create">
                <label style="font-weight: bold;">部屋の保持期限: </label>
                <select name="validDays" style="padding: 5px; font-size: 16px; margin-right: 10px;">
                    <option value="1">1日間</option>
                    <option value="3">3日間</option>
                    <option value="8" selected>8日間 (標準)</option>
                </select>
                <button type="submit" class="btn">🚀 新しいビンゴゲームを開始する</button>
            </form>
        </div>
    <% } else { %>
        <div class="info-bar">
            <div>🔑 部屋番号 (ゲームID): <span style="color: #007bff;"><%= gameId %></span></div>
            <div>🔮 引いた玉の数: <span style="color: #28a745;"><%= ballCount %> / 75</span></div>
        </div>

        <div style="margin-bottom: 20px;">
            <a href="BingoServlet?action=draw&userType=admin" class="btn">🎲 次の数字を引く</a>
            <a href="BingoServlet?action=reset&userType=admin" class="btn btn-danger" onclick="return confirm('本当にゲームをリセットして全てのデータを消去しますか？');">🔄 ゲームを完全リセット</a>
        </div>

        <div class="grid-container">
            <div class="panel">
                <h3>📊 出た数字の履歴 (最新が赤色)</h3>
                <div class="history-grid">
                    <% for (int i = 0; i < reverseDrawnNumbers.size(); i++) { 
                        int num = reverseDrawnNumbers.get(i);
                        if (i == 0) { %>
                            <div class="history-cell newest"><%= num %></div>
                        <% } else { %>
                            <div class="history-cell"><%= num %></div>
                        <% }
                    } %>
                </div>
            </div>

            <div class="panel">
                <h3>🏆 ビンゴ達成者一覧</h3>
                <ul id="bingoList">
                    <% 
                       List<PlayerResult> bingoList = game.getBingoPlayers();
                       // ★パターン2: リストの末尾（最新の達成者）から0番目（最初の1位）に向かって逆ループ
                       for (int i = bingoList.size() - 1; i >= 0; i--) {
                           PlayerResult p = bingoList.get(i);
                           // インデックスに1を足した数字がそのまま本来の「〇位」になります
                           int currentRank = i + 1;
                    %>
                        <li><strong><%= currentRank %>位</strong>: <%= p.getPlayerName() %> さん <span style="color:#e63946; font-weight:bold;">(🔑<%= p.getDrawnNumberAtBingo() %>番でビンゴ!)</span></li>
                    <% 
                       } 
                       if (bingoList.isEmpty()) { %> <p style="#888;">まだビンゴした人はいません</p> <% } 
                    %>
                </ul>

                <h3 style="margin-top: 25px;">🔥 リーチの人（全自動検知）</h3>
                <ul>
                    <% for (PlayerResult p : game.getReachPlayers()) { %>
                        <li><strong><%= p.getPlayerName() %> さん</strong> <span style="color: #ff9800; font-size: 14px; font-weight: bold;">（あと <%= game.getWaitNumbers(p.getPlayerName()) %> 番でビンゴ！）</span></li>
                    <% } 
                       if (game.getReachPlayers().isEmpty()) { %> <p style="color:#888;">まだリーチの人はいません</p> <% } %>
                </ul>
            </div>
        </div>
    <% } %>
</div>

</body>
</html>
