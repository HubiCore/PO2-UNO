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
    public void Insert_User(Connection conn,String name){
        String sql = "INSERT INTO gracz (username, liczba_wygranych) VALUES ("+name + ", 0)";
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
            System.out.println("Dodano gracza "+name);
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }
    public String Top5_Best(Connection conn) {
        StringBuilder result = new StringBuilder();
        // Poprawione: używamy username zamiast nazwa_gracza
        String sql = "SELECT username, liczba_wygranych FROM gracz ORDER BY liczba_wygranych DESC LIMIT 5";

        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            int position = 1;
            while (rs.next()) {
                String nazwa = rs.getString("username");
                int wygrane = rs.getInt("liczba_wygranych");
                result.append(position).append(". ").append(nazwa).append(" - ").append(wygrane).append(" wygranych\n");
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
