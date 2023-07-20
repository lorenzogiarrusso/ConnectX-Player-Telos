package connectx.Telos;

import connectx.CXPlayer;
import connectx.Telos.Pair;
import connectx.CXBoard;
import connectx.CXCell;
import connectx.CXCellState;
import connectx.CXGameState;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Telos implements CXPlayer {
    private Random rand;
    private int M, N, K; // Number of rows, columns and pieces to align in order to win
    private CXGameState myWin, yourWin; // Game states representing this player's victory and the opponent's victory
    private CXCellState myPiece, yourPiece; // Cell states representing this player's piece and the opponent's piece
    private int TIMEOUT; // Variable that holds the time limit in seconds
    private long START; // Variable that holds the starting time in milliseconds
    private int bestMove;
    private boolean timeout_reached;


    public Telos() {
    }

    /**
     * Player initialization.
     * Cost: O(1)
     */
    public void initPlayer(int M, int N, int K, boolean first, int timeout_in_secs) {
        this.rand = new Random(System.currentTimeMillis());
        this.M = M;
        this.N = N;
        this.K = K;
        this.myWin = first ? CXGameState.WINP1 : CXGameState.WINP2;
        this.yourWin = first ? CXGameState.WINP2 : CXGameState.WINP1;
        this.myPiece = first ? CXCellState.P1 : CXCellState.P2;
        this.yourPiece = first ? CXCellState.P2 : CXCellState.P1;
        this.TIMEOUT = timeout_in_secs;
    }

    /**
     * Picks the best next move, represented by the number of the column where the piece should be dropped.
     * Cost: O(M N^d K)
    */
    public int selectColumn(CXBoard B) {
        timeout_reached = false;
        START = System.currentTimeMillis(); // Save starting time

        if (B.numOfMarkedCells() <= 1) return N / 2; // The best first move is always the middle column

        iterativeDeepening(B, 8);
        return this.bestMove;
    }

    /**
     * Returns player's name.
     * Cost: O(1)
     */
    public String playerName() {
        return "Telos";
    }

    /**
     * Minimax with alphabeta pruning. Root node picks the best move by editing the member variable bestMove.
     * @return Score of the node.
     * Cost: O(M N^d K)
     */
    private Long alphabeta(CXBoard B, Long current_eval, int depthCurrent, int depthMax, Long alpha, Long beta, boolean maximizing) {

        //Terminal situation or max depth reached
        if (depthCurrent == depthMax || isLeaf(B))
            return eval(B, depthCurrent);

        if (maximizing) {
            Long val = Long.MIN_VALUE + 1;
            Integer[] L = B.getAvailableColumns();
            List<Pair<Integer, Long>> orderedMoves = new ArrayList<>(); //Pair => (Move, Score)

            //Move ordering (decreasing order)
            for (int i : L) {
                Long childHeuristic = update_eval(B, depthCurrent, current_eval, i);
                orderedMoves.add(new Pair<>(i, childHeuristic));
            }
            orderedMoves.sort((a, b) -> b.second.compareTo(a.second));

            for (Pair<Integer, Long> j : orderedMoves) {
                Integer i = j.first;

                if (timeIsRunningOut()) {
                    this.timeout_reached = true;
                    return val;
                }

                B.markColumn(i);
                Long childVal = alphabeta(B, j.second, depthCurrent+1, depthMax, alpha, beta, false);
                B.unmarkColumn();

                if (childVal > val) { //Maximize
                    val = childVal;
                    if (depthCurrent == 0) //If true we are in the root node, so we should update the bestMove variable.
                        bestMove = i;
                }

                alpha = Math.max(alpha, val);
                if (beta <= alpha) //Pruning
                    break;
            }
            return val;
        }

        else {
            Long val = Long.MAX_VALUE - 1;
            Integer[] L = B.getAvailableColumns();
            List<Pair<Integer, Long>> orderedMoves = new ArrayList<>();

            //Move ordering (increasing order)
            for (int i : L) {
                Long childEval = update_eval(B, depthCurrent, current_eval, i);
                orderedMoves.add(new Pair<>(i, childEval));
            }
            orderedMoves.sort((a, b) -> -b.second.compareTo(a.second));

            for (Pair<Integer, Long> j : orderedMoves) {
                int i = j.first;

                if (timeIsRunningOut()) {
                    this.timeout_reached = true;
                    return val;
                }

                B.markColumn(i);
                val = Math.min(val, alphabeta(B, j.second, depthCurrent+1, depthMax, alpha, beta, true)); //Minimize
                B.unmarkColumn();

                beta = Math.min(beta, val); 
                if (beta <= alpha) //Pruning
                    break;
            }
            return val;
        }
    }

    /**
     * Returns true if the elapsed time is close to the timeout limit.
     * Cost: O(1)
     */
    private boolean timeIsRunningOut() {
        return ((System.currentTimeMillis() - START) / 1000.0 >= TIMEOUT * (95.0 / 100.0));
    }

    /**
     * Heuristic evaluation of the given board.
     * Cost: O(MNK)
     */
    private Long eval(CXBoard B, int depth) {

        if (B.gameState() != CXGameState.OPEN) {
            if (B.gameState() == myWin)
                return Long.MAX_VALUE - 1 - depth;
            else if (B.gameState() == yourWin)
                return Long.MIN_VALUE + 1 + depth;
            else
                return 0L;
        }

        CXCellState[][] board = B.getBoard(); // Matrix representing the cells of the game's board
        CXCellState[] row_array = new CXCellState[N]; // Array that contains a row of cells
        CXCellState[] col_array = new CXCellState[M]; // Array that contains a column of cells
        CXCellState[] subdiag_array = new CXCellState[K]; // Array that contains a set of diagonally aligned cells
        boolean[] isEmptyRow = new boolean[M];
        Long score = 0L;

        for(int i = 0; i < M; i++) isEmptyRow[i] = true;

        // Vertical alignments
        for (int i = 0; i < N; i++) { // For each column
            for (int d = 0; d < M; d++){
                col_array[d] = board[d][i]; // col_array now contains the current column
                if(col_array[d] != CXCellState.FREE) isEmptyRow[d] = false; //If board[d][i] contains a piece, row d is not empty
            }
            if(col_array[M-1] == CXCellState.FREE) continue; // If the lowest cell is empty the column is empty, so we can ignore it
            for (int r = 0; r < M - (K - 1); r++) { // Check each subsequence of K elements starting from the r-th
                                                    // position
                if(col_array[r+K-1] == CXCellState.FREE) continue; // If the lowest cell of the subcolumn is empty the subcolumn is empty, so we can ignore it
                score += eval_sub(col_array, r, r + K); // Increment the current score by the score assigned to the
                                                        // current subsequence
            }
        }

        // Horizontal alignments
        for (int i = 0; i < M; i++) { // For each row
            if(isEmptyRow[i] == true) continue; // If row is empty, we can ignore it
            row_array = board[i]; // row_array now contains the current row
            for (int c = 0; c < N - (K - 1); c++) { // Check each subsequence of K elements starting from the c-th
                                                    // position
                score += eval_sub(row_array, c, c + K); // Increment the current score by the score assigned to the
                                                        // current subsequence
            }
        }

        // Descending diagonals
        for (int r = 0; r < M - (K - 1); r++) {
            for (int c = 0; c < N - (K - 1); c++) { // Exclude cells in the top-right and bottom-left corners, because
                                                    // they belong to diagonals with less than K elements
                for (int i = 0; i < K; i++)
                    subdiag_array[i] = board[r + i][c + i]; // subdiag_array now contains the elements of the current
                                                            // subdiagonal of K elements

                score += eval_sub(subdiag_array, 0, K); // Increment the current score by the score assigned to the
                                                        // current subsequence
            }
        }

        // Ascending diagonals
        // Similar to above, but excludes cells in the top-left and bottom-right corners
        for (int r = 0; r < M - (K - 1); r++) {
            for (int c = 0; c < N - (K - 1); c++) {
                for (int i = 0; i < K; i++)
                    subdiag_array[i] = board[r + (K - 1) - i][c + i];
                score += eval_sub(subdiag_array, 0, K);
            }
        }

        return score;
    }

    /**
     * Function returning the scorre assigned to the subset of cells specified in the arguments.
     * Cost: θ(n), with n = end index - start index (assuming Math.pow() is O(1))
     */
    private Long eval_sub(CXCellState arr[], int start, int end) {
        int count_mine = 0, count_empty = 0, count_yours = 0; //Counters for my pieces, empty spaces and opponent's pieces
        Long score = 0L;

        for (int i = start; i < end; i++) {

            if (arr[i] == myPiece)
                count_mine++;

            else if (arr[i] == CXCellState.FREE)
                count_empty++;

            else
                count_yours++;
        }

        score += (count_mine - count_yours);

        if (count_yours == 0)
            score *= (long) Math.pow(2, count_mine);
        if (count_mine == 0)
            score *= (long) Math.pow(2, count_yours);

        return score;
    }


    /**
     * Given the board B with its heuristic evaluation "lastEval", updates the latter considering the insertion of a piece in the "col" column.
     * @return: heuristic evaluation for the new state of the board.
     * Cost: θ(K^2)
     */
    private Long update_eval(CXBoard B, int depth, Long lastEval, int col){
        B.markColumn(col);
        if(isLeaf(B)){
            Long score = eval(B, depth+1); //If it's a terminal game state, let eval() calculate the evaluation in O(1) time
            B.unmarkColumn();
            return score;
        }
        CXCell newMove = B.getLastMove();
        int row = newMove.i;
        B.unmarkColumn();


        CXCellState[][] board = B.getBoard();
        int verticalStart = Math.max(row - (K-1), 0);
        int verticalEnd = Math.min(row + (K-1), M-1);
        int horizontalStart = Math.max(col - (K-1), 0);
        int horizontalEnd = Math.min(col + (K-1), N-1);

        CXCellState[] current_sub = new CXCellState[K];
    
        /* 
         * Part 1: Removing the contributions of the obsolete subsequences from the evaluation 
         */

        //Vertical subsequences
        for(int i = verticalStart; i+K-1 <= verticalEnd; i++){
            for(int r = i; r < i+K; r++) current_sub[r-i] = board[i][col];
            lastEval -= eval_sub(current_sub, 0, K);
        }

        //Horizontal subsequences
        for(int j = horizontalStart; j+K-1 <= horizontalEnd; j++){
            current_sub = Arrays.copyOfRange(board[row], j, j+K);
            lastEval -= eval_sub(current_sub, 0, K);
        }

        //Diagonal subsequences
        for(int i = verticalStart, j = horizontalStart; i+K-1 <= verticalEnd && j+K-1 <= horizontalEnd; i++, j++){
            for(int c = 0; c < K; c++) current_sub[c] = board[i+c][j+c];
            lastEval -= eval_sub(current_sub, 0, K);
        }

        //Antidiagonal subsequences
        for(int i = verticalEnd, j = horizontalStart; i-K+1 >= verticalStart && j+K-1 <= horizontalEnd; i--, j++){
            for(int c = 0; c < K; c++) current_sub[c] = board[i-c][j+c];
            lastEval -= eval_sub(current_sub, 0, K);
        }

        /*
         * Part 2: insert piece and add the contributions of the cell's subsequences 
        */

        B.markColumn(col);

        //Vertical subsequences
        for(int i = verticalStart; i+K-1 <= verticalEnd; i++){
            for(int r = i; r < i+K; r++) current_sub[r-i] = board[i][col];
            lastEval += eval_sub(current_sub, 0, K);
        }

        //Horizontal subsequences
        for(int j = horizontalStart; j+K-1 <= horizontalEnd; j++){
            current_sub = Arrays.copyOfRange(board[row], j, j+K);
            lastEval += eval_sub(current_sub, 0, K);
        }

        //Diagonal subsequences
        for(int i = verticalStart, j = horizontalStart; i+K-1 <= verticalEnd && j+K-1 <= horizontalEnd; i++, j++){
            for(int c = 0; c < K; c++) current_sub[c] = board[i+c][j+c];
            lastEval += eval_sub(current_sub, 0, K);
        }

        //Antidiagonal subsequences
        for(int i = verticalEnd, j = horizontalStart; i-K+1 >= verticalStart && j+K-1 <= horizontalEnd; i--, j++){
            for(int c = 0; c < K; c++) current_sub[c] = board[i-c][j+c];
            lastEval += eval_sub(current_sub, 0, K);
        }

        B.unmarkColumn();

        return lastEval;
    }

    /**
     * Returns true if the given Board is relative to a finished game.
     * Cost: O(1)
     */
    private boolean isLeaf(CXBoard B) {
        return (B.gameState() != CXGameState.OPEN) || (B.numOfFreeCells() == 0);
    }

    /**
     * Iterative deepening that considers the timeout limit.
     * Cost: O(M N^d K)
     */
    private void iterativeDeepening(CXBoard B, int maxDepth) {
        Long alpha = Long.MIN_VALUE +1;
        Long beta = Long.MAX_VALUE - 1;
        int prev;
        this.bestMove = -1;
        Long currentEval = eval(B, 0);

        for(int d = 1; d <= maxDepth; d++){
            if(timeout_reached) break;
            prev = this.bestMove; //Save the best move we found considering the previous maximum depth
            alphabeta(B, currentEval, 0, d, alpha, beta, true);
            if(timeout_reached) this.bestMove = prev; //If we timed out with depth = d we couldn't establish a reliable best move, so we use the previous one
        }
    }
}
