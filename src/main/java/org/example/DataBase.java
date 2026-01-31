package org.example;
import java.sql.*;
public class DataBase {
    public Connection connect(String dbPath) {
        Connection conn = null;
        try {
            String url = "jdbc:sqlite:" + dbPath;
            conn = DriverManager.getConnection(url);
            System.out.println("Connection to SQLite established.");
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return conn;
    }
    public boolean is_player(Connection conn, String name) {
        String sql = "SELECT COUNT(*) FROM gracz WHERE username = ?";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, name);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    int count = rs.getInt(1);
                    return count > 0;
                }
            }
        } catch (SQLException e) {
            System.out.println("Błąd przy sprawdzaniu gracza: " + e.getMessage());
        }

        return false;
    }
    public void Insert_User(Connection conn, String name) {
        if (!is_player(conn, name)) {
            String sql = "INSERT INTO gracz (username, liczba_wygranych) VALUES (?, 0)";

            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, name);
                pstmt.executeUpdate();
                System.out.println("Dodano gracza " + name);
            } catch (SQLException e) {
                System.out.println("Błąd przy dodawaniu gracza: " + e.getMessage());
            }
        } else {
            System.out.println("Gracz " + name + " już istnieje w bazie");
        }
    }

    public void increaseWins(Connection conn, String name) {
        String sql = "UPDATE gracz SET liczba_wygranych = liczba_wygranych + 1 WHERE username = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, name);
            int affectedRows = pstmt.executeUpdate();

            if (affectedRows > 0) {
                System.out.println("Dodano 1 do " + name);
            } else {
                System.out.println(name+ "-> nie ma go w bazie danych (coś jest mocno walnięte");
            }
        } catch (SQLException e) {
            System.out.println("Błąd przy zwiększaniu liczby wygranych: " + e.getMessage());
        }
    }


    public String Top5_Best(Connection conn) {
        StringBuilder result = new StringBuilder();
        String sql = "SELECT username, liczba_wygranych FROM gracz ORDER BY liczba_wygranych DESC LIMIT 5";

        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            int position = 1;
            while (rs.next()) {
                String nazwa = rs.getString("username");
                int wygrane = rs.getInt("liczba_wygranych");
                result.append(position).append(". ").append(nazwa).append(" - ").append(wygrane).append(" wygranych/");
                position++;
            }

            if (position == 1) {
                result.append("Brak danych o graczach.");
            }
        } catch (SQLException e) {
            result.append("Błąd podczas pobierania danych: ").append(e.getMessage());
            e.printStackTrace();
        }
        return result.toString();
    }


}
