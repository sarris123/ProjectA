package com.example.myapplication;

public class CellChange {
    private int row;
    private int col;
    private boolean newValue;

    public CellChange(int row, int col, boolean newValue) {
        this.row = row;
        this.col = col;
        this.newValue = newValue;
    }

    public int getRow() {
        return row;
    }

    public int getCol() {
        return col;
    }

    public boolean getNewValue() {
        return newValue;
    }
}