package com.example.myapplication;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import java.util.List;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

public class MainActivity extends AppCompatActivity {
    private GameBoardView gameBoardView;
    private WebSocket webSocket;
    private OkHttpClient client;
    private GameBoardState previousState;
    private Handler handler;
    private Runnable gameBoardDataSender;
    private final String TAG = "WebSocketData";
    private boolean gameOverFlag = false;

    private TextView scoreTextView;
    private int scoreCounter = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize scoreTextView and set it in GameBoardView
        scoreTextView = findViewById(R.id.scoreTextView);
        gameBoardView = findViewById(R.id.game_board);
        gameBoardView.setScoreTextView(scoreTextView);

        previousState = gameBoardView.getGameBoardState();

        // Connect to ESP32 and send gameBoardView
        connectWebSocket();

        // Initialize ImageButtons
        ImageButton leftButton = findViewById(R.id.left_button);
        ImageButton rightButton = findViewById(R.id.right_button);
        ImageButton rotateButton = findViewById(R.id.rotate_button);
        ImageButton downButton = findViewById(R.id.down_button);

        // Set event listeners for the buttons
        leftButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                moveBlockLeft();
            }
        });

        rightButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                moveBlockRight();
            }
        });

        rotateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                rotateBlock();
            }
        });

        downButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DropBlock();
            }
        });

        //handler to periodically send the game board data
        handler = new Handler(Looper.getMainLooper());
        gameBoardDataSender = new Runnable() {
            @Override
            public void run() {
                // Check if gameOverFlag is false
                if (!gameOverFlag) {
                    // Gather the game board state and send it over WebSocket
                    String gameBoardState = generateGameBoardState();

                    // Log the game board state being sent
                    Log.d(TAG, "Sending game board data: " + gameBoardState);

                    // Send the game board state over WebSocket
                    if (webSocket != null) {
                        webSocket.send(gameBoardState);
                    }

                    // Schedule the next update
                    handler.postDelayed(this, 500);
                } else {
                    Log.d(TAG, "Game Over - Not sending data");
                }
            }
        };
    }

    // onGameOver method to handle the game over state
    public void onGameOver() {
        gameOverFlag = true;  // Set the flag to true
        handler.removeCallbacks(gameBoardDataSender);  // Stop sending game board data
        Log.d(TAG, "Game Over - Flag set to true");
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Start sending game board data when the activity is resumed
        if (!gameOverFlag) {
            handler.post(gameBoardDataSender);
        } else {
            Log.d(TAG, "Game Over - Not resuming data sending");
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Stop sending game board data when the activity is paused
        handler.removeCallbacks(gameBoardDataSender);
    }

    private void connectWebSocket() {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url("ws://192.168.4.1:81").build();
        webSocket = client.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onMessage(WebSocket webSocket, String text) {
                super.onMessage(webSocket, text);
                Log.d(TAG, "WebSocket message received: " + text);
                if (text.startsWith("GAMEOVER")) {
                    gameOverFlag = true;  // Set the flag to true on GAME_OVER
                    handler.removeCallbacks(gameBoardDataSender); // Stop sending data
                    Log.d(TAG, "Game Over - Flag set to true");
                }
            }
            @Override
            public void onOpen(WebSocket webSocket, okhttp3.Response response) {
                Log.d(TAG, "WebSocket connection opened");
                super.onOpen(webSocket, response);
            }
            @Override
            public void onFailure(WebSocket webSocket, Throwable t, okhttp3.Response response) {
                Log.e(TAG, "WebSocket connection error", t);
                super.onFailure(webSocket, t, response);
            }
        });

        // Pass the WebSocket to the GameBoardView after it's initialized
        gameBoardView.setWebSocket(webSocket);
    }

    private String generateGameBoardState() {
        if (gameOverFlag) {
            Log.d(TAG, "Game Over - No data generated");
            return ""; // Do not send any data if the game is over
        }

        StringBuilder delta = new StringBuilder();
        // Get the updated blocks on the board from the GameBoardState
        GameBoardState currentState = gameBoardView.getGameBoardState();
        List<Block> currentBlocks = currentState.getBlocks();

        // Iterate through the updated blocks
        for (Block block : currentBlocks) {
            List<Cell> cells = block.getCells();
            for (Cell cell : cells) {
                int row = cell.getRow();
                int col = cell.getCol();
                String color = cell.getColor();

                delta.append("(")
                        .append(row).append(",")
                        .append(col).append(",")
                        .append("0,")
                        .append(color).append("),");
            }
        }

        // Check for cells that are no longer occupied
        List<Block> prev = previousState.getBlocks();
        for (Block block : prev) {
            List<Cell> cells = block.getCells();
            for (Cell cell : cells) {
                int row = cell.getRow();
                int col = cell.getCol();
                if (!isOccupiedInCurrentState(row, col)) {
                    delta.append("(")
                            .append(row).append(",")
                            .append(col).append(",")
                            .append("1,")
                            .append("000000")
                            .append("),");
                }
            }
        }

        updatePreviousState(currentBlocks);

        if (delta.length() > 0) {
            delta.setLength(delta.length() - 1);
        }

        return delta.toString();
    }

    private boolean isOccupiedInCurrentState(int newRow, int newCol) {
        GameBoardState currentState = gameBoardView.getGameBoardState();
        List<Block> curr = currentState.getBlocks();
        for (Block block : curr) {
            List<Cell> cells = block.getCells();
            for (Cell cell : cells) {
                int row = cell.getRow();
                int col = cell.getCol();
                if (row == newRow && col == newCol) {
                    return true;
                }
            }
        }
        return false;
    }

    private void updatePreviousState(List<Block> currentBlocks) {
        previousState.getBlocks().clear();
        for (Block block : currentBlocks) {
            previousState.addBlock(new Block(block));
        }
    }

    private void DropBlock() {
        gameBoardView.dropCurrentBlock();
    }

    private void moveBlockLeft() {
        gameBoardView.moveCurrentBlockLeft();
    }

    private void moveBlockRight() {
        gameBoardView.moveCurrentBlockRight();
    }

    private void rotateBlock() {
        gameBoardView.rotateCurrentBlock();
    }
}
