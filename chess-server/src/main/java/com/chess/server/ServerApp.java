package com.chess.server;

import com.chess.server.config.ServerConfig;
import com.chess.server.net.ChessServer;


public class ServerApp {

    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("  ChessKSiS Server - Starting...");
        System.out.println("========================================");

        ServerConfig config;
        if (args.length > 0) {
            config = ServerConfig.fromFile(args[0]);
        } else {
            config = ServerConfig.fromFile("server.properties");
        }

        ChessServer server = new ChessServer(config);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\nShutting down server...");
            server.stop();
        }));

        server.start();
    }
}
