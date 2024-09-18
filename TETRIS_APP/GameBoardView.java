package com.example.myapplication;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.Toast;
import android.widget.TextView;
import okhttp3.WebSocket;



import java.util.ArrayList;
import java.util.List;

public class GameBoardView extends View {
    private static final int NUM_ROWS = 20; // Number of rows in the game board
    private static final int NUM_COLS = 10; // Number of columns in the game board
    private static final int CELL_SIZE = 50; // Size of each cell

    private Paint gridPaint;
    private Paint blockPaint;
    private List<Block> blocksOnBoard;
    private Block currentBlock;
    private Handler handler;
    private Runnable blockFallRunnable;
    private Block nextBlock;
    private int scoreCounter = 0; // Initialize the counter to zero
    private TextView scoreTextView; // Reference to the TextView that displays the score

    private WebSocket webSocket;

    public GameBoardView(Context context) {
        super(context);
        init();
    }

    public GameBoardView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public GameBoardView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        // Initialize paints
        gridPaint = new Paint();
        gridPaint.setColor(Color.BLACK);
        gridPaint.setStyle(Paint.Style.STROKE);
        gridPaint.setStrokeWidth(2);

        // Initialize the list of blocks
        blocksOnBoard = new ArrayList<>();
        handler = new Handler(Looper.getMainLooper());
        blockFallRunnable = new Runnable() {
            @Override
            public void run() {
                moveBlockDown();
                invalidate(); // Redraw the view
                if (!isGameOver()) {
                    handler.postDelayed(this, 500); // Schedule the next block fall
                } else {
                    // Handle game over state
                    gameOver();
                }
            }
        };
        startBlockFall();
    }
    public void setScoreTextView(TextView scoreTextView) {
        this.scoreTextView = scoreTextView;
        updateScoreDisplay(); // Ensure the initial score is displayed
    }

    // Notify the MainActivity about the game over
    private void notifyGameOver() {
        if (getContext() instanceof MainActivity) {
            ((MainActivity) getContext()).onGameOver();
        }
    }

    private void gameOver() {
        handler.removeCallbacks(blockFallRunnable);
        Toast.makeText(getContext(), "Game Over!", Toast.LENGTH_SHORT).show();

        // Send the GAMEOVER message along with the score to the ESP
        if (webSocket != null) {
            String gameOverMessage = "GAMEOVER," + scoreCounter;
            Log.d("WebSocket", "Sending message: " + gameOverMessage);
            webSocket.send(gameOverMessage);

            // Notify the MainActivity that the game is over
            notifyGameOver();
        } else {
            Log.e("WebSocket", "WebSocket is null. Cannot send GAMEOVER message.");
        }
    }





    public void setWebSocket(WebSocket webSocket) {
        this.webSocket = webSocket;
    }


    private boolean isGameOver() {
        if (currentBlock != null) {
            for (Cell cell : currentBlock.getCells()) {
                int row = cell.getRow();
                int col = cell.getCol();

                // Check if the cell is out of bounds (above the top row)
                if (row < 0) {
                    return true;
                }
                if (is_cell_full(cell)) {
                    return true;
                }
            }
            return false;
        }
        return true;
    }

    private void startBlockFall() {
        if (nextBlock == null) {
            nextBlock = BlockFactory.createRandomBlock(); // Prepare the next block
        }

        currentBlock = nextBlock;
        nextBlock = BlockFactory.createRandomBlock(); // Prepare the next block for after the current one is placed

        blockPaint = new Paint();
        blockPaint.setColor(currentBlock.getColor());
        blockPaint.setStyle(Paint.Style.FILL);

        handler.postDelayed(blockFallRunnable, 500); // Schedule the first block fall
    }

    private void moveBlockDown() {
        // Move the current block down by one row
        if (isBlockColliding() || isBlockAtBottom()) {
            blocksOnBoard.add(currentBlock);
            clearFullRows();

            currentBlock = nextBlock; // Set the current block to the next block
            nextBlock = BlockFactory.createRandomBlock(); // Generate a new next block

            invalidate();
        }
        currentBlock.moveDown();
    }

    private void clearFullRows() {
        ArrayList<Integer> rowsToDelete = new ArrayList<>();
        for (int row = 0; row < NUM_ROWS; row++) {
            if (isRowFull(row)) {
                rowsToDelete.add(row);
                scoreCounter++; // Increment the score counter each time a row is cleared
            }
        }
        for (Integer row : rowsToDelete) {
            clearRow(row);
            shiftRowsDown(row);
            invalidate(); // Redraw the view after each row is cleared
        }
        updateScoreDisplay(); // Update the score display after rows are cleared
    }

    private void updateScoreDisplay() {
        if (scoreTextView != null) {
            scoreTextView.setText("Score: " + scoreCounter);
        }
    }



    private void shiftRowsDown(int clearedRow) {
        for (Block block : blocksOnBoard) {
            for (Cell cell : block.getCells()) {
                if (cell.getRow() < clearedRow) {
                    // Shift down the cells above the cleared row
                    cell.moveDown();
                }
            }
        }
    }

    private void clearRow(int row) {
        // Remove cells from the specified row that belong to blocks
        List<Cell> cellsToRemove = new ArrayList<>();
        for (Block block : blocksOnBoard) {
            for (Cell cell : block.getCells()) {
                if (cell.getRow() == row) {
                    // Mark the cell for removal if it is in the specified row
                    cellsToRemove.add(cell);
                }
            }
        }
        for (Cell cell : cellsToRemove) {
            // Get the containing block of the cell and remove the cell from it
            Block containingBlock = cell.getContainingBlock(blocksOnBoard);
            if (containingBlock != null) {
                containingBlock.getCells().remove(cell);
            }
        }
    }

    private boolean isRowFull(int row) {
        for (int col = 0; col < NUM_COLS; col++) {
            Cell cell = new Cell(row, col, "000000"); // Provide a default color
            if (!is_cell_full(cell)) {
                return false;
            }
        }
        return true;
    }

    private boolean isBlockColliding() {
        // Check if the current block is colliding with other blocks on the board
        for (Block block : blocksOnBoard) {
            if (currentBlock.isCollidingWith(block)) {
                return true;
            }
        }
        return false;
    }

    private boolean isBlockAtBottom() {
        // Check if the current block has reached the bottom of the board
        for (Cell cell : currentBlock.getCells()) {
            if (cell.getRow() >= NUM_ROWS - 1) {
                return true;
            }
        }
        return false;
    }

    public void moveCurrentBlockLeft() {
        if (currentBlock != null) {
            // Check if the block can be moved to the left
            boolean canMoveLeft = true;
            for (Cell cell : currentBlock.getCells()) {
                if (cell.getCol() <= 0) {
                    canMoveLeft = false; // Cannot move left if any cell is at the left edge
                    break;
                }
            }
            boolean LeftFlag = checkLeftCells();

            // Move the block to the left if possible
            if (canMoveLeft && LeftFlag) {
                for (Cell cell : currentBlock.getCells()) {
                    cell.moveLeft(); // Move each cell of the block left
                }
                invalidate(); // Redraw the view
            }
        }
    }

    private boolean checkLeftCells() {
        List<Cell> my_cells = currentBlock.getCells();
        for (Cell cell : my_cells) {
            int left_col = cell.getCol() - 1;
            for (Block block : blocksOnBoard) {
                if (block != currentBlock) { // Make sure not to check against itself
                    for (Cell otherCell : block.getCells()) {
                        if (otherCell.getRow() == cell.getRow() && otherCell.getCol() == left_col) {
                            // Found an overlapping cell to the left of the current cell
                            return false;
                        }
                    }
                }
            }
        }
        return true;
    }

    public void moveCurrentBlockRight() {
        if (currentBlock != null) {
            // Check if the block can be moved to the right
            boolean canMoveRight = true;
            for (Cell cell : currentBlock.getCells()) {
                if (cell.getCol() >= NUM_COLS - 1) {
                    canMoveRight = false; // Cannot move right if any cell is at the right edge
                    break;
                }
            }
            boolean RightFlag = checkRightCells();

            // Move the block to the right if possible
            if (canMoveRight && RightFlag) {
                for (Cell cell : currentBlock.getCells()) {
                    cell.moveRight(); // Move each cell of the block right
                }
                invalidate(); // Redraw the view
            }
        }
    }

    private boolean checkRightCells() {
        List<Cell> my_cells = currentBlock.getCells();
        for (Cell cell : my_cells) {
            int right_col = cell.getCol() + 1;
            for (Block block : blocksOnBoard) {
                if (block != currentBlock) { // Make sure not to check against itself
                    for (Cell otherCell : block.getCells()) {
                        if (otherCell.getRow() == cell.getRow() && otherCell.getCol() == right_col) {
                            // Found an overlapping cell to the left of the current cell
                            return false;
                        }
                    }
                }
            }
        }
        return true;
    }

    public void rotateCurrentBlock() {
        if (currentBlock != null) {
            switch (currentBlock.getType()) {
                case 1:
                    break;
                case 2:
                    rotateType2();
                    break;
                case 3:
                    rotateType3();
                    break;
                case 4:
                    rotateType4();
                    break;
                default:
                    rotateType5();
                    break;
            }
            invalidate(); // Redraw the view after rotation
        }
    }

    private void rotateType5() {
        List<Cell> my_cells = currentBlock.getCells();
        boolean canRotate = true;
        if (currentBlock.getOrientation() == Orientation.UP) {
            if (my_cells.get(0).getRow() == 0) {
                canRotate = false;
            }
            int row0 = my_cells.get(0).getRow();
            int col0 = my_cells.get(0).getCol();
            Cell problem_cell = new Cell(row0 - 1, col0 - 1, "000000");
            Cell problem_cell1 = new Cell(row0, col0 - 1, "000000");
            if (is_cell_full(problem_cell) || is_cell_full(problem_cell1)) {
                canRotate = false;
            }
            if (canRotate) {
                my_cells.get(1).setRow(my_cells.get(1).getRow() + 1);
                my_cells.get(1).setCol(my_cells.get(1).getCol() - 1);
                my_cells.get(2).setRow(my_cells.get(2).getRow() - 2);
                my_cells.get(2).setCol(my_cells.get(2).getCol());
                my_cells.get(3).setRow(my_cells.get(3).getRow() - 1);
                my_cells.get(3).setCol(my_cells.get(3).getCol() - 1);
                currentBlock.setOrientation(Orientation.RIGHT);
            }
        } else if (currentBlock.getOrientation() == Orientation.RIGHT) {
            if (my_cells.get(0).getCol() == NUM_COLS - 1) {
                canRotate = false;
            }
            int row2 = my_cells.get(2).getRow();
            int col2 = my_cells.get(2).getCol();
            Cell problem_cell = new Cell(row2, col2 + 1, "000000");
            Cell problem_cell1 = new Cell(row2, col2 + 2, "000000");
            if (is_cell_full(problem_cell) || is_cell_full(problem_cell1)) {
                canRotate = false;
            }
            if (canRotate) {
                my_cells.get(1).setRow(my_cells.get(1).getRow() - 1);
                my_cells.get(1).setCol(my_cells.get(1).getCol() - 1);
                my_cells.get(2).setRow(my_cells.get(2).getRow());
                my_cells.get(2).setCol(my_cells.get(2).getCol() + 2);
                my_cells.get(3).setRow(my_cells.get(3).getRow() - 1);
                my_cells.get(3).setCol(my_cells.get(3).getCol() + 1);
                currentBlock.setOrientation(Orientation.DOWN);
            }

        } else if (currentBlock.getOrientation() == Orientation.DOWN) {
            if (my_cells.get(0).getRow() == NUM_ROWS - 1) {
                canRotate = false;
            }
            int row2 = my_cells.get(2).getRow();
            int col2 = my_cells.get(2).getCol();
            Cell problem_cell = new Cell(row2 + 1, col2, "000000");
            Cell problem_cell1 = new Cell(row2 + 2, col2, "000000");
            if (is_cell_full(problem_cell) || is_cell_full(problem_cell1)) {
                canRotate = false;
            }
            if (canRotate) {
                my_cells.get(1).setRow(my_cells.get(1).getRow() - 1);
                my_cells.get(1).setCol(my_cells.get(1).getCol() + 1);
                my_cells.get(2).setRow(my_cells.get(2).getRow() + 2);
                my_cells.get(2).setCol(my_cells.get(2).getCol());
                my_cells.get(3).setRow(my_cells.get(3).getRow() + 1);
                my_cells.get(3).setCol(my_cells.get(3).getCol() + 1);
                currentBlock.setOrientation(Orientation.LEFT);
            }

        } else {
            if (my_cells.get(0).getCol() == 0) {
                canRotate = false;
            }
            int row2 = my_cells.get(2).getRow();
            int col2 = my_cells.get(2).getCol();
            Cell problem_cell = new Cell(row2, col2 - 1, "000000");
            Cell problem_cell1 = new Cell(row2, col2 - 2, "000000");
            if (is_cell_full(problem_cell) || is_cell_full(problem_cell1)) {
                canRotate = false;
            }
            if (canRotate) {
                my_cells.get(1).setRow(my_cells.get(1).getRow() + 1);
                my_cells.get(1).setCol(my_cells.get(1).getCol() + 1);
                my_cells.get(2).setRow(my_cells.get(2).getRow());
                my_cells.get(2).setCol(my_cells.get(2).getCol() - 2);
                my_cells.get(3).setRow(my_cells.get(3).getRow() + 1);
                my_cells.get(3).setCol(my_cells.get(3).getCol() - 1);
                currentBlock.setOrientation(Orientation.UP);
            }

        }
    }

    private void rotateType4() {
        List<Cell> my_cells = currentBlock.getCells();
        boolean canRotate = true;
        if (currentBlock.getOrientation() == Orientation.UP) {
            for (Cell cell : my_cells) {
                if (cell.getCol() == 0) {
                    canRotate = false;
                    break;
                }
            }
            int row1 = my_cells.get(1).getRow();
            int row2 = my_cells.get(2).getRow();
            int col1 = my_cells.get(1).getCol();
            int col2 = my_cells.get(2).getCol();
            Cell problem_cell = new Cell(row1, col1 - 1, "000000");
            Cell problem_cell1 = new Cell(row1, col1 + 1, "000000");
            Cell problem_cell2 = new Cell(row2, col2 - 1, "000000");
            if (is_cell_full(problem_cell) || is_cell_full(problem_cell1) || is_cell_full(problem_cell2)) {
                canRotate = false;
            }
            if (canRotate) {
                my_cells.get(0).setRow(my_cells.get(0).getRow() + 1);
                my_cells.get(2).setRow(my_cells.get(2).getRow() - 1);
                my_cells.get(3).setRow(my_cells.get(3).getRow());
                my_cells.get(0).setCol(my_cells.get(0).getCol() + 1);
                my_cells.get(2).setCol(my_cells.get(2).getCol() - 1);
                my_cells.get(3).setCol(my_cells.get(3).getCol() - 2);
                currentBlock.setOrientation(Orientation.RIGHT);
            }
        } else if (currentBlock.getOrientation() == Orientation.RIGHT) {
            if (my_cells.get(1).getRow() == 0) {
                canRotate = false;
            }
            int row3 = my_cells.get(3).getRow();
            int row2 = my_cells.get(2).getRow();
            int col3 = my_cells.get(3).getCol();
            int col2 = my_cells.get(2).getCol();
            Cell problem_cell = new Cell(row3, col3 + 1, "000000");
            Cell problem_cell1 = new Cell(row2 - 1, col2, "000000");
            Cell problem_cell2 = new Cell(row2 - 1, col2 + 1, "000000");
            if (is_cell_full(problem_cell) || is_cell_full(problem_cell1) || is_cell_full(problem_cell2)) {
                canRotate = false;
            }
            if (canRotate) {
                my_cells.get(0).setRow(my_cells.get(0).getRow() + 1);
                my_cells.get(0).setCol(my_cells.get(0).getCol() - 1);
                my_cells.get(2).setRow(my_cells.get(2).getRow() - 1);
                my_cells.get(2).setCol(my_cells.get(2).getCol() + 1);
                my_cells.get(3).setRow(my_cells.get(3).getRow() - 2);
                my_cells.get(3).setCol(my_cells.get(3).getCol());
                currentBlock.setOrientation(Orientation.DOWN);
            }
        } else if (currentBlock.getOrientation() == Orientation.DOWN) {
            if (my_cells.get(1).getCol() == NUM_COLS - 1) {
                canRotate = false;
            }
            int row1 = my_cells.get(1).getRow();
            int row2 = my_cells.get(2).getRow();
            int col1 = my_cells.get(1).getCol();
            int col2 = my_cells.get(2).getCol();
            Cell problem_cell = new Cell(row1, col1 - 1, "000000");
            Cell problem_cell1 = new Cell(row2, col2 + 1, "000000");
            Cell problem_cell2 = new Cell(row1, col1 + 1, "000000");
            if (is_cell_full(problem_cell) || is_cell_full(problem_cell1) || is_cell_full(problem_cell2)) {
                canRotate = false;
            }
            if (canRotate) {
                my_cells.get(0).setRow(my_cells.get(0).getRow() - 1);
                my_cells.get(0).setCol(my_cells.get(0).getCol() - 1);
                my_cells.get(2).setRow(my_cells.get(2).getRow() + 1);
                my_cells.get(2).setCol(my_cells.get(2).getCol() + 1);
                my_cells.get(3).setRow(my_cells.get(3).getRow());
                my_cells.get(3).setCol(my_cells.get(3).getCol() + 2);
                currentBlock.setOrientation(Orientation.LEFT);
            }

        } else {
            if (my_cells.get(0).getRow() == NUM_ROWS - 1) {
                canRotate = false;
            }
            int row1 = my_cells.get(1).getRow();
            int row3 = my_cells.get(3).getRow();
            int col1 = my_cells.get(1).getCol();
            int col3 = my_cells.get(3).getCol();
            Cell problem_cell = new Cell(row3, col3 - 1, "000000");
            Cell problem_cell1 = new Cell(row1 + 1, col1, "000000");
            Cell problem_cell2 = new Cell(row1 + 1, col3, "000000");
            if (is_cell_full(problem_cell) || is_cell_full(problem_cell1) || is_cell_full(problem_cell2)) {
                canRotate = false;
            }
            if (canRotate) {
                my_cells.get(0).setRow(my_cells.get(0).getRow() - 1);
                my_cells.get(0).setCol(my_cells.get(0).getCol() + 1);
                my_cells.get(2).setRow(my_cells.get(2).getRow() + 1);
                my_cells.get(2).setCol(my_cells.get(2).getCol() - 1);
                my_cells.get(3).setRow(my_cells.get(3).getRow() + 2);
                my_cells.get(3).setCol(my_cells.get(3).getCol());
                currentBlock.setOrientation(Orientation.UP);
            }

        }
    }

    private void rotateType2() {
        // Check if we can rotate
        List<Cell> my_cells = currentBlock.getCells();
        boolean canRotate = true;
        if (currentBlock.getOrientation() == Orientation.DOWN) {
            System.out.println("in rotate 2");
            for (Cell cell : my_cells) {
                if (cell.getCol() >= NUM_COLS - 3) {
                    canRotate = false;
                    break;
                }
            }
            // Check if the cells full
            int col = my_cells.get(0).getCol();
            int row = my_cells.get(0).getRow();
            Cell problem_cell;
            for (int i = 1; i < 4; i++) {
                problem_cell = new Cell(row, col + i, "000000");
                if (is_cell_full(problem_cell)) {
                    canRotate = false;
                    break;
                }
            }
            if (canRotate) {
                int tmp = my_cells.get(0).getRow();
                my_cells.get(1).setRow(tmp);
                my_cells.get(2).setRow(tmp);
                my_cells.get(3).setRow(tmp);
                my_cells.get(1).setCol(my_cells.get(1).getCol() + 1);
                my_cells.get(2).setCol(my_cells.get(2).getCol() + 2);
                my_cells.get(3).setCol(my_cells.get(3).getCol() + 3);
                currentBlock.setOrientation(Orientation.UP);
            }

        } else {
            for (Cell cell : my_cells) {
                if (cell.getRow() >= NUM_ROWS - 3) {
                    canRotate = false;
                    break;
                }
            }
            // Check if the cells full
            int col = my_cells.get(0).getCol();
            int row = my_cells.get(0).getRow();
            Cell problem_cell;
            for (int i = 1; i < 4; i++) {
                problem_cell = new Cell(row + i, col, "000000");
                if (is_cell_full(problem_cell)) {
                    canRotate = false;
                    break;
                }

            }
            if (canRotate) {
                int tmp = my_cells.get(0).getCol();
                my_cells.get(1).setCol(tmp);
                my_cells.get(2).setCol(tmp);
                my_cells.get(3).setCol(tmp);
                my_cells.get(1).setRow(my_cells.get(1).getRow() + 1);
                my_cells.get(2).setRow(my_cells.get(2).getRow() + 2);
                my_cells.get(3).setRow(my_cells.get(3).getRow() + 3);
                currentBlock.setOrientation(Orientation.DOWN);
            }

        }
    }

    private boolean is_cell_full(Cell problem_cell) {
        if (problem_cell.getCol() >= NUM_COLS || problem_cell.getCol() < 0) {
            return true;
        }
        if (problem_cell.getRow() < 0 || problem_cell.getRow() >= NUM_ROWS) {
            return true;
        }
        // Check if the current block is colliding with other blocks on the board
        for (Block block : blocksOnBoard) {
            List<Cell> block_cells = block.getCells();
            for (Cell cell : block_cells) {
                if (cell.getCol() == problem_cell.getCol() && cell.getRow() == problem_cell.getRow()) {
                    return true;
                }
            }
        }
        return false;
    }

    private void rotateType3() {
        List<Cell> my_cells = currentBlock.getCells();
        int problem_col;
        int problem_row;
        Cell problem_cell;
        boolean cellFull;
        switch (currentBlock.getOrientation()) {

            case DOWN:
                // Rotate DOWN orientation to LEFT orientation
                // New positions:
                // [ ][X][ ]
                // [X][X][ ]
                // [ ][X][ ]
                boolean canRotateLeft = true;
                problem_col = my_cells.get(0).getCol() + 1;
                problem_row = my_cells.get(0).getRow() - 1;
                problem_cell = new Cell(problem_row, problem_col, "000000");
                cellFull = is_cell_full(problem_cell);
                if (cellFull) {
                    canRotateLeft = false;
                }

                for (Cell cell : my_cells) {
                    if (cell.getRow() == 0) {
                        canRotateLeft = false;
                        break;
                    }
                }
                if (canRotateLeft) {
                    my_cells.get(0).setCol(my_cells.get(0).getCol() + 1);
                    my_cells.get(0).setRow(my_cells.get(0).getRow() - 1);
                    my_cells.get(2).setCol(my_cells.get(2).getCol() - 1);
                    my_cells.get(2).setRow(my_cells.get(2).getRow() + 1);
                    my_cells.get(3).setCol(my_cells.get(3).getCol() - 1);
                    my_cells.get(3).setRow(my_cells.get(3).getRow() - 1);
                    // Update the block's orientation
                    currentBlock.setOrientation(Orientation.LEFT);
                }

                break;
            case UP:
                // Rotate UP orientation to RIGHT orientation
                // New positions:
                // [ ][X][ ]
                // [ ][X][X]
                // [ ][X][ ]
                boolean canRotateRight = true;
                problem_col = my_cells.get(0).getCol() - 1;
                problem_row = my_cells.get(0).getRow() + 1;
                problem_cell = new Cell(problem_row, problem_col, "000000");
                cellFull = is_cell_full(problem_cell);
                if (cellFull) {
                    canRotateRight = false;
                }
                for (Cell cell : my_cells) {
                    if (cell.getCol() >= NUM_COLS) {
                        canRotateRight = false; // Cannot move right if any cell is at the right edge
                        break;
                    }
                }
                if (canRotateRight) {
                    my_cells.get(0).setCol(my_cells.get(0).getCol() - 1);
                    my_cells.get(0).setRow(my_cells.get(0).getRow() + 1);
                    my_cells.get(2).setCol(my_cells.get(2).getCol() + 1);
                    my_cells.get(2).setRow(my_cells.get(2).getRow() - 1);
                    my_cells.get(3).setCol(my_cells.get(3).getCol() + 1);
                    my_cells.get(3).setRow(my_cells.get(3).getRow() + 1);
                    // Update the block's orientation
                    currentBlock.setOrientation(Orientation.RIGHT);
                }

                break;
            case LEFT:
                // Rotate LEFT orientation to UP orientation
                // New positions:
                // [ ][X ][ ]
                // [X][X][X]
                // [ ][ ][ ]
                boolean canRotateUp = true;
                problem_col = my_cells.get(0).getCol() + 1;
                problem_row = my_cells.get(0).getRow() + 1;
                problem_cell = new Cell(problem_row, problem_col, "000000");
                cellFull = is_cell_full(problem_cell);
                if (cellFull) {
                    canRotateUp = false;
                }
                for (Cell cell : my_cells) {
                    if (cell.getCol() >= NUM_COLS - 1) {
                        canRotateUp = false; // Cannot move right if any cell is at the right edge
                        break;
                    }
                }
                if (canRotateUp) {
                    my_cells.get(0).setRow(my_cells.get(0).getRow() + 1);
                    my_cells.get(0).setCol(my_cells.get(0).getCol() + 1);
                    my_cells.get(2).setCol(my_cells.get(2).getCol() - 1);
                    my_cells.get(2).setRow(my_cells.get(2).getRow() - 1);
                    my_cells.get(3).setCol(my_cells.get(3).getCol() + 1);
                    my_cells.get(3).setRow(my_cells.get(3).getRow() - 1);
                    // Update the block's orientation
                    currentBlock.setOrientation(Orientation.UP);
                }

                break;
            case RIGHT:
                // Rotate RIGHT orientation to DOWN orientation
                // New positions:
                // [ ][ ][ ]
                // [ X][X][X]
                // [ ][X][ ]
                boolean canRotateDown = true;
                problem_col = my_cells.get(0).getCol() - 1;
                problem_row = my_cells.get(0).getRow() - 1;
                problem_cell = new Cell(problem_row, problem_col, "000000");
                cellFull = is_cell_full(problem_cell);
                if (cellFull) {
                    canRotateDown = false;
                }
                for (Cell cell : my_cells) {
                    if (cell.getCol() <= 0) {
                        canRotateDown = false; // Cannot move right if any cell is at the right edge
                        break;
                    }
                }
                if (canRotateDown) {
                    my_cells.get(0).setCol(my_cells.get(0).getCol() - 1);
                    my_cells.get(0).setRow(my_cells.get(0).getRow() - 1);
                    my_cells.get(2).setCol(my_cells.get(2).getCol() + 1);
                    my_cells.get(2).setRow(my_cells.get(2).getRow() + 1);
                    my_cells.get(3).setCol(my_cells.get(3).getCol() - 1);
                    my_cells.get(3).setRow(my_cells.get(3).getRow() + 1);
                    // Update the block's orientation
                    currentBlock.setOrientation(Orientation.DOWN);
                }

                break;
            default:
                break;
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        // Draw the game board grid
        drawGameBoard(canvas);
        // Draw the Tetris blocks
        drawBlocks(canvas);
        drawPreviewBox(canvas);
        // Draw the preview of the next block
        drawNextBlockPreview(canvas);

    }

    private void drawNextBlockPreview(Canvas canvas) {
        if (nextBlock == null)
            return;

        // Define the preview area dimensions
        int previewBoxLeft = getWidth() - (CELL_SIZE * 5);
        int previewBoxTop = 20;
        int previewBoxSize = CELL_SIZE * 4; // the largest block fits in a 4x4 grid

        // Find the minimum column and row for the block to normalize its position
        int minCol = Integer.MAX_VALUE;
        int minRow = Integer.MAX_VALUE;
        for (Cell cell : nextBlock.getCells()) {
            if (cell.getCol() < minCol) {
                minCol = cell.getCol();
            }
            if (cell.getRow() < minRow) {
                minRow = cell.getRow();
            }
        }

        // Calculate the offset to center the block in the preview box
        int maxBlockWidth = nextBlock.getMaxWidth() * CELL_SIZE;
        int maxBlockHeight = nextBlock.getMaxHeight() * CELL_SIZE;
        int offsetX = (previewBoxSize - maxBlockWidth) / 2;
        int offsetY = (previewBoxSize - maxBlockHeight) / 2;

        // Draw the block cells centered in the preview area
        for (Cell cell : nextBlock.getCells()) {
            int normalizedCol = cell.getCol() - minCol;
            int normalizedRow = cell.getRow() - minRow;
            int cellLeft = previewBoxLeft + offsetX + (normalizedCol * CELL_SIZE);
            int cellTop = previewBoxTop + offsetY + (normalizedRow * CELL_SIZE);
            int cellRight = cellLeft + CELL_SIZE;
            int cellBottom = cellTop + CELL_SIZE;

            blockPaint.setColor(nextBlock.getColor());
            canvas.drawRect(cellLeft, cellTop, cellRight, cellBottom, blockPaint);
        }
    }

    private void drawPreviewBox(Canvas canvas) {
        int previewBoxLeft = getWidth() - (CELL_SIZE * 5); // preview box is to the right
        int previewBoxTop = 20; // Top margin
        int previewBoxWidth = CELL_SIZE * 4; // preview box can fit the largest block
        int previewBoxHeight = CELL_SIZE * 4; // Same as width for square area

        Paint previewBoxPaint = new Paint();
        previewBoxPaint.setColor(Color.WHITE); // Set the border color (e.g., white)
        previewBoxPaint.setStyle(Paint.Style.STROKE); // Set style to only draw the border
        previewBoxPaint.setStrokeWidth(5); // Set the border width (adjust as needed)

        // Draw the background of the preview box
        canvas.drawRect(previewBoxLeft, previewBoxTop, previewBoxLeft + previewBoxWidth, previewBoxTop + previewBoxHeight, previewBoxPaint);

        // Optionally draw a border around the preview box
        previewBoxPaint.setColor(Color.BLACK);
        previewBoxPaint.setStyle(Paint.Style.STROKE);
        canvas.drawRect(previewBoxLeft, previewBoxTop, previewBoxLeft + previewBoxWidth, previewBoxTop + previewBoxHeight, previewBoxPaint);
    }

    private void drawGameBoard(Canvas canvas) {
        for (int row = 0; row < NUM_ROWS; row++) {
            for (int col = 0; col < NUM_COLS; col++) {
                int left = col * CELL_SIZE;
                int top = row * CELL_SIZE;
                int right = left + CELL_SIZE;
                int bottom = top + CELL_SIZE;
                canvas.drawRect(left, top, right, bottom, gridPaint);
            }
        }
    }

    private void drawBlocks(Canvas canvas) {
        // Draw blocks on the board
        for (Block block : blocksOnBoard) {
            for (Cell cell : block.getCells()) {
                int left = cell.getCol() * CELL_SIZE;
                int top = cell.getRow() * CELL_SIZE;
                int right = left + CELL_SIZE;
                int bottom = top + CELL_SIZE;
                blockPaint.setColor(block.getColor());
                canvas.drawRect(left, top, right, bottom, blockPaint);
            }
        }
        // Draw current block
        if (currentBlock != null) {
            for (Cell cell : currentBlock.getCells()) {
                int left = cell.getCol() * CELL_SIZE;
                int top = cell.getRow() * CELL_SIZE;
                int right = left + CELL_SIZE;
                int bottom = top + CELL_SIZE;
                blockPaint.setColor(currentBlock.getColor());
                canvas.drawRect(left, top, right, bottom, blockPaint);
            }
        }
    }

    public void dropCurrentBlock() {
        if (currentBlock != null) {
            while (!isBlockAtBottom() && !isBlockColliding()) {
                currentBlock.moveDown(); // Move the block down by one row
            }

            // Add the block to the list of blocks on the board since it's now landed
            blocksOnBoard.add(currentBlock);
            clearFullRows(); // Check and clear any full rows

            // Prepare the next block
            currentBlock = nextBlock;
            nextBlock = BlockFactory.createRandomBlock();

            invalidate(); // Redraw the view to show the block in its new position

        }
    }

    public GameBoardState getGameBoardState() {
        GameBoardState boardState = new GameBoardState();

        // Add all blocks currently on the board to the game board state
        for (Block block : blocksOnBoard) {
            boardState.addBlock(block);
        }

        // If there is a currentBlock (the block currently being moved), add it to the game board state
        if (currentBlock != null) {
            boardState.addBlock(currentBlock);
        }

        return boardState;
    }
}
