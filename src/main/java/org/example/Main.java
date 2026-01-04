package org.example;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {
    static void main() {
        UnoServer server = new UnoServer();
        server.start();
    }
}
