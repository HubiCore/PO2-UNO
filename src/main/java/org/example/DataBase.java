package org.example;

import java.sql.*;

public class DataBase {
    public Connection connect(String dbPath) {
        Connection conn = null;
        try {
            String url = "jdbc:sqlite:" + dbPath;
            conn = DriverManager.getConnection(url);
            System.out.println("Connection to SQLite established.");

            // Tworzenie tabeli z hasłem jeśli nie istnieje
            createTableWithPassword(conn);
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return conn;
    }

    private void createTableWithPassword(Connection conn) throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS gracz (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "username TEXT UNIQUE NOT NULL," +
                "password_hash TEXT NOT NULL," +
                "liczba_wygranych INTEGER DEFAULT 0)";
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
            System.out.println("Tabela gracz została utworzona/zweryfikowana");
        }
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

    // Metoda do pobierania hasha hasła
    public String getPasswordHash(Connection conn, String username) {
        String sql = "SELECT password_hash FROM gracz WHERE username = ?";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("password_hash");
                }
            }
        } catch (SQLException e) {
            System.out.println("Błąd przy pobieraniu hasła: " + e.getMessage());
        }

        return null;
    }

    // Atomiczne tworzenie użytkownika (zwraca true jeśli udało się utworzyć, false jeśli już istnieje)
    public boolean createUserIfNotExists(Connection conn, String name, String passwordHash) {
        String sql = "INSERT OR IGNORE INTO gracz (username, password_hash, liczba_wygranych) VALUES (?, ?, 0)";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, name);
            pstmt.setString(2, passwordHash);
            int affectedRows = pstmt.executeUpdate();

            if (affectedRows > 0) {
                System.out.println("Utworzono nowego użytkownika: " + name);
                return true;
            } else {
                System.out.println("Użytkownik " + name + " już istnieje");
                return false;
            }

        } catch (SQLException e) {
            System.out.println("Błąd przy dodawaniu gracza: " + e.getMessage());
            return false;
        }
    }

    // Metoda do tworzenia użytkownika (bez sprawdzania czy istnieje - dla kompatybilności)
    public void Insert_User(Connection conn, String name) {
        Insert_User(conn, name, ""); // Domyślne puste hasło
    }

    // Metoda do tworzenia użytkownika z hasłem
    public void Insert_User(Connection conn, String name, String passwordHash) {
        if (!is_player(conn, name)) {
            String sql = "INSERT INTO gracz (username, password_hash, liczba_wygranych) VALUES (?, ?, 0)";

            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, name);
                pstmt.setString(2, passwordHash);
                pstmt.executeUpdate();
                System.out.println("Dodano gracza " + name + " z hasłem");
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
                System.out.println(name+ "-> nie ma go w bazie danych");
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