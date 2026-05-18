package com.chess.client.gui;

import com.chess.common.game.Board;
import com.chess.common.model.*;
import com.chess.common.model.Piece;

import javafx.event.EventHandler;
import javafx.geometry.HPos;
import javafx.geometry.VPos;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Custom JavaFX widget that renders an interactive chess board.
 * Supports piece selection, move highlighting, and board flipping.
 */
public class ChessBoardWidget extends GridPane {

    private static final Logger LOGGER = Logger.getLogger(ChessBoardWidget.class.getName());

    private static final int BOARD_SIZE = 8;
    private static final double SQUARE_SIZE = 70;

    // Colors
    private static final Color LIGHT_SQUARE = Color.rgb(240, 217, 181);
    private static final Color DARK_SQUARE = Color.rgb(181, 136, 99);
    private static final Color SELECTED_COLOR = Color.rgb(130, 151, 105, 0.7);
    private static final Color LEGAL_MOVE_COLOR = Color.rgb(130, 151, 105, 0.5);
    private static final Color LAST_MOVE_COLOR = Color.rgb(205, 210, 106, 0.5);

    private final Rectangle[][] squares = new Rectangle[BOARD_SIZE][BOARD_SIZE];
    private final StackPane[][] cells = new StackPane[BOARD_SIZE][BOARD_SIZE];

    private Board board;
    private boolean flipped = false; // If true, black is at the bottom
    private Position selectedPosition = null;
    private List<Position> legalMovePositions = new ArrayList<>();
    private Position lastMoveFrom = null;
    private Position lastMoveTo = null;

    private MoveCallback moveCallback;

    /**
     * Callback interface for when the user makes a move on the board.
     */
    public interface MoveCallback {
        /**
         * Called when a move is attempted.
         *
         * @param from source position
         * @param to   destination position
         */
        void onMove(Position from, Position to);
    }

    public ChessBoardWidget() {
        setHgap(0);
        setVgap(0);
        setPadding(javafx.geometry.Insets.EMPTY);
        buildBoard();
    }

    /**
     * Builds the 8x8 grid of squares.
     */
    private void buildBoard() {
        getChildren().clear();

        for (int displayRow = 0; displayRow < BOARD_SIZE; displayRow++) {
            for (int displayCol = 0; displayCol < BOARD_SIZE; displayCol++) {
                int boardRow = flipped ? (7 - displayRow) : displayRow;
                int boardCol = flipped ? (7 - displayCol) : displayCol;

                boolean isLight = (boardRow + boardCol) % 2 == 0;
                Color color = isLight ? LIGHT_SQUARE : DARK_SQUARE;

                Rectangle square = new Rectangle(SQUARE_SIZE, SQUARE_SIZE, color);
                squares[displayRow][displayCol] = square;

                StackPane cell = new StackPane(square);
                cells[displayRow][displayCol] = cell;

                // Click handler
                final int finalBoardRow = boardRow;
                final int finalBoardCol = boardCol;
                cell.setOnMouseClicked(new EventHandler<MouseEvent>() {
                    @Override
                    public void handle(MouseEvent event) {
                        onSquareClicked(finalBoardRow, finalBoardCol);
                    }
                });

                add(cell, displayCol, displayRow);
            }
        }

        // Add rank/file labels
        addRankFileLabels();
    }

    /**
     * Adds rank (1-8) and file (a-h) labels around the board.
     */
    private void addRankFileLabels() {
        // Column constraints and row constraints for labels
        // For simplicity, we add labels as overlays or skip them
        // Labels can be added as text nodes in the grid margins
    }

    /**
     * Sets the board state and redraws.
     *
     * @param board the current board state
     */
    public void setBoard(Board board) {
        this.board = board;
        drawPieces();
        updateHighlights();
    }

    /**
     * Draws all pieces on the board using Unicode chess symbols.
     */
    private void drawPieces() {
        if (board == null) return;

        for (int displayRow = 0; displayRow < BOARD_SIZE; displayRow++) {
            for (int displayCol = 0; displayCol < BOARD_SIZE; displayCol++) {
                int boardRow = flipped ? (7 - displayRow) : displayRow;
                int boardCol = flipped ? (7 - displayCol) : displayCol;

                StackPane cell = cells[displayRow][displayCol];
                // Remove any existing piece (keep the Rectangle background)
                cell.getChildren().removeIf(node -> node instanceof javafx.scene.text.Text);

                Piece piece = board.getPiece(boardRow, boardCol);
                if (piece != null) {
                    javafx.scene.text.Text pieceText = new javafx.scene.text.Text(getUnicodePiece(piece));
                    pieceText.setStyle("-fx-font-size: 48px; -fx-font-family: 'Segoe UI Symbol', Arial;");
                    if (piece.getColor() == GameColor.WHITE) {
                        pieceText.setFill(javafx.scene.paint.Color.WHITE);
                        pieceText.setStroke(javafx.scene.paint.Color.BLACK);
                        pieceText.setStrokeWidth(0.5);
                    } else {
                        pieceText.setFill(javafx.scene.paint.Color.BLACK);
                    }
                    cell.getChildren().add(pieceText);
                }
            }
        }
    }

    /**
     * Returns the Unicode chess piece symbol for the given piece.
     */
    private String getUnicodePiece(Piece piece) {
        if (piece.getColor() == GameColor.WHITE) {
            switch (piece.getType()) {
                case KING:   return "\u2654";
                case QUEEN:  return "\u2655";
                case ROOK:   return "\u2656";
                case BISHOP: return "\u2657";
                case KNIGHT: return "\u2658";
                case PAWN:   return "\u2659";
            }
        } else {
            switch (piece.getType()) {
                case KING:   return "\u265A";
                case QUEEN:  return "\u265B";
                case ROOK:   return "\u265C";
                case BISHOP: return "\u265D";
                case KNIGHT: return "\u265E";
                case PAWN:   return "\u265F";
            }
        }
        return "?";
    }

    /**
     * Handles a click on a board square.
     */
    private void onSquareClicked(int boardRow, int boardCol) {
        if (board == null) return;

        Position clickedPos = new Position(boardRow, boardCol);

        if (selectedPosition != null) {
            // A piece is already selected
            if (clickedPos.equals(selectedPosition)) {
                // Deselect
                selectedPosition = null;
                legalMovePositions.clear();
                updateHighlights();
                return;
            }

            // Check if this is a legal move target
            if (legalMovePositions.contains(clickedPos)) {
                // Make the move
                if (moveCallback != null) {
                    moveCallback.onMove(selectedPosition, clickedPos);
                }
                selectedPosition = null;
                legalMovePositions.clear();
                updateHighlights();
                return;
            }

            // Clicked on another square — try to select a new piece
            selectedPosition = null;
            legalMovePositions.clear();
        }

        // Try to select a piece at the clicked position
        Piece piece = board.getPiece(boardRow, boardCol);
        if (piece != null) {
            selectedPosition = clickedPos;
            // Get legal moves for this piece from the board
            legalMovePositions = board.getLegalMoves(boardRow, boardCol);
            updateHighlights();
        }
    }

    /**
     * Updates the visual highlights on the board.
     */
    private void updateHighlights() {
        for (int displayRow = 0; displayRow < BOARD_SIZE; displayRow++) {
            for (int displayCol = 0; displayCol < BOARD_SIZE; displayCol++) {
                int boardRow = flipped ? (7 - displayRow) : displayRow;
                int boardCol = flipped ? (7 - displayCol) : displayCol;

                boolean isLight = (boardRow + boardCol) % 2 == 0;
                Color baseColor = isLight ? LIGHT_SQUARE : DARK_COLOR;

                Position pos = new Position(boardRow, boardCol);
                Color color = baseColor;

                // Highlight last move
                if (lastMoveFrom != null && pos.equals(lastMoveFrom)) {
                    color = LAST_MOVE_COLOR;
                } else if (lastMoveTo != null && pos.equals(lastMoveTo)) {
                    color = LAST_MOVE_COLOR;
                }

                // Highlight selected square
                if (selectedPosition != null && pos.equals(selectedPosition)) {
                    color = SELECTED_COLOR;
                }

                squares[displayRow][displayCol].setFill(color);

                // Add legal move indicators (small circles)
                StackPane cell = cells[displayRow][displayCol];
                cell.getChildren().removeIf(node -> node instanceof CircleIndicator);

                if (legalMovePositions.contains(pos)) {
                    Piece targetPiece = board != null ? board.getPiece(boardRow, boardCol) : null;
                    CircleIndicator indicator;
                    if (targetPiece != null) {
                        // Capture indicator — ring around the square
                        indicator = new CircleIndicator(SQUARE_SIZE / 2 - 4, true);
                    } else {
                        // Move indicator — small dot in center
                        indicator = new CircleIndicator(10, false);
                    }
                    cell.getChildren().add(indicator);
                }
            }
        }
    }

    private static final Color DARK_COLOR = Color.rgb(181, 136, 99);

    /**
     * Circle indicator for legal moves.
     */
    private static class CircleIndicator extends javafx.scene.shape.Circle {
        CircleIndicator(double radius, boolean capture) {
            super(radius);
            if (capture) {
                setFill(Color.TRANSPARENT);
                setStroke(LEGAL_MOVE_COLOR);
                setStrokeWidth(4);
            } else {
                setFill(LEGAL_MOVE_COLOR);
                setStroke(null);
            }
            setMouseTransparent(true);
        }
    }

    /**
     * Sets the callback for move events.
     */
    public void setMoveCallback(MoveCallback callback) {
        this.moveCallback = callback;
    }

    /**
     * Sets the last move for highlighting.
     */
    public void setLastMove(Position from, Position to) {
        this.lastMoveFrom = from;
        this.lastMoveTo = to;
        updateHighlights();
    }

    /**
     * Flips the board orientation.
     */
    public void setFlipped(boolean flipped) {
        if (this.flipped != flipped) {
            this.flipped = flipped;
            buildBoard();
            if (board != null) {
                drawPieces();
                updateHighlights();
            }
        }
    }

    /**
     * Clears the current selection.
     */
    public void clearSelection() {
        selectedPosition = null;
        legalMovePositions.clear();
        updateHighlights();
    }

    /**
     * @return the square size in pixels
     */
    public double getSquareSize() {
        return SQUARE_SIZE;
    }
}
