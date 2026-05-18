package com.chess.common.model;

import java.util.Objects;

/**
 * Represents a chess piece with type, color, and movement tracking.
 */
public class Piece {

    private final PieceType type;
    private final GameColor color;
    private boolean hasMoved;

    /**
     * Creates a new piece.
     *
     * @param type  the piece type
     * @param color the piece color
     */
    public Piece(PieceType type, GameColor color) {
        this.type = Objects.requireNonNull(type);
        this.color = Objects.requireNonNull(color);
        this.hasMoved = false;
    }

    /**
     * Creates a new piece with explicit hasMoved state.
     *
     * @param type     the piece type
     * @param color    the piece color
     * @param hasMoved whether the piece has moved
     */
    public Piece(PieceType type, GameColor color, boolean hasMoved) {
        this.type = Objects.requireNonNull(type);
        this.color = Objects.requireNonNull(color);
        this.hasMoved = hasMoved;
    }

    /**
     * Creates a piece from a FEN character.
     *
     * @param fenChar the FEN character (e.g., 'P' for white pawn, 'p' for black pawn)
     * @return the corresponding Piece
     */
    public static Piece fromFenChar(char fenChar) {
        GameColor color = PieceType.colorFromFenChar(fenChar);
        PieceType type = PieceType.fromFenChar(fenChar);
        if (type == null) {
            throw new IllegalArgumentException("Invalid FEN piece character: " + fenChar);
        }
        return new Piece(type, color);
    }

    /**
     * Returns the FEN character for this piece.
     *
     * @return uppercase for white, lowercase for black
     */
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

    /**
     * Creates a copy of this piece.
     *
     * @return a new Piece with the same state
     */
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
