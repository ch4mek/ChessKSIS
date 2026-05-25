package com.chess.common.model;

import java.util.Objects;

public class Position {

    private final int row;
    private final int col;

    public Position(int row, int col) {
        if (row < 0 || row > 7 || col < 0 || col > 7) {
            throw new IllegalArgumentException(
                    "Invalid position: row=" + row + ", col=" + col + " (must be 0-7)");
        }
        this.row = row;
        this.col = col;
    }

    public static Position fromAlgebraic(String algebraic) {
        if (algebraic == null || algebraic.length() != 2) {
            throw new IllegalArgumentException("Invalid algebraic notation: " + algebraic);
        }
        int col = algebraic.charAt(0) - 'a';
        int rank = algebraic.charAt(1) - '1';
        int row = 7 - rank;
        return new Position(row, col);
    }

    public String toAlgebraic() {
        char file = (char) ('a' + col);
        char rank = (char) ('1' + (7 - row));
        return String.valueOf(file) + rank;
    }

    public boolean isValid() {
        return row >= 0 && row <= 7 && col >= 0 && col <= 7;
    }

    public Position offset(int dRow, int dCol) {
        return new Position(row + dRow, col + dCol);
    }

    public Position unsafeOffset(int dRow, int dCol) {
        return new Position(Math.max(0, Math.min(7, row + dRow)),
                Math.max(0, Math.min(7, col + dCol)));
    }

    public int getRow() {
        return row;
    }

    public int getCol() {
        return col;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Position position = (Position) o;
        return row == position.row && col == position.col;
    }

    @Override
    public int hashCode() {
        return Objects.hash(row, col);
    }

    @Override
    public String toString() {
        return toAlgebraic();
    }
}
