package com.example.im2073;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.Button;
import android.view.View;
import android.widget.Toast;
import android.util.Log;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;


public class ChoiceActivity extends AppCompatActivity {

    int roomId, playerId, current_question;
    String choice;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_choice);

        Button btnA = findViewById(R.id.btn_a);
        Button btnB = findViewById(R.id.btn_b);
        Button btnC = findViewById(R.id.btn_c);
        Button btnD = findViewById(R.id.btn_d);


        // Load player & room info from SharedPreferences
        SharedPreferences prefs = getSharedPreferences("sessionPrefs", MODE_PRIVATE);
        roomId = prefs.getInt("roomId", -1);
        playerId = prefs.getInt("playerId", -1);
        current_question = prefs.getInt("current_question", -1);

        View.OnClickListener choiceListener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int id = view.getId();

                if (id == R.id.btn_a) {
                    choice = "A";
                } else if (id == R.id.btn_b) {
                    choice = "B";
                } else if (id == R.id.btn_c) {
                    choice = "C";
                } else if (id == R.id.btn_d) {
                    choice = "D";
                }

                new SelectTask().execute(choice);
            }
        };

        btnA.setOnClickListener(choiceListener);
        btnB.setOnClickListener(choiceListener);
        btnC.setOnClickListener(choiceListener);
        btnD.setOnClickListener(choiceListener);
    }

    class SelectTask extends AsyncTask<String, Void, JSONObject> {
        @Override
        protected JSONObject doInBackground(String... params) {
            HttpURLConnection conn = null;

            try {
                URL url = new URL("http://10.0.2.2:9999/quiz/select");
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

                String postData = "roomId=" + roomId +
                        "&playerId=" + playerId +
                        "&current_question=" + current_question +
                        "&choice=" + URLEncoder.encode(params[0], "UTF-8");

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

            // debugging log
            Log.d("SelectTask", "Response: " + response.toString());

            try {
                if (response == null) {
                    Toast.makeText(ChoiceActivity.this, "Server error. Please try again later.", Toast.LENGTH_SHORT).show();
                    return;
                }

                int responseCode = response.getInt("httpResponseCode");

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    // Go to loading screen
                    Intent intent = new Intent(ChoiceActivity.this, LoadingActivity.class);
                    startActivity(intent);
                    finish();
                } else {
                    Toast.makeText(ChoiceActivity.this, "Failed to submit answer", Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(ChoiceActivity.this, "Error processing response", Toast.LENGTH_SHORT).show();
            }
        }
    }
}