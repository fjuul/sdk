package com.fjuul.sdk.android.ExampleApp;


import com.fjuul.sdk.analytics.entities.DailyStats;
import com.fjuul.sdk.analytics.http.services.AnalyticsService;
import com.fjuul.sdk.entities.SigningKeychain;
import com.fjuul.sdk.entities.UserCredentials;
import com.fjuul.sdk.http.ApiClient;

import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

public class MainActivity extends AppCompatActivity {
    static String TAG = "FJUUL_SDK";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        TextView textView = findViewById(R.id.main_text);
        textView.setText(TAG);

        // UNCOMMENT BOTTOM LINE to perform a test request
        // testAnalyticsRequest();
    }

    private void testAnalyticsRequest() {
        // NOTE: provide your credentials
        String userToken = "<TOKEN>";
        String secret = "<SECRET>";
        ApiClient client = new ApiClient.Builder(
            getApplicationContext(),
            "https://dev.api.fjuul.com",
            "c1e51fc6-d253-4961-ab9a-5d91560bae75")
            .setUserCredentials(new UserCredentials(userToken, secret))
            .build();
        AnalyticsService analyticsService = new AnalyticsService(client);
        analyticsService
            // NOTE: set an accessible date
            .getDailyStats("2020-06-10")
            .enqueue((call, result) -> {
                if (result.isError()) {
                    Log.i(TAG, String.format("error: %s", result.getError().getMessage()));
                    return;
                }
                DailyStats dailyStats = result.getValue();
                Log.i(TAG,
                    String.format("date: %s; active calories: %f", dailyStats.getDate(), dailyStats.getActiveKcal()));
                Log.i(TAG, String.format("lowest: seconds: %f, metMinutes %f", dailyStats.getLowest().getSeconds(),
                    dailyStats.getLowest().getMetMinutes()));
                Log.i(TAG, String.format("low: seconds: %f, metMinutes %f", dailyStats.getLow().getSeconds(),
                    dailyStats.getLow().getMetMinutes()));
                Log.i(TAG, String.format("moderate: seconds: %f, metMinutes %f", dailyStats.getModerate().getSeconds(),
                    dailyStats.getModerate().getMetMinutes()));
                Log.i(TAG, String.format("high: seconds: %f, metMinutes %f", dailyStats.getHigh().getSeconds(),
                    dailyStats.getHigh().getMetMinutes()));
            });
    }
}
