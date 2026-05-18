package com.chess.common.model;

import java.util.Objects;

/**
 * Represents a chess move from one position to another.
 * <p>
 * Includes support for special moves: castling, en passant, and pawn promotion.
 */
public class Move {

    private final Position from;
    private final Position to;
    private final PieceType promotionPiece; // null unless this is a promotion move
    private boolean enPassant;
    private boolean castling;

    /**
     * Creates a basic move.
     *
     * @param from source position
     * @param to   destination position
     */
    public Move(Position from, Position to) {
        this.from = Objects.requireNonNull(from);
        this.to = Objects.requireNonNull(to);
        this.promotionPiece = null;
    }

    /**
     * Creates a move with promotion piece.
     *
     * @param from           source position
     * @param to             destination position
     * @param promotionPiece the piece type to promote to (QUEEN, ROOK, BISHOP, KNIGHT)
     */
    public Move(Position from, Position to, PieceType promotionPiece) {
        this.from = Objects.requireNonNull(from);
        this.to = Objects.requireNonNull(to);
        this.promotionPiece = promotionPiece;
    }

    /**
     * Parses a move from algebraic notation strings.
     * Format: "e2|e4" or "e7|e8|Q" (with promotion)
     *
     * @param fromStr source square in algebraic notation
     * @param toStr   destination square in algebraic notation
     * @return the parsed Move
     */
    public static Move fromAlgebraic(String fromStr, String toStr) {
        Position from = Position.fromAlgebraic(fromStr);
        Position to = Position.fromAlgebraic(toStr);
        return new Move(from, to);
    }

    /**
     * Parses a move from algebraic notation with optional promotion.
     *
     * @param fromStr        source square
     * @param toStr          destination square
     * @param promotionStr   promotion piece type string (e.g., "Q", "R", "B", "N"), or null
     * @return the parsed Move
     */
    public static Move fromAlgebraic(String fromStr, String toStr, String promotionStr) {
        Position from = Position.fromAlgebraic(fromStr);
        Position to = Position.fromAlgebraic(toStr);
        if (promotionStr != null && !promotionStr.isEmpty()) {
            PieceType promo = PieceType.valueOf(promotionStr.toUpperCase());
            return new Move(from, to, promo);
        }
        return new Move(from, to);
    }

    /**
     * Checks if this move is a kingside castling move (O-O).
     */
    public boolean isKingsideCastling() {
        return castling && to.getCol() == 6;
    }

    /**
     * Checks if this move is a queenside castling move (O-O-O).
     */
    public boolean isQueensideCastling() {
        return castling && to.getCol() == 2;
    }

    /**
     * Checks if this is a castling move (either side).
     */
    public boolean isCastling() {
        return castling;
    }

    public void setCastling(boolean castling) {
        this.castling = castling;
    }

    public boolean isEnPassant() {
        return enPassant;
    }

    public void setEnPassant(boolean enPassant) {
        this.enPassant = enPassant;
    }

    public boolean isPromotion() {
        return promotionPiece != null;
    }

    public Position getFrom() {
        return from;
    }

    public Position getTo() {
        return to;
    }

    public PieceType getPromotionPiece() {
        return promotionPiece;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Move move = (Move) o;
        return Objects.equals(from, move.from)
                && Objects.equals(to, move.to)
                && promotionPiece == move.promotionPiece;
    }

    @Override
    public int hashCode() {
        return Objects.hash(from, to, promotionPiece);
    }

    @Override
    public String toString() {
        String s = from.toAlgebraic() + "->" + to.toAlgebraic();
        if (promotionPiece != null) {
            s += "=" + promotionPiece;
        }
        if (castling) {
            s += " (castling)";
        }
        if (enPassant) {
            s += " (en passant)";
        }
        return s;
    }
}
