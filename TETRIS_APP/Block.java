package com.example.myapplication;

import java.util.ArrayList;
import java.util.List;

public class Block {
    private List<Cell> cells; // List of cells that make up the block
    private int color;
    private int type;
    private Orientation orientation;

    private static final int NUM_ROWS = 20; // Number of rows in the game board
    private static final int NUM_COLS = 10; // Number of columns in the game board

    public Block(List<Cell> cells, int color, int type, Orientation orientation) {
        this.cells = cells;
        this.color = color;
        this.type = type;
        this.orientation = orientation;
        for (Cell cell : cells) {
            cell.setColor(Integer.toHexString(color)); // Set the color for each cell
        }
    }

    // Copy constructor
    public Block(Block other) {
        this.color = other.color;
        this.type = other.type;
        this.orientation = other.orientation;

        // Create a new list of cells
        this.cells = new ArrayList<>();

        // Copy each cell from the other block
        for (Cell cell : other.cells) {
            this.cells.add(new Cell(cell));
        }
    }

    public void setColor(int color) {
        this.color = color;
        for (Cell cell : cells) {
            cell.setColor(Integer.toHexString(color)); // Update the color for each cell
        }
    }

    public void setOrientation(Orientation orientation) {
        this.orientation = orientation;
    }

    // Getter for cells
    public List<Cell> getCells() {
        return cells;
    }

    public int getColor() {
        return color;
    }

    public int getType() {
        return type;
    }

    public Orientation getOrientation() {
        return orientation;
    }

    public void moveDown() {
        for (Cell cell : cells) {
            cell.moveDown(); // Move each cell of the block down
        }
    }

    public void moveLeft() {
        for (Cell cell : cells) {
            cell.moveLeft(); // Move each cell of the block left
        }
    }

    public void moveRight() {
        for (Cell cell : cells) {
            cell.moveRight(); // Move each cell of the block right
        }
    }

    // Method to check collision with another block
    public boolean isCollidingWith(Block block) {
        // Iterate through each cell of the current block
        for (Cell currentCell : cells) {
            // Iterate through each cell of the other block
            for (Cell otherCell : block.getCells()) {
                // Check if the cells are at the same position
                if (currentCell.getRow() + 1 == otherCell.getRow() && currentCell.getCol() == otherCell.getCol()) {
                    return true; // Collision detected
                }
            }
        }
        return false; // No collision detected
    }

    public int getMaxWidth() {
        int minCol = Integer.MAX_VALUE;
        int maxCol = Integer.MIN_VALUE;

        for (Cell cell : cells) {
            int col = cell.getCol();
            if (col < minCol) minCol = col;
            if (col > maxCol) maxCol = col;
        }

        return maxCol - minCol + 1;
    }

    public int getMaxHeight() {
        int minRow = Integer.MAX_VALUE;
        int maxRow = Integer.MIN_VALUE;

        for (Cell cell : cells) {
            int row = cell.getRow();
            if (row < minRow) minRow = row;
            if (row > maxRow) maxRow = row;
        }

        return maxRow - minRow + 1;
    }
}
