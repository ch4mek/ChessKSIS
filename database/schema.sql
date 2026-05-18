-- ============================================================
-- ChessKSiS Database Schema
-- MySQL 8.0+
-- ============================================================

CREATE DATABASE IF NOT EXISTS chess_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE chess_db;

-- ------------------------------------------------------------
-- Users table
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS users (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    username        VARCHAR(50)  NOT NULL UNIQUE,
    password_hash   VARCHAR(64)  NOT NULL COMMENT 'SHA-256 hex digest',
    rating          INT          NOT NULL DEFAULT 1200,
    games_played    INT          NOT NULL DEFAULT 0,
    wins            INT          NOT NULL DEFAULT 0,
    losses          INT          NOT NULL DEFAULT 0,
    draws           INT          NOT NULL DEFAULT 0,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_login      TIMESTAMP    NULL ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_username (username),
    INDEX idx_rating (rating DESC)
) ENGINE=InnoDB;

-- ------------------------------------------------------------
-- Games table
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS games (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    white_player_id INT          NOT NULL,
    black_player_id INT          NOT NULL,
    result          ENUM('white_win', 'black_win', 'draw') NULL COMMENT 'NULL if game is still in progress',
    reason          VARCHAR(50)  NULL COMMENT 'checkmate, resignation, stalemate, timeout, agreement, fifty_move_rule, threefold_repetition, insufficient_material',
    pgn             TEXT         NULL COMMENT 'Game record in PGN format',
    initial_fen     VARCHAR(100) NULL COMMENT 'FEN of starting position',
    started_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    finished_at     TIMESTAMP    NULL,

    FOREIGN KEY (white_player_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (black_player_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_white_player (white_player_id),
    INDEX idx_black_player (black_player_id),
    INDEX idx_started_at (started_at)
) ENGINE=InnoDB;

-- ------------------------------------------------------------
-- Moves table
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS moves (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    game_id         INT          NOT NULL,
    move_number     INT          NOT NULL COMMENT 'Sequential move number (1, 2, 3...)',
    from_square     VARCHAR(2)   NOT NULL COMMENT 'e.g. e2',
    to_square       VARCHAR(2)   NOT NULL COMMENT 'e.g. e4',
    piece_type      VARCHAR(10)  NOT NULL COMMENT 'PAWN, KNIGHT, BISHOP, ROOK, QUEEN, KING',
    is_capture      BOOLEAN      NOT NULL DEFAULT FALSE,
    is_check        BOOLEAN      NOT NULL DEFAULT FALSE,
    notation        VARCHAR(10)  NOT NULL COMMENT 'Algebraic notation: e4, Nf3, O-O, Bxe5',
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (game_id) REFERENCES games(id) ON DELETE CASCADE,
    INDEX idx_game_id (game_id)
) ENGINE=InnoDB;

-- ------------------------------------------------------------
-- Create application database user (optional, run as root)
-- ------------------------------------------------------------
-- CREATE USER IF NOT EXISTS 'chess_server'@'localhost' IDENTIFIED BY 'chess_password';
-- GRANT ALL PRIVILEGES ON chess_db.* TO 'chess_server'@'localhost';
-- FLUSH PRIVILEGES;
