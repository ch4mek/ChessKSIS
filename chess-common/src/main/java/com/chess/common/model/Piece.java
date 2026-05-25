package com.chess.common.model;

import java.util.Objects;

public class Piece {

    private final PieceType type;
    private final GameColor color;
    private boolean hasMoved;

    public Piece(PieceType type, GameColor color) {
        this.type = Objects.requireNonNull(type);
        this.color = Objects.requireNonNull(color);
        this.hasMoved = false;
    }

    public Piece(PieceType type, GameColor color, boolean hasMoved) {
        this.type = Objects.requireNonNull(type);
        this.color = Objects.requireNonNull(color);
        this.hasMoved = hasMoved;
    }

    public static Piece fromFenChar(char fenChar) {
        GameColor color = PieceType.colorFromFenChar(fenChar);
        PieceType type = PieceType.fromFenChar(fenChar);
        if (type == null) {
            throw new IllegalArgumentException("Invalid FEN piece character: " + fenChar);
        }
        return new Piece(type, color);
    }

    public char toFenChar() {
        return type.toFenChar(color);
    }

    public PieceType getType() {
        return type;
    }

    public GameColor getColor() {
        return color;
    }

    public boolean hasMoved() {
        return hasMoved;
    }

    public void setHasMoved(boolean hasMoved) {
        this.hasMoved = hasMoved;
    }

    public Piece copy() {
        return new Piece(type, color, hasMoved);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Piece piece = (Piece) o;
        return hasMoved == piece.hasMoved && type == piece.type && color == piece.color;
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, color, hasMoved);
    }

    @Override
    public String toString() {
        return color + " " + type + (hasMoved ? " (moved)" : "");
    }
}
