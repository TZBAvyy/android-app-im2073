package com.example.im2073;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // Now it's safe to call these
        SharedPreferences prefs = getSharedPreferences("userPrefs", MODE_PRIVATE);
        int userId = prefs.getInt("userId", -1);

        EditText roomCodeInput = findViewById(R.id.et_enter_room_code);
        Button joinRoomBtn = findViewById(R.id.btn_join_room);
        Button logoutBtn = findViewById(R.id.btn_logout);

        joinRoomBtn.setOnClickListener(v -> {
            String roomCode = roomCodeInput.getText().toString().trim();

            if (roomCode.isEmpty()) {
                Toast.makeText(MainActivity.this, "Please enter the room code.", Toast.LENGTH_SHORT).show();
                return;
            }

            new JoinRoomTask().execute(roomCode, String.valueOf(userId));
        });

        logoutBtn.setOnClickListener(v -> {
            // Clear stored userPrefs if logging out
            getSharedPreferences("userPrefs", MODE_PRIVATE).edit().clear().apply();

            // Back to login screen
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            startActivity(intent);
            finish(); // close current activity
        });
    }

    class JoinRoomTask extends AsyncTask<String, Void, JSONObject> {
        @Override
        protected JSONObject doInBackground(String... params) {
            HttpURLConnection conn = null;

            try {
                URL url = new URL("http://10.0.2.2:9999/quiz/join_room");
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

                String postData = "roomCode=" + URLEncoder.encode(params[0], "UTF-8") +
                        "&userId=" + URLEncoder.encode(params[1], "UTF-8");

                OutputStream os = conn.getOutputStream();
                os.write(postData.getBytes());
                os.flush();
                os.close();

                int responseCode = conn.getResponseCode();

                InputStream is = new BufferedInputStream(conn.getInputStream());
                BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                StringBuilder result = new StringBuilder();
                String line;

                while ((line = reader.readLine()) != null) {
                    result.append(line);
                }

                JSONObject jsonResponse = new JSONObject(result.toString());
                jsonResponse.put("httpResponseCode", responseCode);
                return jsonResponse;

            } catch (Exception e) {
                e.printStackTrace();
                return null;
            } finally {
                if (conn != null) conn.disconnect();
            }
        }

        @Override
        protected void onPostExecute(JSONObject response) {
            try {
                if (response == null) {
                    Toast.makeText(MainActivity.this, "Server error. Please try again later.", Toast.LENGTH_SHORT).show();
                    return;
                }

                int responseCode = response.getInt("httpResponseCode");

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    int roomId = response.getInt("roomId");
                    int playerId = response.getInt("playerId");
                    int current_question = response.getInt("current_question");

                    // store room info
                    SharedPreferences prefs = getSharedPreferences("sessionPrefs", MODE_PRIVATE);
                    prefs.edit()
                            .putInt("roomId", roomId)
                            .putInt("playerId", playerId)
                            .putInt("current_question", current_question)
                            .apply();

                    // go to next screen - Choice Activity
                    Intent intent = new Intent(MainActivity.this, ChoiceActivity.class);
                    startActivity(intent);

                } else {
                    Toast.makeText(MainActivity.this, "Invalid room code.", Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(MainActivity.this, "Join failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

}