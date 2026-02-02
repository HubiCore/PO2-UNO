package org.example;

import java.sql.*;

/**
 * Klasa DataBase zarządza połączeniem z bazą danych SQLite oraz operacjami na tabeli graczy.
 * Zawiera metody do łączenia się z bazą, tworzenia tabel, dodawania graczy, weryfikacji istnienia,
 * pobierania hashów haseł, zwiększania liczby wygranych oraz pobierania rankingów.
 */
public class DataBase {

    /**
     * Nawiązuje połączenie z bazą danych SQLite o podanej ścieżce.
     * W przypadku powodzenia, automatycznie tworzy tabelę graczy (jeśli nie istnieje).
     *
     * @param dbPath ścieżka do pliku bazy danych SQLite
     * @return obiekt Connection reprezentujący połączenie z bazą danych lub null w przypadku błędu
     */

    private final Logger logger = Logger.getInstance();
    public Connection connect(String dbPath) {
        Connection conn = null;
        try {
            String url = "jdbc:sqlite:" + dbPath;
            conn = DriverManager.getConnection(url);
            logger.info("Connection to SQLite established: " + dbPath);

            createTableWithPassword(conn);
        } catch (SQLException e) {
            logger.error(e, "Database connection error");
        }
        return conn;
    }

    /**
     * Prywatna metoda tworząca tabelę gracz w bazie danych, jeśli nie istnieje.
     * Tabela zawiera kolumny: id (klucz główny), username (unikalny), password_hash, liczba_wygranych.
     *
     * @param conn aktywny obiekt Connection do bazy danych
     * @throws SQLException jeśli wystąpi błąd podczas wykonywania zapytania SQL
     */
    private void createTableWithPassword(Connection conn) throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS gracz (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "username TEXT UNIQUE NOT NULL," +
                "password_hash TEXT NOT NULL," +
                "liczba_wygranych INTEGER DEFAULT 0)";
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
            logger.info("Table 'gracz' created/verified");
        }
    }

    /**
     * Sprawdza, czy gracz o podanej nazwie istnieje w bazie danych.
     *
     * @param conn aktywny obiekt Connection do bazy danych
     * @param name nazwa gracza do sprawdzenia
     * @return true jeśli gracz istnieje, false w przeciwnym przypadku lub w przypadku błędu
     */
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
            logger.error(e, "Error checking player: " + name);
        }

        return false;
    }

    /**
     * Pobiera hash hasła dla podanego użytkownika z bazy danych.
     *
     * @param conn aktywny obiekt Connection do bazy danych
     * @param username nazwa użytkownika, dla którego pobierany jest hash
     * @return hash hasła jako String lub null jeśli użytkownik nie istnieje lub wystąpił błąd
     */
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
            logger.error(e, "Error retrieving password for: " + username);
        }

        return null;
    }

    /**
     * Atomowo tworzy nowego użytkownika, jeśli nie istnieje w bazie danych.
     * Wykorzystuje klauzulę INSERT OR IGNORE, aby uniknąć konfliktów duplikatów.
     *
     * @param conn aktywny obiekt Connection do bazy danych
     * @param name nazwa nowego użytkownika
     * @param passwordHash hash hasła nowego użytkownika
     * @return true jeśli użytkownik został utworzony, false jeśli już istniał lub wystąpił błąd
     */
    public boolean createUserIfNotExists(Connection conn, String name, String passwordHash) {
        String sql = "INSERT OR IGNORE INTO gracz (username, password_hash, liczba_wygranych) VALUES (?, ?, 0)";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, name);
            pstmt.setString(2, passwordHash);
            int affectedRows = pstmt.executeUpdate();

            if (affectedRows > 0) {
                logger.info("Created new user: " + name);
                return true;
            } else {
                logger.info("User already exists: " + name);
                return false;
            }

        } catch (SQLException e) {
            logger.error(e, "Error adding player: " + name);
            return false;
        }
    }

    /**
     * Dodaje nowego gracza do bazy danych bez hasła (dla zachowania kompatybilności).
     * Wywołuje przeciążoną metodę Insert_User z pustym hasłem.
     *
     * @param conn aktywny obiekt Connection do bazy danych
     * @param name nazwa nowego gracza
     */
    public void Insert_User(Connection conn, String name) {
        Insert_User(conn, name, "");
    }

    /**
     * Dodaje nowego gracza do bazy danych z podanym hashem hasła.
     * Metoda najpierw sprawdza, czy gracz już istnieje (używając is_player).
     *
     * @param conn aktywny obiekt Connection do bazy danych
     * @param name nazwa nowego gracza
     * @param passwordHash hash hasła nowego gracza
     */
    public void Insert_User(Connection conn, String name, String passwordHash) {
        if (!is_player(conn, name)) {
            String sql = "INSERT INTO gracz (username, password_hash, liczba_wygranych) VALUES (?, ?, 0)";

            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, name);
                pstmt.setString(2, passwordHash);
                pstmt.executeUpdate();
                logger.info("Added player " + name + " with password");
            } catch (SQLException e) {
                logger.error(e, "Error adding player: " + name);
            }
        } else {
            logger.info("Player " + name + " already exists in database");
        }
    }

    /**
     * Zwiększa liczbę wygranych dla podanego gracza o 1.
     *
     * @param conn aktywny obiekt Connection do bazy danych
     * @param name nazwa gracza, dla którego zwiększana jest liczba wygranych
     */
    public void increaseWins(Connection conn, String name) {
        String sql = "UPDATE gracz SET liczba_wygranych = liczba_wygranych + 1 WHERE username = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, name);
            int affectedRows = pstmt.executeUpdate();

            if (affectedRows > 0) {
                logger.info("Added 1 win to " + name);
            } else {
                logger.warning(name + " not found in database when increasing wins");
            }
        } catch (SQLException e) {
            logger.error(e, "Error increasing wins for: " + name);
        }
    }

    /**
     * Pobiera ranking 5 najlepszych graczy na podstawie liczby wygranych.
     *
     * @param conn aktywny obiekt Connection do bazy danych
     * @return String z listą 5 najlepszych graczy w formacie: "1. nazwa1 - liczba1 wygranych/2. nazwa2 - liczba2 wygranych/..."
     *         lub komunikat "Brak danych o graczach." jeśli tabela jest pusta
     */
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
            logger.error(e, "Error retrieving top 5 players");
            result.append("Błąd podczas pobierania danych: ").append(e.getMessage());
        }
        return result.toString();
    }
}