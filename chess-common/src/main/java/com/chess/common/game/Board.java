package com.chess.common.game;

import com.chess.common.model.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents the chess board and manages all game state.
 * <p>
 * Board coordinates:
 * row 0 = rank 8 (black's back rank)
 * row 7 = rank 1 (white's back rank)
 * col 0 = file a
 * col 7 = file h
 */
public class Board {

    private static final String INITIAL_FEN =
            "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";

    private final Piece[][] squares;
    private GameColor currentTurn;
    private boolean whiteKingSideCastle;  // K - white kingside (O-O)
    private boolean whiteQueenSideCastle; // Q - white queenside (O-O-O)
    private boolean blackKingSideCastle;  // k - black kingside
    private boolean blackQueenSideCastle; // q - black queenside
    private Position enPassantTarget;     // square that can be captured en passant, or null
    private int halfMoveClock;            // for 50-move rule
    private int fullMoveNumber;
    private final List<String> positionHistory; // for threefold repetition

    /**
     * Creates a new board in the standard starting position.
     */
    public Board() {
        this.squares = new Piece[8][8];
        this.positionHistory = new ArrayList<>();
        initializeStandardPosition();
    }

    /**
     * Creates a board from a FEN string.
     *
     * @param fen the FEN string
     */
    public Board(String fen) {
        this.squares = new Piece[8][8];
        this.positionHistory = new ArrayList<>();
        parseFEN(fen);
    }

    /**
     * Private copy constructor for cloning.
     */
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

    // ========================================================================
    // Initialization
    // ========================================================================

    private void initializeStandardPosition() {
        parseFEN(INITIAL_FEN);
    }

    // ========================================================================
    // FEN parsing and serialization
    // ========================================================================

    /**
     * Parses a FEN string and sets the board state.
     *
     * @param fen the FEN string
     */
    public final void parseFEN(String fen) {
        // Clear board
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                squares[r][c] = null;
            }
        }

        String[] parts = fen.split(" ");
        if (parts.length < 4) {
            throw new IllegalArgumentException("Invalid FEN: expected at least 4 parts, got " + parts.length);
        }

        // 1. Piece placement
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

        // 2. Active color
        currentTurn = parts[1].equals("w") ? GameColor.WHITE : GameColor.BLACK;

        // 3. Castling availability
        String castling = parts[2];
        whiteKingSideCastle = castling.contains("K");
        whiteQueenSideCastle = castling.contains("Q");
        blackKingSideCastle = castling.contains("k");
        blackQueenSideCastle = castling.contains("q");

        // 4. En passant target square
        if (parts[3].equals("-")) {
            enPassantTarget = null;
        } else {
            enPassantTarget = Position.fromAlgebraic(parts[3]);
        }

        // 5. Halfmove clock (optional)
        halfMoveClock = parts.length > 4 ? Integer.parseInt(parts[4]) : 0;

        // 6. Fullmove number (optional)
        fullMoveNumber = parts.length > 5 ? Integer.parseInt(parts[5]) : 1;

        positionHistory.clear();
        positionHistory.add(getPositionKey());
    }

    /**
     * Returns the FEN representation of the current board state.
     *
     * @return FEN string
     */
    public String toFEN() {
        StringBuilder sb = new StringBuilder();

        // 1. Piece placement
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

        // 2. Active color
        sb.append(' ').append(currentTurn.toFenChar());

        // 3. Castling availability
        sb.append(' ');
        StringBuilder castling = new StringBuilder();
        if (whiteKingSideCastle) castling.append('K');
        if (whiteQueenSideCastle) castling.append('Q');
        if (blackKingSideCastle) castling.append('k');
        if (blackQueenSideCastle) castling.append('q');
        sb.append(castling.length() > 0 ? castling : "-");

        // 4. En passant target
        sb.append(' ').append(enPassantTarget != null ? enPassantTarget.toAlgebraic() : "-");

        // 5. Halfmove clock
        sb.append(' ').append(halfMoveClock);

        // 6. Fullmove number
        sb.append(' ').append(fullMoveNumber);

        return sb.toString();
    }

    // ========================================================================
    // Board access
    // ========================================================================

    /**
     * Gets the piece at the given position.
     *
     * @param pos the position
     * @return the piece, or null if empty
     */
    public Piece getPiece(Position pos) {
        return squares[pos.getRow()][pos.getCol()];
    }

    /**
     * Gets the piece at the given row and column.
     */
    public Piece getPiece(int row, int col) {
        return squares[row][col];
    }

    /**
     * Sets a piece at the given position.
     */
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

    // ========================================================================
    // Move generation
    // ========================================================================

    /**
     * Generates all pseudo-legal moves for the given color.
     * Pseudo-legal means the move follows piece movement rules but may leave the king in check.
     *
     * @param color the color to generate moves for
     * @return list of pseudo-legal moves
     */
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

    /**
     * Generates all legal moves for the given color.
     * Legal moves are pseudo-legal moves that don't leave the king in check.
     *
     * @param color the color to generate moves for
     * @return list of legal moves
     */
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

    /**
     * Returns legal move destination positions for a piece at the given square.
     * Convenience method for GUI highlighting.
     *
     * @param row the row of the piece
     * @param col the column of the piece
     * @return list of positions the piece can legally move to
     */
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

    /**
     * Generates pseudo-legal moves for a specific piece.
     */
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

    // --- Pawn moves ---

    private void generatePawnMoves(Position from, Piece pawn, List<Move> moves) {
        int direction = pawn.getColor() == GameColor.WHITE ? -1 : 1; // white moves up (row decreases)
        int startRow = pawn.getColor() == GameColor.WHITE ? 6 : 1;
        int promotionRow = pawn.getColor() == GameColor.WHITE ? 0 : 7;

        // Single push
        Position oneForward = new Position(from.getRow() + direction, from.getCol());
        if (isOnBoard(oneForward) && getPiece(oneForward) == null) {
            if (oneForward.getRow() == promotionRow) {
                addPromotionMoves(from, oneForward, moves);
            } else {
                moves.add(new Move(from, oneForward));

                // Double push from starting position
                if (from.getRow() == startRow) {
                    Position twoForward = new Position(from.getRow() + 2 * direction, from.getCol());
                    if (getPiece(twoForward) == null) {
                        Move doublePush = new Move(from, twoForward);
                        moves.add(doublePush);
                    }
                }
            }
        }

        // Captures (including en passant)
        int[] captureCols = {from.getCol() - 1, from.getCol() + 1};
        for (int captureCol : captureCols) {
            int captureRow = from.getRow() + direction;
            if (captureRow < 0 || captureRow > 7 || captureCol < 0 || captureCol > 7) continue;
            Position captureTarget = new Position(captureRow, captureCol);

            Piece targetPiece = getPiece(captureTarget);
            if (targetPiece != null && targetPiece.getColor() != pawn.getColor()) {
                // Normal capture
                if (captureTarget.getRow() == promotionRow) {
                    addPromotionMoves(from, captureTarget, moves);
                } else {
                    moves.add(new Move(from, captureTarget));
                }
            }

            // En passant
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

    // --- Knight moves ---

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

    // --- Sliding moves (Bishop, Rook, Queen) ---

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
                        moves.add(new Move(from, to)); // capture
                    }
                    break; // blocked
                }
                r += dir[0];
                c += dir[1];
            }
        }
    }

    // --- King moves ---

    private void generateKingMoves(Position from, Piece king, List<Move> moves) {
        // Normal king moves
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

        // Castling
        generateCastlingMoves(from, king, moves);
    }

    private void generateCastlingMoves(Position kingPos, Piece king, List<Move> moves) {
        if (king.hasMoved()) return;

        int row = kingPos.getRow(); // should be row 0 for black, row 7 for white

        // Kingside castling (O-O)
        boolean ksc = king.getColor() == GameColor.WHITE ? whiteKingSideCastle : blackKingSideCastle;
        if (ksc) {
            // Check squares between king and rook are empty
            if (squares[row][5] == null && squares[row][6] == null) {
                // Check rook is present and hasn't moved
                Piece rook = squares[row][7];
                if (rook != null && rook.getType() == PieceType.ROOK && !rook.hasMoved()) {
                    // Check king doesn't pass through or end in check
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

        // Queenside castling (O-O-O)
        boolean qsc = king.getColor() == GameColor.WHITE ? whiteQueenSideCastle : blackQueenSideCastle;
        if (qsc) {
            // Check squares between king and rook are empty
            if (squares[row][1] == null && squares[row][2] == null && squares[row][3] == null) {
                Piece rook = squares[row][0];
                if (rook != null && rook.getType() == PieceType.ROOK && !rook.hasMoved()) {
                    // Check king doesn't pass through or end in check
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

    // ========================================================================
    // Move validation and execution
    // ========================================================================

    /**
     * Checks if a move is legal for the given color.
     *
     * @param move  the move to validate
     * @param color the color making the move
     * @return true if the move is legal
     */
    public boolean isLegalMove(Move move, GameColor color) {
        // Make the move on a copy of the board
        Board copy = this.clone();

        // Execute the move on the copy (without validation)
        copy.executeMove(move);

        // Check if the king is in check after the move
        return !copy.isInCheck(color);
    }

    /**
     * Validates and executes a move. Returns true if the move was valid and executed.
     *
     * @param move the move to make
     * @return true if the move was valid and executed
     */
    public boolean makeMove(Move move) {
        // Validate it's the right color's turn
        Piece piece = getPiece(move.getFrom());
        if (piece == null || piece.getColor() != currentTurn) {
            return false;
        }

        // Check if the move is in the list of legal moves
        List<Move> legalMoves = getLegalMoves(currentTurn);
        boolean isLegal = false;
        Move legalMove = null;
        for (Move legal : legalMoves) {
            if (legal.getFrom().equals(move.getFrom()) && legal.getTo().equals(move.getTo())) {
                // For promotion moves, also check promotion piece
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
            }
        }

        if (!isLegal) {
            return false;
        }

        // Detect capture before executing the move
        boolean isCapture = getPiece(legalMove.getTo()) != null || legalMove.isEnPassant();
        Piece movedPiece = getPiece(legalMove.getFrom());

        // Execute the move
        executeMove(legalMove);

        // Update game state
        updateCastlingRights(legalMove);
        updateEnPassantTarget(legalMove);
        updateMoveCounters(movedPiece, isCapture);

        // Switch turn
        currentTurn = currentTurn.opposite();
        fullMoveNumber += (currentTurn == GameColor.WHITE) ? 1 : 0;

        // Record position for threefold repetition
        positionHistory.add(getPositionKey());

        return true;
    }

    /**
     * Executes a move on the board without validation.
     * Used internally and for simulation.
     *
     * @param move the move to execute
     */
    public void executeMove(Move move) {
        Piece piece = getPiece(move.getFrom());
        if (piece == null) return;

        // Handle en passant capture
        if (move.isEnPassant()) {
            // Remove the captured pawn (it's on the same row as our pawn, same col as target)
            int capturedRow = move.getFrom().getRow();
            int capturedCol = move.getTo().getCol();
            squares[capturedRow][capturedCol] = null;
        }

        // Handle castling - move the rook
        if (move.isCastling()) {
            int row = move.getFrom().getRow();
            if (move.isKingsideCastling()) {
                // Move rook from h-file to f-file
                squares[row][5] = squares[row][7];
                squares[row][7] = null;
                if (squares[row][5] != null) squares[row][5].setHasMoved(true);
            } else {
                // Move rook from a-file to d-file
                squares[row][3] = squares[row][0];
                squares[row][0] = null;
                if (squares[row][3] != null) squares[row][3].setHasMoved(true);
            }
        }

        // Move the piece
        squares[move.getTo().getRow()][move.getTo().getCol()] = piece;
        squares[move.getFrom().getRow()][move.getFrom().getCol()] = null;
        piece.setHasMoved(true);

        // Handle promotion
        if (move.isPromotion() && piece.getType() == PieceType.PAWN) {
            squares[move.getTo().getRow()][move.getTo().getCol()] =
                    new Piece(move.getPromotionPiece(), piece.getColor(), true);
        }
    }

    private void updateCastlingRights(Move move) {
        Piece piece = getPiece(move.getTo()); // piece already moved to destination
        if (piece == null) return;

        // King moved - lose both castling rights
        if (piece.getType() == PieceType.KING) {
            if (piece.getColor() == GameColor.WHITE) {
                whiteKingSideCastle = false;
                whiteQueenSideCastle = false;
            } else {
                blackKingSideCastle = false;
                blackQueenSideCastle = false;
            }
        }

        // Rook moved or captured - lose that side's castling right
        if (piece.getType() == PieceType.ROOK) {
            if (piece.getColor() == GameColor.WHITE) {
                if (move.getFrom().equals(new Position(7, 0))) whiteQueenSideCastle = false;
                if (move.getFrom().equals(new Position(7, 7))) whiteKingSideCastle = false;
            } else {
                if (move.getFrom().equals(new Position(0, 0))) blackQueenSideCastle = false;
                if (move.getFrom().equals(new Position(0, 7))) blackKingSideCastle = false;
            }
        }

        // Rook captured - lose that side's castling right
        if (move.getFrom().equals(new Position(7, 0)) || move.getTo().equals(new Position(7, 0)))
            whiteQueenSideCastle = false;
        if (move.getFrom().equals(new Position(7, 7)) || move.getTo().equals(new Position(7, 7)))
            whiteKingSideCastle = false;
        if (move.getFrom().equals(new Position(0, 0)) || move.getTo().equals(new Position(0, 0)))
            blackQueenSideCastle = false;
        if (move.getFrom().equals(new Position(0, 7)) || move.getTo().equals(new Position(0, 7)))
            blackKingSideCastle = false;
    }

    private void updateEnPassantTarget(Move move) {
        Piece piece = getPiece(move.getTo());
        if (piece != null && piece.getType() == PieceType.PAWN) {
            int rowDiff = Math.abs(move.getTo().getRow() - move.getFrom().getRow());
            if (rowDiff == 2) {
                // Double pawn push - set en passant target
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
        // Reset half-move clock on pawn move or capture
        if (movedPiece != null && movedPiece.getType() == PieceType.PAWN) {
            halfMoveClock = 0;
        } else if (isCapture) {
            halfMoveClock = 0;
        } else {
            halfMoveClock++;
        }
    }

    // ========================================================================
    // Check, checkmate, stalemate detection
    // ========================================================================

    /**
     * Finds the king of the given color.
     *
     * @param color the king's color
     * @return the king's position, or null if not found
     */
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

    /**
     * Checks if the given color's king is in check.
     *
     * @param color the color to check
     * @return true if the king is in check
     */
    public boolean isInCheck(GameColor color) {
        Position kingPos = findKing(color);
        if (kingPos == null) return false;
        return isSquareAttacked(kingPos, color.opposite());
    }

    /**
     * Checks if the given color is in checkmate.
     *
     * @param color the color to check
     * @return true if checkmated
     */
    public boolean isCheckmate(GameColor color) {
        return isInCheck(color) && getLegalMoves(color).isEmpty();
    }

    /**
     * Checks if the given color is in stalemate.
     *
     * @param color the color to check
     * @return true if stalemated
     */
    public boolean isStalemate(GameColor color) {
        return !isInCheck(color) && getLegalMoves(color).isEmpty();
    }

    /**
     * Determines the game status for the current position.
     *
     * @return the current GameStatus
     */
    public GameStatus getGameStatus() {
        // Checkmate
        if (isCheckmate(currentTurn)) {
            return currentTurn == GameColor.WHITE ? GameStatus.BLACK_WINS : GameStatus.WHITE_WINS;
        }

        // Stalemate
        if (isStalemate(currentTurn)) {
            return GameStatus.DRAW_STALEMATE;
        }

        // 50-move rule
        if (halfMoveClock >= 100) { // 100 half-moves = 50 full moves
            return GameStatus.DRAW_FIFTY_MOVE;
        }

        // Threefold repetition
        if (isThreefoldRepetition()) {
            return GameStatus.DRAW_THREEFOLD_REPETITION;
        }

        // Insufficient material
        if (isInsufficientMaterial()) {
            return GameStatus.DRAW_INSUFFICIENT_MATERIAL;
        }

        return GameStatus.IN_PROGRESS;
    }

    /**
     * Checks if a square is attacked by any piece of the given color.
     *
     * @param target   the square to check
     * @param attacker the attacking color
     * @return true if the square is attacked
     */
    public boolean isSquareAttacked(Position target, GameColor attacker) {
        // Check all pieces of the attacker
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

    /**
     * Checks if a specific piece can attack a target square.
     */
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

    /**
     * Checks if the path between two positions is clear (for sliding pieces).
     */
    private boolean isPathClear(Position from, Position to) {
        int dr = Integer.signum(to.getRow() - from.getRow());
        int dc = Integer.signum(to.getCol() - from.getCol());
        int r = from.getRow() + dr;
        int c = from.getCol() + dc;
        while (r != to.getRow() || c != to.getCol()) {
            if (squares[r][c] != null) return false;
            r += dr;
            c += dc;
        }
        return true;
    }

    // ========================================================================
    // Draw detection helpers
    // ========================================================================

    /**
     * Checks for threefold repetition of the current position.
     */
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

    /**
     * Checks for insufficient material to deliver checkmate.
     */
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

        // Remove kings from count
        long whiteNonKing = whitePieces.stream().filter(p -> p.getType() != PieceType.KING).count();
        long blackNonKing = blackPieces.stream().filter(p -> p.getType() != PieceType.KING).count();

        // K vs K
        if (whiteNonKing == 0 && blackNonKing == 0) return true;

        // K+B vs K or K+N vs K
        if (whiteNonKing == 0 && blackNonKing == 1) {
            PieceType type = blackPieces.stream().filter(p -> p.getType() != PieceType.KING).findFirst().get().getType();
            if (type == PieceType.BISHOP || type == PieceType.KNIGHT) return true;
        }
        if (blackNonKing == 0 && whiteNonKing == 1) {
            PieceType type = whitePieces.stream().filter(p -> p.getType() != PieceType.KING).findFirst().get().getType();
            if (type == PieceType.BISHOP || type == PieceType.KNIGHT) return true;
        }

        // K+B vs K+B (same colored bishops)
        if (whiteNonKing == 1 && blackNonKing == 1) {
            Piece whiteExtra = whitePieces.stream().filter(p -> p.getType() != PieceType.KING).findFirst().get();
            Piece blackExtra = blackPieces.stream().filter(p -> p.getType() != PieceType.KING).findFirst().get();
            if (whiteExtra.getType() == PieceType.BISHOP && blackExtra.getType() == PieceType.BISHOP) {
                // Check if bishops are on the same color square
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

    // ========================================================================
    // Utility
    // ========================================================================

    private boolean isOnBoard(Position pos) {
        return pos.getRow() >= 0 && pos.getRow() < 8 && pos.getCol() >= 0 && pos.getCol() < 8;
    }

    /**
     * Generates a position key for repetition detection.
     * Includes piece positions, turn, castling rights, and en passant.
     */
    private String getPositionKey() {
        return toFEN();
    }

    /**
     * Creates a deep copy of this board.
     *
     * @return a cloned Board
     */
    @Override
    public Board clone() {
        return new Board(squares, currentTurn,
                whiteKingSideCastle, whiteQueenSideCastle,
                blackKingSideCastle, blackQueenSideCastle,
                enPassantTarget, halfMoveClock, fullMoveNumber,
                positionHistory);
    }

    /**
     * Prints the board as a text representation (for debugging).
     */
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
