package com.example.myapplication;

import java.util.ArrayList;
import java.util.List;


public class GameBoardState {
    private static final int NUM_ROWS = 20; // Number of rows in the game board
    private static final int NUM_COLS = 10; // Number of columns in the game board

    private List<Block> blocks;


    public GameBoardState() {
        blocks = new ArrayList<>();
    }

    public void addBlock(Block block) {
        // Add the block to the list of blocks
        blocks.add(block);
    }

    public List<Block> getBlocks() {
        return blocks;
    }






}



