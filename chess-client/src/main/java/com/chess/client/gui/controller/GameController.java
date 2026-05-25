package com.chess.client.gui.controller;

import com.chess.client.gui.ChessBoardWidget;
import com.chess.client.gui.SceneNavigator;
import com.chess.client.net.MessageListener;
import com.chess.client.net.ServerConnection;
import com.chess.common.game.Board;
import com.chess.common.model.GameColor;
import com.chess.common.model.PieceType;
import com.chess.common.model.Position;
import com.chess.common.protocol.Message;
import com.chess.common.protocol.MessageType;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.StackPane;

import java.io.IOException;
import java.util.logging.Logger;

public class GameController implements MessageListener {

    private static final Logger LOGGER = Logger.getLogger(GameController.class.getName());

    @FXML private StackPane boardContainer;
    @FXML private Label turnLabel;
    @FXML private Label gameStatusLabel;
    @FXML private Label opponentLabel;
    @FXML private Label playerLabel;
    @FXML private Label colorLabel;
    @FXML private Label roomIdIdLabel;
    @FXML private Label statusLabel;
    @FXML private ListView<String> moveHistoryList;
    @FXML private Button acceptDrawBtn;
    @FXML private Button declineDrawBtn;

    private ServerConnection connection;
    private SceneNavigator navigator;

    private ChessBoardWidget boardWidget;
    private Board board;
    private GameColor myColor;
    private String opponentName;
    private String roomId;
    private boolean myTurn = false;
    private boolean gameOver = false;

    private final ObservableList<String> moveHistory = FXCollections.observableArrayList();

    public void setConnection(ServerConnection connection) {
        this.connection = connection;
    }

    public void setNavigator(SceneNavigator navigator) {
        this.navigator = navigator;
    }

    @FXML
    private void initialize() {
        boardWidget = new ChessBoardWidget();
        boardContainer.getChildren().add(boardWidget);

        moveHistoryList.setItems(moveHistory);
        moveHistoryList.setStyle("-fx-font-family: monospace; -fx-font-size: 12px;");
    }

    public void initGame(String colorStr, String opponent, String roomId, String fen) {
        this.myColor = GameColor.valueOf(colorStr);
        this.opponentName = opponent;
        this.roomId = roomId;
        this.board = new Board(fen);
        this.gameOver = false;
        this.myTurn = (myColor == GameColor.WHITE);

        connection.setListener(this);

        colorLabel.setText("Вы играете: " + (myColor == GameColor.WHITE ? "Белыми" : "Чёрными"));
        roomIdIdLabel.setText("Комната: " + roomId);

        if (myColor == GameColor.WHITE) {
            playerLabel.setText("Вы (Белые)");
            opponentLabel.setText(opponent + " (Чёрные)");
            boardWidget.setFlipped(false);
        } else {
            playerLabel.setText("Вы (Чёрные)");
            opponentLabel.setText(opponent + " (Белые)");
            boardWidget.setFlipped(true);
        }

        updateTurnLabel();
        boardWidget.setBoard(board);

        boardWidget.setMoveCallback((from, to, promotionPiece) -> {
            if (!myTurn || gameOver) {
                statusLabel.setText("Сейчас не ваш ход!");
                return;
            }
            sendMove(from, to, promotionPiece);
        });

        moveHistory.clear();
        hideDrawButtons();
        statusLabel.setText("Игра началась!");
    }

    private void updateTurnLabel() {
        if (gameOver) {
            turnLabel.setText("Игра окончена");
            return;
        }
        if (myTurn) {
            turnLabel.setText("Ваш ход (" + (myColor == GameColor.WHITE ? "Белые" : "Чёрные") + ")");
            turnLabel.setStyle("-fx-text-fill: #5a9e5a; -fx-font-size: 14px;");
        } else {
            turnLabel.setText("Ход соперника");
            turnLabel.setStyle("-fx-text-fill: #cccccc; -fx-font-size: 14px;");
        }
    }

    private void sendMove(Position from, Position to, PieceType promotionPiece) {
        String fromStr = from.toAlgebraic();
        String toStr = to.toAlgebraic();

        try {
            if (promotionPiece != null) {
                connection.sendMessage(new Message(MessageType.MOVE, fromStr, toStr, promotionPiece.name()));
            } else {
                connection.sendMessage(new Message(MessageType.MOVE, fromStr, toStr));
            }
            statusLabel.setText("Отправка хода...");
            boardWidget.clearSelection();
        } catch (IOException e) {
            statusLabel.setText("Ошибка отправки хода: " + e.getMessage());
        }
    }

    @FXML
    private void onResign() {
        if (gameOver) return;

        try {
            connection.sendMessage(new Message(MessageType.RESIGN));
            statusLabel.setText("Вы сдались.");
        } catch (IOException e) {
            statusLabel.setText("Ошибка: " + e.getMessage());
        }
    }

    @FXML
    private void onOfferDraw() {
        if (gameOver) return;

        try {
            connection.sendMessage(new Message(MessageType.OFFER_DRAW));
            statusLabel.setText("Предложение ничьей отправлено.");
        } catch (IOException e) {
            statusLabel.setText("Ошибка: " + e.getMessage());
        }
    }

    @FXML
    private void onAcceptDraw() {
        if (gameOver) return;

        try {
            connection.sendMessage(new Message(MessageType.ACCEPT_DRAW));
            statusLabel.setText("Ничья принята.");
            hideDrawButtons();
        } catch (IOException e) {
            statusLabel.setText("Ошибка: " + e.getMessage());
        }
    }

    @FXML
    private void onDeclineDraw() {
        if (gameOver) return;

        try {
            connection.sendMessage(new Message(MessageType.DECLINE_DRAW));
            statusLabel.setText("Ничья отклонена.");
            hideDrawButtons();
        } catch (IOException e) {
            statusLabel.setText("Ошибка: " + e.getMessage());
        }
    }

    private void showDrawButtons() {
        acceptDrawBtn.setVisible(true);
        acceptDrawBtn.setManaged(true);
        declineDrawBtn.setVisible(true);
        declineDrawBtn.setManaged(true);
    }

    private void hideDrawButtons() {
        acceptDrawBtn.setVisible(false);
        acceptDrawBtn.setManaged(false);
        declineDrawBtn.setVisible(false);
        declineDrawBtn.setManaged(false);
    }

    @FXML
    private void onLeaveRoom() {
        try {
            connection.sendMessage(new Message(MessageType.LEAVE_ROOM));
        } catch (IOException e) {
        }

        if (navigator != null) {
            LobbyController controller = navigator.navigateToAndGetController(SceneNavigator.LOBBY);
            if (controller != null) {
                controller.setConnection(connection);
                controller.setNavigator(navigator);
                controller.requestRoomList();
            }
        }
    }

    @Override
    public void onMessageReceived(Message message) {
        Platform.runLater(() -> {
            switch (message.getType()) {
                case MOVE_OK:
                    handleMoveOk(message);
                    break;

                case OPPONENT_MOVE:
                    handleOpponentMove(message);
                    break;

                case MOVE_INVALID:
                    statusLabel.setText("Недопустимый ход: " + message.getParam(0));
                    boardWidget.clearSelection();
                    break;

                case GAME_OVER:
                    handleGameOver(message);
                    break;

                case DRAW_OFFERED:
                    if (message.getParamCount() > 0 && "DECLINED".equals(message.getParam(0))) {
                        statusLabel.setText("Соперник отклонил предложение ничьей.");
                    } else {
                        statusLabel.setText("Соперник предлагает ничью. Принять?");
                        showDrawButtons();
                    }
                    break;

                case OPPONENT_DISCONNECTED:
                    statusLabel.setText("Соперник отключился. Вы победили!");
                    gameStatusLabel.setText("Соперник отключился — Победа!");
                    gameStatusLabel.setStyle("-fx-text-fill: #5a9e5a; -fx-font-size: 14px; -fx-font-weight: bold;");
                    gameOver = true;
                    updateTurnLabel();
                    break;

                case ERROR:
                    statusLabel.setText("Ошибка: " + message.getParam(0));
                    break;

                default:
                    break;
            }
        });
    }

    private void handleMoveOk(Message message) {
        String fromStr = message.getParam(0);
        String toStr = message.getParam(1);
        String fen = message.getParam(2);
        String promoStr = message.getParamCount() > 3 ? message.getParam(3) : "";

        board = new Board(fen);
        boardWidget.setBoard(board);
        boardWidget.setLastMove(
            Position.fromAlgebraic(fromStr),
            Position.fromAlgebraic(toStr)
        );

        int moveNum = moveHistory.size() + 1;
        String moveText = fromStr + "→" + toStr;
        if (!promoStr.isEmpty()) moveText += "=" + promoStr;
        moveHistory.add(moveNum + ". " + moveText);
        moveHistoryList.scrollTo(moveHistory.size() - 1);

        myTurn = false;
        updateTurnLabel();
        statusLabel.setText("Ход отправлен. Ожидание соперника...");
    }

    private void handleOpponentMove(Message message) {
        String fromStr = message.getParam(0);
        String toStr = message.getParam(1);
        String fen = message.getParam(2);
        String promoStr = message.getParamCount() > 3 ? message.getParam(3) : "";

        board = new Board(fen);
        boardWidget.setBoard(board);
        boardWidget.setLastMove(
            Position.fromAlgebraic(fromStr),
            Position.fromAlgebraic(toStr)
        );

        int moveNum = moveHistory.size() + 1;
        String moveText = fromStr + "→" + toStr;
        if (!promoStr.isEmpty()) moveText += "=" + promoStr;
        moveHistory.add(moveNum + ". ..." + moveText);
        moveHistoryList.scrollTo(moveHistory.size() - 1);

        myTurn = true;
        updateTurnLabel();
        statusLabel.setText("Ваш ход!");
    }

    private void handleGameOver(Message message) {
        gameOver = true;
        hideDrawButtons();
        String result = message.getParam(0);
        String reason = message.getParam(1);
        String ratingChange = message.getParamCount() > 2 ? message.getParam(2) : "";

        String displayText;
        switch (result) {
            case "WIN":
                displayText = "Вы победили!";
                break;
            case "LOSE":
                displayText = "Вы проиграли!";
                break;
            case "DRAW":
                displayText = "Ничья!";
                break;
            default:
                displayText = result;
        }

        gameStatusLabel.setText(displayText + " (" + reason + ")");
        gameStatusLabel.setStyle("-fx-text-fill: #f0d9b5; -fx-font-size: 14px; -fx-font-weight: bold;");
        statusLabel.setText("Игра окончена: " + displayText + " — " + reason);
        updateTurnLabel();
    }

    @Override
    public void onConnectionLost() {
        Platform.runLater(() -> {
            statusLabel.setText("Соединение с сервером потеряно!");
            statusLabel.setStyle("-fx-text-fill: #ff6b6b; -fx-font-size: 12px;");
            gameOver = true;
            updateTurnLabel();
        });
    }

    @Override
    public void onError(String error) {
        Platform.runLater(() -> statusLabel.setText("Ошибка: " + error));
    }
}
