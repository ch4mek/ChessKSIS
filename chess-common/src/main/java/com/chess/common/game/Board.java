package com.chess.common.game;

import com.chess.common.model.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class Board {

    private static final String INITIAL_FEN =
            "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";

    private final Piece[][] squares;
    private GameColor currentTurn;
    private boolean whiteKingSideCastle;
    private boolean whiteQueenSideCastle;
    private boolean blackKingSideCastle;
    private boolean blackQueenSideCastle;
    private Position enPassantTarget;
    private int halfMoveClock;
    private int fullMoveNumber;
    private final List<String> positionHistory;


    public Board() {
        this.squares = new Piece[8][8];
        this.positionHistory = new ArrayList<>();
        initializeStandardPosition();
    }


    public Board(String fen) {
        this.squares = new Piece[8][8];
        this.positionHistory = new ArrayList<>();
        parseFEN(fen);
    }


    private Board(Piece[][] squares, GameColor currentTurn,
                  boolean wksc, boolean wqsc, boolean bksc, boolean bqsc,
                  Position enPassantTarget, int halfMoveClock, int fullMoveNumber,
                  List<String> positionHistory) {
        this.squares = new Piece[8][8];
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                if (squares[r][c] != null) {
                    this.squares[r][c] = squares[r][c].copy();
                }
            }
        }
        this.currentTurn = currentTurn;
        this.whiteKingSideCastle = wksc;
        this.whiteQueenSideCastle = wqsc;
        this.blackKingSideCastle = bksc;
        this.blackQueenSideCastle = bqsc;
        this.enPassantTarget = enPassantTarget;
        this.halfMoveClock = halfMoveClock;
        this.fullMoveNumber = fullMoveNumber;
        this.positionHistory = new ArrayList<>(positionHistory);
    }



    private void initializeStandardPosition() {
        parseFEN(INITIAL_FEN);
    }


    public final void parseFEN(String fen) {

        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                squares[r][c] = null;
            }
        }

        String[] parts = fen.split(" ");
        if (parts.length < 4) {
            throw new IllegalArgumentException("Invalid FEN: expected at least 4 parts, got " + parts.length);
        }


        String[] ranks = parts[0].split("/");
        if (ranks.length != 8) {
            throw new IllegalArgumentException("Invalid FEN: expected 8 ranks");
        }
        for (int r = 0; r < 8; r++) {
            int col = 0;
            for (char c : ranks[r].toCharArray()) {
                if (Character.isDigit(c)) {
                    col += c - '0';
                } else {
                    squares[r][col] = Piece.fromFenChar(c);
                    col++;
                }
            }
        }

        currentTurn = parts[1].equals("w") ? GameColor.WHITE : GameColor.BLACK;

        String castling = parts[2];
        whiteKingSideCastle = castling.contains("K");
        whiteQueenSideCastle = castling.contains("Q");
        blackKingSideCastle = castling.contains("k");
        blackQueenSideCastle = castling.contains("q");

        if (parts[3].equals("-")) {
            enPassantTarget = null;
        } else {
            enPassantTarget = Position.fromAlgebraic(parts[3]);
        }

        halfMoveClock = parts.length > 4 ? Integer.parseInt(parts[4]) : 0;

        fullMoveNumber = parts.length > 5 ? Integer.parseInt(parts[5]) : 1;

        positionHistory.clear();
        positionHistory.add(getPositionKey());
    }


    public String toFEN() {
        StringBuilder sb = new StringBuilder();

        for (int r = 0; r < 8; r++) {
            int empty = 0;
            for (int c = 0; c < 8; c++) {
                if (squares[r][c] == null) {
                    empty++;
                } else {
                    if (empty > 0) {
                        sb.append(empty);
                        empty = 0;
                    }
                    sb.append(squares[r][c].toFenChar());
                }
            }
            if (empty > 0) {
                sb.append(empty);
            }
            if (r < 7) {
                sb.append('/');
            }
        }

        sb.append(' ').append(currentTurn.toFenChar());

        sb.append(' ');
        StringBuilder castling = new StringBuilder();
        if (whiteKingSideCastle) castling.append('K');
        if (whiteQueenSideCastle) castling.append('Q');
        if (blackKingSideCastle) castling.append('k');
        if (blackQueenSideCastle) castling.append('q');
        sb.append(castling.length() > 0 ? castling : "-");

        sb.append(' ').append(enPassantTarget != null ? enPassantTarget.toAlgebraic() : "-");

        sb.append(' ').append(halfMoveClock);

        sb.append(' ').append(fullMoveNumber);

        return sb.toString();
    }



    public Piece getPiece(Position pos) {
        return squares[pos.getRow()][pos.getCol()];
    }


    public Piece getPiece(int row, int col) {
        return squares[row][col];
    }


    public void setPiece(Position pos, Piece piece) {
        squares[pos.getRow()][pos.getCol()] = piece;
    }

    public GameColor getCurrentTurn() {
        return currentTurn;
    }

    public int getHalfMoveClock() {
        return halfMoveClock;
    }

    public int getFullMoveNumber() {
        return fullMoveNumber;
    }

    public Position getEnPassantTarget() {
        return enPassantTarget;
    }

    public boolean canWhiteKingSideCastle() {
        return whiteKingSideCastle;
    }

    public boolean canWhiteQueenSideCastle() {
        return whiteQueenSideCastle;
    }

    public boolean canBlackKingSideCastle() {
        return blackKingSideCastle;
    }

    public boolean canBlackQueenSideCastle() {
        return blackQueenSideCastle;
    }


    public List<Move> generatePseudoLegalMoves(GameColor color) {
        List<Move> moves = new ArrayList<>();
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                Piece piece = squares[r][c];
                if (piece != null && piece.getColor() == color) {
                    Position from = new Position(r, c);
                    generatePieceMoves(from, piece, moves);
                }
            }
        }
        return moves;
    }


    public List<Move> getLegalMoves(GameColor color) {
        List<Move> pseudoLegal = generatePseudoLegalMoves(color);
        List<Move> legal = new ArrayList<>();

        for (Move move : pseudoLegal) {
            if (isLegalMove(move, color)) {
                legal.add(move);
            }
        }
        return legal;
    }


    public List<Position> getLegalMoves(int row, int col) {
        Piece piece = getPiece(row, col);
        if (piece == null) {
            return new ArrayList<>();
        }

        List<Move> allLegal = getLegalMoves(piece.getColor());
        List<Position> destinations = new ArrayList<>();
        for (Move move : allLegal) {
            if (move.getFrom().getRow() == row && move.getFrom().getCol() == col) {
                destinations.add(move.getTo());
            }
        }
        return destinations;
    }


    private void generatePieceMoves(Position from, Piece piece, List<Move> moves) {
        switch (piece.getType()) {
            case PAWN -> generatePawnMoves(from, piece, moves);
            case KNIGHT -> generateKnightMoves(from, piece, moves);
            case BISHOP -> generateSlidingMoves(from, piece, new int[][]{{-1, -1}, {-1, 1}, {1, -1}, {1, 1}}, moves);
            case ROOK -> generateSlidingMoves(from, piece, new int[][]{{-1, 0}, {1, 0}, {0, -1}, {0, 1}}, moves);
            case QUEEN -> generateSlidingMoves(from, piece,
                    new int[][]{{-1, -1}, {-1, 1}, {1, -1}, {1, 1}, {-1, 0}, {1, 0}, {0, -1}, {0, 1}}, moves);
            case KING -> generateKingMoves(from, piece, moves);
        }
    }



    private void generatePawnMoves(Position from, Piece pawn, List<Move> moves) {
        int direction = pawn.getColor() == GameColor.WHITE ? -1 : 1;
        int startRow = pawn.getColor() == GameColor.WHITE ? 6 : 1;
        int promotionRow = pawn.getColor() == GameColor.WHITE ? 0 : 7;

        Position oneForward = new Position(from.getRow() + direction, from.getCol());
        if (isOnBoard(oneForward) && getPiece(oneForward) == null) {
            if (oneForward.getRow() == promotionRow) {
                addPromotionMoves(from, oneForward, moves);
            } else {
                moves.add(new Move(from, oneForward));

                if (from.getRow() == startRow) {
                    Position twoForward = new Position(from.getRow() + 2 * direction, from.getCol());
                    if (getPiece(twoForward) == null) {
                        Move doublePush = new Move(from, twoForward);
                        moves.add(doublePush);
                    }
                }
            }
        }

        int[] captureCols = {from.getCol() - 1, from.getCol() + 1};
        for (int captureCol : captureCols) {
            int captureRow = from.getRow() + direction;
            if (captureRow < 0 || captureRow > 7 || captureCol < 0 || captureCol > 7) continue;
            Position captureTarget = new Position(captureRow, captureCol);

            Piece targetPiece = getPiece(captureTarget);
            if (targetPiece != null && targetPiece.getColor() != pawn.getColor()) {
                if (captureTarget.getRow() == promotionRow) {
                    addPromotionMoves(from, captureTarget, moves);
                } else {
                    moves.add(new Move(from, captureTarget));
                }
            }

            if (enPassantTarget != null && captureTarget.equals(enPassantTarget)) {
                Move epMove = new Move(from, captureTarget);
                epMove.setEnPassant(true);
                moves.add(epMove);
            }
        }
    }

    private void addPromotionMoves(Position from, Position to, List<Move> moves) {
        moves.add(new Move(from, to, PieceType.QUEEN));
        moves.add(new Move(from, to, PieceType.ROOK));
        moves.add(new Move(from, to, PieceType.BISHOP));
        moves.add(new Move(from, to, PieceType.KNIGHT));
    }



    private void generateKnightMoves(Position from, Piece knight, List<Move> moves) {
        int[][] offsets = {{-2, -1}, {-2, 1}, {-1, -2}, {-1, 2}, {1, -2}, {1, 2}, {2, -1}, {2, 1}};
        for (int[] offset : offsets) {
            int r = from.getRow() + offset[0];
            int c = from.getCol() + offset[1];
            if (r < 0 || r > 7 || c < 0 || c > 7) continue;
            Position to = new Position(r, c);
            Piece target = getPiece(to);
            if (target == null || target.getColor() != knight.getColor()) {
                moves.add(new Move(from, to));
            }
        }
    }



    private void generateSlidingMoves(Position from, Piece piece, int[][] directions, List<Move> moves) {
        for (int[] dir : directions) {
            int r = from.getRow() + dir[0];
            int c = from.getCol() + dir[1];
            while (r >= 0 && r < 8 && c >= 0 && c < 8) {
                Position to = new Position(r, c);
                Piece target = getPiece(to);
                if (target == null) {
                    moves.add(new Move(from, to));
                } else {
                    if (target.getColor() != piece.getColor()) {
                        moves.add(new Move(from, to));
                    }
                    break;
                }
                r += dir[0];
                c += dir[1];
            }
        }
    }



    private void generateKingMoves(Position from, Piece king, List<Move> moves) {
        int[][] offsets = {{-1, -1}, {-1, 0}, {-1, 1}, {0, -1}, {0, 1}, {1, -1}, {1, 0}, {1, 1}};
        for (int[] offset : offsets) {
            int r = from.getRow() + offset[0];
            int c = from.getCol() + offset[1];
            if (r < 0 || r > 7 || c < 0 || c > 7) continue;
            Position to = new Position(r, c);
            Piece target = getPiece(to);
            if (target == null || target.getColor() != king.getColor()) {
                moves.add(new Move(from, to));
            }
        }

        generateCastlingMoves(from, king, moves);
    }

    private void generateCastlingMoves(Position kingPos, Piece king, List<Move> moves) {
        if (king.hasMoved()) return;

        int row = kingPos.getRow();

        boolean ksc = king.getColor() == GameColor.WHITE ? whiteKingSideCastle : blackKingSideCastle;
        if (ksc) {
            if (squares[row][5] == null && squares[row][6] == null) {
                Piece rook = squares[row][7];
                if (rook != null && rook.getType() == PieceType.ROOK && !rook.hasMoved()) {
                    if (!isSquareAttacked(new Position(row, 4), king.getColor().opposite()) &&
                            !isSquareAttacked(new Position(row, 5), king.getColor().opposite()) &&
                            !isSquareAttacked(new Position(row, 6), king.getColor().opposite())) {
                        Move castle = new Move(kingPos, new Position(row, 6));
                        castle.setCastling(true);
                        moves.add(castle);
                    }
                }
            }
        }

        boolean qsc = king.getColor() == GameColor.WHITE ? whiteQueenSideCastle : blackQueenSideCastle;
        if (qsc) {
            if (squares[row][1] == null && squares[row][2] == null && squares[row][3] == null) {
                Piece rook = squares[row][0];
                if (rook != null && rook.getType() == PieceType.ROOK && !rook.hasMoved()) {
                    if (!isSquareAttacked(new Position(row, 4), king.getColor().opposite()) &&
                            !isSquareAttacked(new Position(row, 3), king.getColor().opposite()) &&
                            !isSquareAttacked(new Position(row, 2), king.getColor().opposite())) {
                        Move castle = new Move(kingPos, new Position(row, 2));
                        castle.setCastling(true);
                        moves.add(castle);
                    }
                }
            }
        }
    }

    public boolean isLegalMove(Move move, GameColor color) {
        Board copy = this.clone();

        copy.executeMove(move);

        return !copy.isInCheck(color);
    }

    public boolean makeMove(Move move) {
        Piece piece = getPiece(move.getFrom());
        if (piece == null || piece.getColor() != currentTurn) {
            return false;
        }

        List<Move> legalMoves = getLegalMoves(currentTurn);
        boolean isLegal = false;
        Move legalMove = null;
        Move fallbackPromotion = null;
        for (Move legal : legalMoves) {
            if (legal.getFrom().equals(move.getFrom()) && legal.getTo().equals(move.getTo())) {
                if (move.isPromotion()) {
                    if (legal.isPromotion() && legal.getPromotionPiece() == move.getPromotionPiece()) {
                        isLegal = true;
                        legalMove = legal;
                        break;
                    }
                } else if (!legal.isPromotion()) {
                    isLegal = true;
                    legalMove = legal;
                    break;
                }
                if (!move.isPromotion() && legal.isPromotion() && fallbackPromotion == null) {
                    fallbackPromotion = legal;
                }
            }
        }

        if (!isLegal && fallbackPromotion != null) {
            isLegal = true;
            legalMove = fallbackPromotion;
        }

        if (!isLegal) {
            return false;
        }

        boolean isCapture = getPiece(legalMove.getTo()) != null || legalMove.isEnPassant();
        Piece movedPiece = getPiece(legalMove.getFrom());
        Piece capturedPiece = getPiece(legalMove.getTo());

        executeMove(legalMove);

        updateCastlingRights(legalMove, capturedPiece);
        updateEnPassantTarget(legalMove);
        updateMoveCounters(movedPiece, isCapture);

        currentTurn = currentTurn.opposite();
        fullMoveNumber += (currentTurn == GameColor.WHITE) ? 1 : 0;

        positionHistory.add(getPositionKey());

        return true;
    }

    public void executeMove(Move move) {
        Piece piece = getPiece(move.getFrom());
        if (piece == null) return;

        if (move.isEnPassant()) {
            int capturedRow = move.getFrom().getRow();
            int capturedCol = move.getTo().getCol();
            squares[capturedRow][capturedCol] = null;
        }

        if (move.isCastling()) {
            int row = move.getFrom().getRow();
            if (move.isKingsideCastling()) {
                squares[row][5] = squares[row][7];
                squares[row][7] = null;
                if (squares[row][5] != null) squares[row][5].setHasMoved(true);
            } else {
                squares[row][3] = squares[row][0];
                squares[row][0] = null;
                if (squares[row][3] != null) squares[row][3].setHasMoved(true);
            }
        }

        squares[move.getTo().getRow()][move.getTo().getCol()] = piece;
        squares[move.getFrom().getRow()][move.getFrom().getCol()] = null;
        piece.setHasMoved(true);

        if (move.isPromotion() && piece.getType() == PieceType.PAWN) {
            squares[move.getTo().getRow()][move.getTo().getCol()] =
                    new Piece(move.getPromotionPiece(), piece.getColor(), true);
        }
    }

    private void updateCastlingRights(Move move, Piece capturedPiece) {
        Piece piece = getPiece(move.getTo());

        if (piece != null && piece.getType() == PieceType.KING) {
            if (piece.getColor() == GameColor.WHITE) {
                whiteKingSideCastle = false;
                whiteQueenSideCastle = false;
            } else {
                blackKingSideCastle = false;
                blackQueenSideCastle = false;
            }
        }

        if (piece != null && piece.getType() == PieceType.ROOK) {
            if (piece.getColor() == GameColor.WHITE) {
                if (move.getFrom().equals(new Position(7, 0))) whiteQueenSideCastle = false;
                if (move.getFrom().equals(new Position(7, 7))) whiteKingSideCastle = false;
            } else {
                if (move.getFrom().equals(new Position(0, 0))) blackQueenSideCastle = false;
                if (move.getFrom().equals(new Position(0, 7))) blackKingSideCastle = false;
            }
        }

        if (capturedPiece != null && capturedPiece.getType() == PieceType.ROOK) {
            if (capturedPiece.getColor() == GameColor.WHITE) {
                if (move.getTo().equals(new Position(7, 0))) whiteQueenSideCastle = false;
                if (move.getTo().equals(new Position(7, 7))) whiteKingSideCastle = false;
            } else {
                if (move.getTo().equals(new Position(0, 0))) blackQueenSideCastle = false;
                if (move.getTo().equals(new Position(0, 7))) blackKingSideCastle = false;
            }
        }
    }

    private void updateEnPassantTarget(Move move) {
        Piece piece = getPiece(move.getTo());
        if (piece != null && piece.getType() == PieceType.PAWN) {
            int rowDiff = Math.abs(move.getTo().getRow() - move.getFrom().getRow());
            if (rowDiff == 2) {
                int epRow = (move.getFrom().getRow() + move.getTo().getRow()) / 2;
                enPassantTarget = new Position(epRow, move.getFrom().getCol());
            } else {
                enPassantTarget = null;
            }
        } else {
            enPassantTarget = null;
        }
    }

    private void updateMoveCounters(Piece movedPiece, boolean isCapture) {
        if (movedPiece != null && movedPiece.getType() == PieceType.PAWN) {
            halfMoveClock = 0;
        } else if (isCapture) {
            halfMoveClock = 0;
        } else {
            halfMoveClock++;
        }
    }

    public Position findKing(GameColor color) {
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                Piece p = squares[r][c];
                if (p != null && p.getType() == PieceType.KING && p.getColor() == color) {
                    return new Position(r, c);
                }
            }
        }
        return null;
    }

    public boolean isInCheck(GameColor color) {
        Position kingPos = findKing(color);
        if (kingPos == null) return false;
        return isSquareAttacked(kingPos, color.opposite());
    }

    public boolean isCheckmate(GameColor color) {
        return isInCheck(color) && getLegalMoves(color).isEmpty();
    }

    public boolean isStalemate(GameColor color) {
        return !isInCheck(color) && getLegalMoves(color).isEmpty();
    }

    public GameStatus getGameStatus() {
        if (isCheckmate(currentTurn)) {
            return currentTurn == GameColor.WHITE ? GameStatus.BLACK_WINS : GameStatus.WHITE_WINS;
        }

        if (isStalemate(currentTurn)) {
            return GameStatus.DRAW_STALEMATE;
        }

        if (halfMoveClock >= 100) {
            return GameStatus.DRAW_FIFTY_MOVE;
        }

        if (isThreefoldRepetition()) {
            return GameStatus.DRAW_THREEFOLD_REPETITION;
        }

        if (isInsufficientMaterial()) {
            return GameStatus.DRAW_INSUFFICIENT_MATERIAL;
        }

        return GameStatus.IN_PROGRESS;
    }

    public boolean isSquareAttacked(Position target, GameColor attacker) {
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                Piece p = squares[r][c];
                if (p != null && p.getColor() == attacker) {
                    if (canPieceAttack(new Position(r, c), target, p)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean canPieceAttack(Position from, Position target, Piece piece) {
        int dr = target.getRow() - from.getRow();
        int dc = target.getCol() - from.getCol();

        return switch (piece.getType()) {
            case PAWN -> {
                int direction = piece.getColor() == GameColor.WHITE ? -1 : 1;
                yield dr == direction && Math.abs(dc) == 1;
            }
            case KNIGHT -> (Math.abs(dr) == 2 && Math.abs(dc) == 1) || (Math.abs(dr) == 1 && Math.abs(dc) == 2);
            case KING -> Math.abs(dr) <= 1 && Math.abs(dc) <= 1 && (dr != 0 || dc != 0);
            case BISHOP -> Math.abs(dr) == Math.abs(dc) && dr != 0 && isPathClear(from, target);
            case ROOK -> (dr == 0 || dc == 0) && (dr != 0 || dc != 0) && isPathClear(from, target);
            case QUEEN -> ((Math.abs(dr) == Math.abs(dc) && dr != 0) || ((dr == 0 || dc == 0) && (dr != 0 || dc != 0)))
                    && isPathClear(from, target);
        };
    }

    private boolean isPathClear(Position from, Position to) {
        int dr = Integer.signum(to.getRow() - from.getRow());
        int dc = Integer.signum(to.getCol() - from.getCol());

        if (dr == 0 && dc == 0) return true;

        int rowDelta = Math.abs(to.getRow() - from.getRow());
        int colDelta = Math.abs(to.getCol() - from.getCol());
        if (rowDelta != colDelta && rowDelta != 0 && colDelta != 0) {
            return false;
        }

        int r = from.getRow() + dr;
        int c = from.getCol() + dc;
        while (r != to.getRow() || c != to.getCol()) {
            if (squares[r][c] != null) return false;
            r += dr;
            c += dc;
        }
        return true;
    }

    public boolean isThreefoldRepetition() {
        String currentKey = getPositionKey();
        int count = 0;
        for (String key : positionHistory) {
            if (key.equals(currentKey)) {
                count++;
            }
        }
        return count >= 3;
    }

    public boolean isInsufficientMaterial() {
        List<Piece> whitePieces = new ArrayList<>();
        List<Piece> blackPieces = new ArrayList<>();

        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                Piece p = squares[r][c];
                if (p != null) {
                    if (p.getColor() == GameColor.WHITE) {
                        whitePieces.add(p);
                    } else {
                        blackPieces.add(p);
                    }
                }
            }
        }

        long whiteNonKing = whitePieces.stream().filter(p -> p.getType() != PieceType.KING).count();
        long blackNonKing = blackPieces.stream().filter(p -> p.getType() != PieceType.KING).count();

        if (whiteNonKing == 0 && blackNonKing == 0) return true;

        if (whiteNonKing == 0 && blackNonKing == 1) {
            PieceType type = blackPieces.stream().filter(p -> p.getType() != PieceType.KING).findFirst().get().getType();
            if (type == PieceType.BISHOP || type == PieceType.KNIGHT) return true;
        }
        if (blackNonKing == 0 && whiteNonKing == 1) {
            PieceType type = whitePieces.stream().filter(p -> p.getType() != PieceType.KING).findFirst().get().getType();
            if (type == PieceType.BISHOP || type == PieceType.KNIGHT) return true;
        }

        if (whiteNonKing == 1 && blackNonKing == 1) {
            Piece whiteExtra = whitePieces.stream().filter(p -> p.getType() != PieceType.KING).findFirst().get();
            Piece blackExtra = blackPieces.stream().filter(p -> p.getType() != PieceType.KING).findFirst().get();
            if (whiteExtra.getType() == PieceType.BISHOP && blackExtra.getType() == PieceType.BISHOP) {
                Position whiteBishopPos = findPiece(whiteExtra);
                Position blackBishopPos = findPiece(blackExtra);
                if (whiteBishopPos != null && blackBishopPos != null) {
                    boolean whiteSquareColor = (whiteBishopPos.getRow() + whiteBishopPos.getCol()) % 2 == 0;
                    boolean blackSquareColor = (blackBishopPos.getRow() + blackBishopPos.getCol()) % 2 == 0;
                    if (whiteSquareColor == blackSquareColor) return true;
                }
            }
        }

        return false;
    }

    private Position findPiece(Piece target) {
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                Piece p = squares[r][c];
                if (p == target) return new Position(r, c);
            }
        }
        return null;
    }

    private boolean isOnBoard(Position pos) {
        return pos.getRow() >= 0 && pos.getRow() < 8 && pos.getCol() >= 0 && pos.getCol() < 8;
    }

    private String getPositionKey() {
        String fen = toFEN();
        String[] parts = fen.split(" ");
        return parts[0] + " " + parts[1] + " " + parts[2] + " " + parts[3];
    }

    @Override
    public Board clone() {
        return new Board(squares, currentTurn,
                whiteKingSideCastle, whiteQueenSideCastle,
                blackKingSideCastle, blackQueenSideCastle,
                enPassantTarget, halfMoveClock, fullMoveNumber,
                positionHistory);
    }

    public String toTextBoard() {
        StringBuilder sb = new StringBuilder();
        sb.append("  a b c d e f g h\n");
        for (int r = 0; r < 8; r++) {
            sb.append(8 - r).append(' ');
            for (int c = 0; c < 8; c++) {
                Piece p = squares[r][c];
                sb.append(p != null ? p.toFenChar() : '.');
                sb.append(' ');
            }
            sb.append(8 - r).append('\n');
        }
        sb.append("  a b c d e f g h\n");
        sb.append("Turn: ").append(currentTurn).append("\n");
        sb.append("FEN: ").append(toFEN());
        return sb.toString();
    }
}
