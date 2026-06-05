package servlet;

import java.io.IOException;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet("/BingoServlet")
public class BingoServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String action = request.getParameter("action");
        String userType = request.getParameter("userType");
        ServletContext application = getServletContext();
        BingoGame game = (BingoGame) application.getAttribute("game");

        // 1. 部屋の新規作成（4桁のランダム数字を自動生成）
        if ("create".equals(action)) {
            // 1000 〜 9999 の範囲でランダムな4桁の数字を生成
            int random4Digit = (int)(Math.random() * 9000) + 1000;
            String gameId = String.valueOf(random4Digit);

            int validDays = 8;
            String validDaysStr = request.getParameter("validDays");
            if (validDaysStr != null && !validDaysStr.isEmpty()) {
                try {
                    validDays = Integer.parseInt(validDaysStr);
                } catch (NumberFormatException e) {
                    validDays = 8;
                }
            }

            game = new BingoGame(gameId, validDays);
            application.setAttribute("game", game);

            request.setAttribute("game", game);
            request.getRequestDispatcher("/admin.jsp").forward(request, response);
            return;
        }

        // 2. リセット処理
        if ("reset".equals(action)) {
            application.removeAttribute("game");
            response.sendRedirect("admin.jsp");
            return;
        }

        // 3. タイマー・期限切れチェック
        if (game != null) {
            if (game.isExpired() || game.isPast2HoursFromLastBingo()) {
                application.removeAttribute("game"); // 期限切れならメモリから完全削除
                game = null;
            }
        }

        // 4. 数字を引くアクション（司会者）
        if ("draw".equals(action) && game != null) {
            game.drawNumber();
            request.setAttribute("game", game);
            request.getRequestDispatcher("/admin.jsp").forward(request, response);
            return;
        }

        // 5. 通常アクセス時の画面振り分け
        if (game == null) {
            if ("admin".equals(userType)) {
                request.getRequestDispatcher("/admin.jsp").forward(request, response);
            } else {
                request.getRequestDispatcher("/index.jsp").forward(request, response);
            }
            return;
        }

        request.setAttribute("game", game);
        if ("admin".equals(userType)) {
            request.getRequestDispatcher("/admin.jsp").forward(request, response);
        } else {
            request.getRequestDispatcher("/player.jsp").forward(request, response);
        }
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doGet(request, response);
    }
}
