package com.example.myapplication;

import android.graphics.Color;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class BlockFactory {
    private static final int NUM_ROWS = 20; // Number of rows in the game board
    private static final int NUM_COLS = 10; // Number of columns in the game board

    // Define different types of blocks
    public static Block createBlockType1() {
        List<Cell> cells = new ArrayList<>();
        int color = getRandomColor();
        String colorHex = Integer.toHexString(color);
        // Randomly select the starting column position for the block
        int startCol = new Random().nextInt(NUM_COLS - 1); // Ensure the entire block fits within the board

        // Add cells to the block with randomized column positions while maintaining the shape
        cells.add(new Cell(0, startCol, colorHex));
        cells.add(new Cell(0, startCol + 1, colorHex));
        cells.add(new Cell(1, startCol, colorHex));
        cells.add(new Cell(1, startCol + 1, colorHex));

        return new Block(cells, color, 1, Orientation.NONE);
    }

    public static Block createBlockType2() {
        List<Cell> cells = new ArrayList<>();
        int color = getRandomColor();
        String colorHex = Integer.toHexString(color);
        // Randomly select the starting column position for the block
        int startCol = new Random().nextInt(NUM_COLS - 1); // Ensure the entire block fits within the board

        // Add cells to the block with randomized column positions while maintaining the shape
        for (int i = 0; i < 4; i++) {
            cells.add(new Cell(i, startCol, colorHex));
        }

        return new Block(cells, color, 2, Orientation.DOWN);
    }

    public static Block createBlockType3() {
        List<Cell> cells = new ArrayList<>();
        int color = getRandomColor();
        String colorHex = Integer.toHexString(color);

        // Randomly select the starting column position for the block
        int startCol = new Random().nextInt(NUM_COLS - 2); // Ensure the entire block fits within the board

        // Add cells to the block with randomized column positions while maintaining the shape
        cells.add(new Cell(0, startCol, colorHex));
        cells.add(new Cell(0, startCol + 1, colorHex));
        cells.add(new Cell(0, startCol + 2, colorHex));
        cells.add(new Cell(1, startCol + 1, colorHex));

        return new Block(cells, color, 3, Orientation.DOWN);
    }

    public static Block createBlockType4() {
        List<Cell> cells = new ArrayList<>();
        int color = getRandomColor();
        String colorHex = Integer.toHexString(color);

        // Randomly select the starting column position for the block
        int startCol = new Random().nextInt(NUM_COLS - 1); // Ensure the block fits within the board

        // Add cells to the block with randomized column positions while maintaining the shape
        cells.add(new Cell(0, startCol, colorHex));
        cells.add(new Cell(1, startCol, colorHex));
        cells.add(new Cell(2, startCol, colorHex));
        cells.add(new Cell(2, startCol + 1, colorHex));

        return new Block(cells, color, 4, Orientation.UP);
    }

    public static Block createBlockType5() {
        List<Cell> cells = new ArrayList<>();
        int color = getRandomColor();
        String colorHex = Integer.toHexString(color);

        // Randomly select the starting column position for the block
        int startCol = new Random().nextInt(NUM_COLS - 2) + 1; // Ensure the block fits within the board

        // Add cells to the block with randomized column positions while maintaining the shape
        cells.add(new Cell(0, startCol, colorHex));
        cells.add(new Cell(0, startCol + 1, colorHex));
        cells.add(new Cell(1, startCol - 1, colorHex));
        cells.add(new Cell(1, startCol, colorHex));

        return new Block(cells, color, 5, Orientation.UP);
    }

    public static Block createRandomBlock() {
        Random random = new Random();
        int randomType = random.nextInt(5); // Randomly choose one of the block types
        switch (randomType) {
            case 0:
                return createBlockType1();
            case 1:
                return createBlockType2();
            case 2:
                return createBlockType3();
            case 3:
                return createBlockType4();
            default:
                return createBlockType5();
        }
    }

    private static int getRandomColor() {
        // Define a set of predefined colors
        int[] colors = {
                Color.rgb(255, 0, 0),   // RED
                Color.rgb(0, 255, 0),   // GREEN
                Color.rgb(0, 0, 255),   // BLUE
                Color.rgb(255, 165, 0), // ORANGE
                Color.rgb(128, 0, 128), // PURPLE
                Color.rgb(255, 105, 180) // PINK
        };

        // Randomly select a color from the array
        Random rnd = new Random();
        return colors[rnd.nextInt(colors.length)];
    }

}
