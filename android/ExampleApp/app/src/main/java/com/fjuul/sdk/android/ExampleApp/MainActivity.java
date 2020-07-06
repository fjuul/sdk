package com.fjuul.sdk.android.ExampleApp;

import com.fjuul.sdk.analytics.Analytics;
import com.fjuul.sdk.analytics.entities.DailyStats;
import com.fjuul.sdk.analytics.http.services.AnalyticsService;
import com.fjuul.sdk.entities.SigningKey;
import com.fjuul.sdk.entities.SigningKeychain;
import com.fjuul.sdk.entities.UserCredentials;
import com.fjuul.sdk.http.FjuulSDKApiHttpClientBuilder;
import com.fjuul.sdk.http.services.UserSigningService;

import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import java.io.IOException;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import io.reactivex.Maybe;

import io.reactivex.schedulers.Schedulers;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {
    static String TAG = "FJUUL_SDK";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        TextView textView = findViewById(R.id.main_text);
        textView.setText(new Analytics().getText());

        // UNCOMMENT BOTTOM LINE to perform a test request
         testAnalyticsRequest();
    }

    private void testAnalyticsRequest() {
        // NOTE: provide your credentials
        String userToken = "cef799c2-d189-49ea-9621-4457dca83655";
        String secret = "a1bdbf49-0966-43d4-af33-a7bdfa4b9857";
        FjuulSDKApiHttpClientBuilder clientBuilder = new FjuulSDKApiHttpClientBuilder(
            "https://dev.api.fjuul.com",
            "c1e51fc6-d253-4961-ab9a-5d91560bae75");
        UserSigningService signingService = new UserSigningService(clientBuilder, new UserCredentials(userToken, secret));
        AnalyticsService analyticsService = new AnalyticsService(clientBuilder, new SigningKeychain(), signingService);

        try {
            analyticsService
                // NOTE: set an accessible date
                .getDailyStats(userToken, "2020-06-30")
                .firstElement()
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.newThread())
                .subscribe(result -> {
                    if (result.isError()) {
                        Log.i(TAG, String.format("error: %s", result.error().toString()));
                        return;
                    }

                    if (result.response().isSuccessful()) {
                        DailyStats dailyStats = result.response().body();
                        Log.i(TAG, String.format("date: %s; active calories: %d", dailyStats.getDate(), dailyStats.getActiveCalories()));
                    } else {
                        Response<DailyStats> response = result.response();
                        Log.i(TAG, String.format("error response: %d %s", response.code(), response.errorBody().string()));
                    }
                }, error -> {
                    Log.i(TAG, String.format("error: %s", error.getMessage()));
                });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
