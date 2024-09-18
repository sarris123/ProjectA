package com.example.myapplication;

import java.util.List;
import java.util.Objects;

public class Cell {
    private int row;
    private int col;
    private String color; // Store the color of the cell

    public Cell(int row, int col, String color) {
        this.row = row;
        this.col = col;
        this.color = color; // Initialize the color
    }

    public Cell(Cell other) {
        this.row = other.row;
        this.col = other.col;
        this.color = other.color;
    }

    public int getRow() {
        return row;
    }

    public void setRow(int row) {
        this.row = row;
    }

    public int getCol() {
        return col;
    }

    public void setCol(int col) {
        this.col = col;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public void moveDown() {
        this.row += 1;
    }

    public void moveLeft() {
        this.col -= 1;
    }

    public void moveRight() {
        this.col += 1;
    }

    public Block getContainingBlock(List<Block> blocks) {
        for (Block block : blocks) {
            if (block.getCells().contains(this)) {
                return block;
            }
        }
        return null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Cell cell = (Cell) o;
        return row == cell.row && col == cell.col;
    }

    @Override
    public int hashCode() {
        return Objects.hash(row, col);
    }
}
