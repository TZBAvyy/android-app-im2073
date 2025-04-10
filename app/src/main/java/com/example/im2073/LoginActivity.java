package com.example.im2073;

import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

import org.json.JSONObject;

public class LoginActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);

        EditText emailInput = findViewById(R.id.et_email);
        EditText passwordInput = findViewById(R.id.et_password);
        Button loginBtn = findViewById(R.id.btn_login);

        loginBtn.setOnClickListener(v -> {
            String email = emailInput.getText().toString().trim();
            String password = passwordInput.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please enter both email and password.", Toast.LENGTH_SHORT).show();
                return;
            }

            new LoginTask().execute(email, password);
        });
    }

    class LoginTask extends AsyncTask<String, Void, JSONObject> {
        @Override
        protected JSONObject doInBackground(String... params) {
            HttpURLConnection conn = null;

            try {
                URL url = new URL("http://10.0.2.2:9999/quiz/login");
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

                String postData = "email=" + URLEncoder.encode(params[0], "UTF-8") +
                                "&password=" + URLEncoder.encode(params[1], "UTF-8");

                OutputStream os = conn.getOutputStream();
                os.write(postData.getBytes());
                os.flush();
                os.close();

                int responseCode = conn.getResponseCode();

                // InputStream is = new BufferedInputStream(conn.getInputStream());
                InputStream is;
                if (conn.getResponseCode() >= HttpURLConnection.HTTP_BAD_REQUEST) {
                    is = new BufferedInputStream(conn.getErrorStream());
                } else {
                    is = new BufferedInputStream(conn.getInputStream());
                }

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
            Log.d("LoginResponse", response.toString());

            try {
                if (response == null) {
                    Toast.makeText(LoginActivity.this, "Server error. Please try again later.", Toast.LENGTH_SHORT).show();
                    return;
                }

                int responseCode = response.getInt("httpResponseCode");

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    int userId = response.getInt("userId");
                    String username = response.getString("username");

                    // store userId & username
                    SharedPreferences prefs = getSharedPreferences("userPrefs", MODE_PRIVATE);
                    prefs.edit().putInt("userId", userId).putString("username", username).apply();

                    // go to next screen - Main Activity (join room)
                    Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                    startActivity(intent);
                } else {
                    Toast.makeText(LoginActivity.this, "Invalid credentials", Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(LoginActivity.this, "Login failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }
}