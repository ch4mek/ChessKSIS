package com.chess.server.service;

import com.chess.server.db.UserDAO;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;


public class AuthService {

    private final UserDAO userDAO;

    public AuthService(UserDAO userDAO) {
        this.userDAO = userDAO;
    }


    public UserDAO.UserRecord register(String username, String password) {
        if (username == null || username.length() < 3 || username.length() > 50) {
            return null;
        }
        if (password == null || password.length() < 4) {
            return null;
        }
        String hash = hashPassword(password);
        boolean created = userDAO.createUser(username, hash);
        if (created) {
            return userDAO.findByUsername(username, hash);
        }
        return null;
    }


    public UserDAO.UserRecord login(String username, String password) {
        String hash = hashPassword(password);
        return userDAO.findByUsername(username, hash);
    }


    private String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}
