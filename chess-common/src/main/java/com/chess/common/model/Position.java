package com.chess.common.model;

import java.util.Objects;

/**
 * Represents a position on the chess board using 0-based row/col coordinates.
 * <p>
 * Row 0 = rank 8 (top, black's back rank)
 * Row 7 = rank 1 (bottom, white's back rank)
 * Col 0 = file a (left)
 * Col 7 = file h (right)
 */
public class Position {

    private final int row;
    private final int col;

    /**
     * Creates a position from 0-based row and column indices.
     *
     * @param row 0-7 (0 = rank 8)
     * @param col 0-7 (0 = file a)
     */
    public Position(int row, int col) {
        if (row < 0 || row > 7 || col < 0 || col > 7) {
            throw new IllegalArgumentException(
                    "Invalid position: row=" + row + ", col=" + col + " (must be 0-7)");
        }
        this.row = row;
        this.col = col;
    }

    /**
     * Creates a Position from algebraic notation (e.g., "e4").
     *
     * @param algebraic the algebraic notation string (e.g., "e4", "h8")
     * @return the corresponding Position
     * @throws IllegalArgumentException if the notation is invalid
     */
    public static Position fromAlgebraic(String algebraic) {
        if (algebraic == null || algebraic.length() != 2) {
            throw new IllegalArgumentException("Invalid algebraic notation: " + algebraic);
        }
        int col = algebraic.charAt(0) - 'a';
        int rank = algebraic.charAt(1) - '1';
        int row = 7 - rank; // rank 1 = row 7, rank 8 = row 0
        return new Position(row, col);
    }

    /**
     * Converts this position to algebraic notation.
     *
     * @return algebraic notation string (e.g., "e4")
     */
    public String toAlgebraic() {
        char file = (char) ('a' + col);
        char rank = (char) ('1' + (7 - row));
        return String.valueOf(file) + rank;
    }

    /**
     * Checks if this position is within the bounds of the chess board.
     *
     * @return true if valid (0-7 for both row and col)
     */
    public boolean isValid() {
        return row >= 0 && row <= 7 && col >= 0 && col <= 7;
    }

    /**
     * Returns a new position offset by the given deltas.
     * Does NOT validate bounds — check isValid() on the result.
     *
     * @param dRow row delta
     * @param dCol column delta
     * @return new Position offset by deltas
     */
    public Position offset(int dRow, int dCol) {
        return new Position(row + dRow, col + dCol);
    }

    /**
     * Returns a new position offset by the given deltas, without bounds checking.
     * Used internally for move generation where bounds are checked later.
     */
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
