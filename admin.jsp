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
        .info-box { font-size: 18px; margin-bottom: 20px; padding: 15px; background: #f0f7f4; border-radius: 8px; display: inline-block; }
        .btn { display: inline-block; padding: 12px 24px; font-size: 18px; color: white; background-color: #007bff; border: none; border-radius: 6px; cursor: pointer; text-decoration: none; font-weight: bold; box-shadow: 0 4px 6px rgba(0,0,0,0.1); }
        .btn:hover { background-color: #0056b3; }
        .btn-danger { background-color: #dc3545; }
        .btn-danger:hover { background-color: #bd2130; }
        .flex-container { display: flex; justify-content: space-between; margin-top: 30px; gap: 20px; text-align: left; }
        .panel { flex: 1; background: #f8f9fa; padding: 20px; border-radius: 8px; border: 1px solid #dee2e6; }
        h3 { border-bottom: 2px solid #2b3a42; padding-bottom: 8px; color: #2b3a42; margin-top: 0; }
        ul { list-style: none; padding: 0; }
        li { padding: 10px; background: white; margin-bottom: 8px; border-radius: 4px; box-shadow: 0 2px 4px rgba(0,0,0,0.05); font-size: 16px; }
        .history-grid { display: flex; flex-wrap: wrap; gap: 8px; justify-content: flex-start; margin-top: 10px; }
        .history-cell { width: 40px; height: 40px; display: flex; align-items: center; justify-content: center; background: #e9ecef; border-radius: 5px; font-weight: bold; font-size: 16px; color: #495057; }
        .history-cell.newest { background: #ff6b6b; color: white; animation: pulse 1s infinite alternate; }
        @keyframes pulse { from { transform: scale(1); } to { transform: scale(1.05); } }
    </style>
</head>
<body>

<div class="admin-container">
    <h1>👑 ビンゴ大会 司会者コントロールパネル</h1>

    <% if (game == null) { %>
        <div class="info-box" style="background: #f8d7da; color: #721c24;">
            ⚠️ ゲームデータが存在しません。新しく部屋を作成してください。
        </div>
        <br>
        <a href="index.jsp" class="btn">トップ画面へ戻る</a>
    <% } else { %>
        <div class="info-box">
            📢 <strong>部屋番号 (ゲームID):</strong> <span style="color: #007bff; font-weight: bold;"><%= gameId %></span>
        </div>

        <div style="margin-bottom: 25px; display: flex; justify-content: center; gap: 15px;">
            <form action="BingoServlet" method="get">
                <input type="hidden" name="action" value="draw">
                <input type="hidden" name="userType" value="admin">
                <button type="submit" class="btn">🎲 次の数字を引く</button>
            </form>

            <form action="BingoServlet" method="get" onsubmit="return confirm('本当にこの部屋のゲームデータを完全にリセットしますか？');">
                <input type="hidden" name="action" value="reset">
                <input type="hidden" name="userType" value="admin">
                <button type="submit" class="btn btn-danger">🔄 一発ゲームリセット</button>
            </form>
        </div>

        <div class="flex-container">
            <div class="panel" style="flex: 1.2;">
                <h3>📊 出た数字の履歴</h3>
                <div style="font-size: 14px; color: #6c757d; margin-bottom: 5px;">
                    現在の玉数: <strong><%= ballCount %></strong> / 75 球
                </div>
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
                       List<PlayerResult> players = game.getBingoPlayers();
                       
                       // 同着（スキップ方式）のリアルタイムランキング順位計算ロジック
                       int currentRank = 1;
                       for (int i = 0; i < players.size(); i++) {
                           PlayerResult p = players.get(i);
                           
                           // 2人目以降の場合、前の人とビンゴした当選番号を比較する
                           if (i > 0) {
                               PlayerResult prev = players.get(i - 1);
                               if (p.getDrawnNumberAtBingo() != prev.getDrawnNumberAtBingo()) {
                                   currentRank = i + 1; // 当選番号が違っていれば順位をスキップ
                               }
                           }
                    %>
                        <li>
                            <strong><%= currentRank %>位</strong>: <%= p.getPlayerName() %> さん 
                            <span style="color:#e63946; font-weight:bold;">(🔑<%= p.getDrawnNumberAtBingo() %>番でビンゴ!)</span>
                        </li>
                    <% 
                       } 
                       if (players.isEmpty()) { 
                    %> 
                        <p style="color:#888;">まだビンゴした人はいません</p> 
                    <% } %>
                </ul>

                <h3 style="margin-top: 25px;">🔥 リーチの人（全自動検知）</h3>
                <ul>
                    <% for (PlayerResult p : game.getReachPlayers()) { %>
                        <li><strong><%= p.getPlayerName() %> さん</strong></li>
                    <% } 
                       if (game.getReachPlayers().isEmpty()) { %> <p style="color:#888;">まだリーチの人はいません</p> <% } %>
                </ul>
            </div>
        </div>
    <% } %>
</div>

</body>
</html>
