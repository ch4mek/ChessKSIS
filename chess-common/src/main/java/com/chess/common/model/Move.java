package com.chess.common.model;

import java.util.Objects;

public class Move {

    private final Position from;
    private final Position to;
    private final PieceType promotionPiece;
    private boolean enPassant;
    private boolean castling;

    public Move(Position from, Position to) {
        this.from = Objects.requireNonNull(from);
        this.to = Objects.requireNonNull(to);
        this.promotionPiece = null;
    }

    public Move(Position from, Position to, PieceType promotionPiece) {
        this.from = Objects.requireNonNull(from);
        this.to = Objects.requireNonNull(to);
        this.promotionPiece = promotionPiece;
    }

    public static Move fromAlgebraic(String fromStr, String toStr) {
        Position from = Position.fromAlgebraic(fromStr);
        Position to = Position.fromAlgebraic(toStr);
        return new Move(from, to);
    }

    public static Move fromAlgebraic(String fromStr, String toStr, String promotionStr) {
        Position from = Position.fromAlgebraic(fromStr);
        Position to = Position.fromAlgebraic(toStr);
        if (promotionStr != null && !promotionStr.isEmpty()) {
            PieceType promo = PieceType.valueOf(promotionStr.toUpperCase());
            return new Move(from, to, promo);
        }
        return new Move(from, to);
    }

    public boolean isKingsideCastling() {
        return castling && to.getCol() == 6;
    }

    public boolean isQueensideCastling() {
        return castling && to.getCol() == 2;
    }

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
