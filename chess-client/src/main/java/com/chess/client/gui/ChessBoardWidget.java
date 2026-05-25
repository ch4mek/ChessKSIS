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
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

public class ChessBoardWidget extends GridPane {

    private static final Logger LOGGER = Logger.getLogger(ChessBoardWidget.class.getName());

    private static final int BOARD_SIZE = 8;
    private static final double SQUARE_SIZE = 70;

    private static final Color LIGHT_SQUARE = Color.rgb(240, 217, 181);
    private static final Color DARK_SQUARE = Color.rgb(181, 136, 99);
    private static final Color SELECTED_COLOR = Color.rgb(130, 151, 105, 0.7);
    private static final Color LEGAL_MOVE_COLOR = Color.rgb(130, 151, 105, 0.5);
    private static final Color LAST_MOVE_COLOR = Color.rgb(205, 210, 106, 0.5);
    private static final Color CHECK_COLOR = Color.rgb(235, 64, 52, 0.7);

    private final Rectangle[][] squares = new Rectangle[BOARD_SIZE][BOARD_SIZE];
    private final StackPane[][] cells = new StackPane[BOARD_SIZE][BOARD_SIZE];

    private Board board;
    private boolean flipped = false;
    private Position selectedPosition = null;
    private List<Position> legalMovePositions = new ArrayList<>();
    private Position lastMoveFrom = null;
    private Position lastMoveTo = null;

    private MoveCallback moveCallback;

    public interface MoveCallback {
        void onMove(Position from, Position to, PieceType promotionPiece);
    }

    public ChessBoardWidget() {
        setHgap(0);
        setVgap(0);
        setPadding(javafx.geometry.Insets.EMPTY);
        buildBoard();
    }

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

        addRankFileLabels();
    }

    private void addRankFileLabels() {
    }

    public void setBoard(Board board) {
        this.board = board;
        drawPieces();
        updateHighlights();
    }

    private void drawPieces() {
        if (board == null) return;

        for (int displayRow = 0; displayRow < BOARD_SIZE; displayRow++) {
            for (int displayCol = 0; displayCol < BOARD_SIZE; displayCol++) {
                int boardRow = flipped ? (7 - displayRow) : displayRow;
                int boardCol = flipped ? (7 - displayCol) : displayCol;

                StackPane cell = cells[displayRow][displayCol];
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

    private void onSquareClicked(int boardRow, int boardCol) {
        if (board == null) return;

        Position clickedPos = new Position(boardRow, boardCol);

        if (selectedPosition != null) {
            if (clickedPos.equals(selectedPosition)) {
                selectedPosition = null;
                legalMovePositions.clear();
                updateHighlights();
                return;
            }

            if (legalMovePositions.contains(clickedPos)) {
                if (moveCallback != null) {
                    Piece selectedPiece = board.getPiece(selectedPosition.getRow(), selectedPosition.getCol());
                    PieceType promotionPiece = null;

                    if (selectedPiece != null && selectedPiece.getType() == PieceType.PAWN) {
                        int promotionRow = (selectedPiece.getColor() == GameColor.WHITE) ? 0 : 7;
                        if (clickedPos.getRow() == promotionRow) {
                            promotionPiece = showPromotionDialog(selectedPiece.getColor());
                        }
                    }

                    moveCallback.onMove(selectedPosition, clickedPos, promotionPiece);
                }
                selectedPosition = null;
                legalMovePositions.clear();
                updateHighlights();
                return;
            }

            selectedPosition = null;
            legalMovePositions.clear();
        }

        Piece piece = board.getPiece(boardRow, boardCol);
        if (piece != null) {
            selectedPosition = clickedPos;
            legalMovePositions = board.getLegalMoves(boardRow, boardCol);
            updateHighlights();
        }
    }

    private void updateHighlights() {
        Position checkKingPos = null;
        if (board != null && board.isInCheck(board.getCurrentTurn())) {
            checkKingPos = board.findKing(board.getCurrentTurn());
        }

        for (int displayRow = 0; displayRow < BOARD_SIZE; displayRow++) {
            for (int displayCol = 0; displayCol < BOARD_SIZE; displayCol++) {
                int boardRow = flipped ? (7 - displayRow) : displayRow;
                int boardCol = flipped ? (7 - displayCol) : displayCol;

                boolean isLight = (boardRow + boardCol) % 2 == 0;
                Color baseColor = isLight ? LIGHT_SQUARE : DARK_COLOR;

                Position pos = new Position(boardRow, boardCol);
                Color color = baseColor;

                if (lastMoveFrom != null && pos.equals(lastMoveFrom)) {
                    color = LAST_MOVE_COLOR;
                } else if (lastMoveTo != null && pos.equals(lastMoveTo)) {
                    color = LAST_MOVE_COLOR;
                }

                if (selectedPosition != null && pos.equals(selectedPosition)) {
                    color = SELECTED_COLOR;
                }

                if (checkKingPos != null && pos.equals(checkKingPos)) {
                    color = CHECK_COLOR;
                }

                squares[displayRow][displayCol].setFill(color);

                StackPane cell = cells[displayRow][displayCol];
                cell.getChildren().removeIf(node -> node instanceof CircleIndicator);

                if (legalMovePositions.contains(pos)) {
                    Piece targetPiece = board != null ? board.getPiece(boardRow, boardCol) : null;
                    CircleIndicator indicator;
                    if (targetPiece != null) {
                        indicator = new CircleIndicator(SQUARE_SIZE / 2 - 4, true);
                    } else {
                        indicator = new CircleIndicator(10, false);
                    }
                    cell.getChildren().add(indicator);
                }
            }
        }
    }

    private static final Color DARK_COLOR = Color.rgb(181, 136, 99);

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

    private PieceType showPromotionDialog(GameColor color) {
        List<String> choices = Arrays.asList("Ферзь", "Ладья", "Слон", "Конь");
        javafx.scene.control.ChoiceDialog<String> dialog = new javafx.scene.control.ChoiceDialog<>("Ферзь", choices);
        dialog.setTitle("Превращение пешки");
        dialog.setHeaderText("Выберите фигуру:");
        dialog.setContentText("Превратить в:");

        dialog.getDialogPane().setStyle(
            "-fx-background-color: #312e2b; " +
            "-fx-text-fill: #f0d9b5;"
        );
        dialog.getDialogPane().lookupButton(javafx.scene.control.ButtonType.OK).setStyle(
            "-fx-background-color: #629924; -fx-text-fill: white;"
        );

        Optional<String> result = dialog.showAndWait();
        if (result.isPresent()) {
            switch (result.get()) {
                case "Ладья":  return PieceType.ROOK;
                case "Слон":   return PieceType.BISHOP;
                case "Конь":   return PieceType.KNIGHT;
                default:       return PieceType.QUEEN;
            }
        }
        return PieceType.QUEEN;
    }

    public void setMoveCallback(MoveCallback callback) {
        this.moveCallback = callback;
    }

    public void setLastMove(Position from, Position to) {
        this.lastMoveFrom = from;
        this.lastMoveTo = to;
        updateHighlights();
    }

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

    public void clearSelection() {
        selectedPosition = null;
        legalMovePositions.clear();
        updateHighlights();
    }

    public double getSquareSize() {
        return SQUARE_SIZE;
    }
}
