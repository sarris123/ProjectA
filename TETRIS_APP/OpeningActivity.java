package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import androidx.appcompat.app.AppCompatActivity;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

public class OpeningActivity extends AppCompatActivity {
    private WebSocket webSocket;
    private OkHttpClient client;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_opening);

        ImageView startLogo = findViewById(R.id.startLogo);
        startLogo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(OpeningActivity.this, MainActivity.class);
                startActivity(intent);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        connectWebSocket();
    }

    private void connectWebSocket() {
        client = new OkHttpClient();
        Request request = new Request.Builder().url("ws://192.168.4.1:81").build();
        webSocket = client.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onOpen(WebSocket webSocket, okhttp3.Response response) {
                super.onOpen(webSocket, response);
                turnOffLEDs();
            }

            @Override
            public void onFailure(WebSocket webSocket, Throwable t, okhttp3.Response response) {
                super.onFailure(webSocket, t, response);
            }
        });
    }

    private void turnOffLEDs() {
        if (webSocket != null) {
            webSocket.send("TURN_OFF");
            webSocket.close(1000, "Closing after sending TURN_OFF");
        }
    }
}
