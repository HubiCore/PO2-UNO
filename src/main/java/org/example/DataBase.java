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
    public void Top5_Best(Connection conn){
        String sql = "SELECT * FROM gracz GROUP BY liczba_wygranych DESC LIMIT 5";
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
            System.out.println("Wypisano top5");
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }


}
