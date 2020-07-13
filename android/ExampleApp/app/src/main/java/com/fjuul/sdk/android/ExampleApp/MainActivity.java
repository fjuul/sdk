package com.fjuul.sdk.android.ExampleApp;

import java.io.IOException;

import com.fjuul.sdk.analytics.Analytics;
import com.fjuul.sdk.analytics.entities.DailyStats;
import com.fjuul.sdk.analytics.http.services.AnalyticsService;
import com.fjuul.sdk.entities.SigningKeychain;
import com.fjuul.sdk.entities.UserCredentials;
import com.fjuul.sdk.http.FjuulSDKApiHttpClientBuilder;
import com.fjuul.sdk.http.services.UserSigningService;
import com.fjuul.sdk.http.utils.ApiCall;
import com.fjuul.sdk.http.utils.ApiCallCallback;
import com.fjuul.sdk.http.utils.Result;

import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
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
        String userToken = "<TOKEN>";
        String secret = "<SECRET>";
        FjuulSDKApiHttpClientBuilder clientBuilder =
                new FjuulSDKApiHttpClientBuilder(
                        "https://dev.api.fjuul.com", "c1e51fc6-d253-4961-ab9a-5d91560bae75");
        UserSigningService signingService =
                new UserSigningService(clientBuilder, new UserCredentials(userToken, secret));
        AnalyticsService analyticsService =
                new AnalyticsService(clientBuilder, new SigningKeychain(), signingService);

        try {
            analyticsService
                    // NOTE: set an accessible date
                    .getDailyStats(userToken, "2020-06-10")
                    .enqueue(new ApiCallCallback<DailyStats, Error, Result<DailyStats, Error>>() {
                        @Override
                        public void onResponse(ApiCall<DailyStats> call, Result<DailyStats, Error> result) {
                            if (result.isError()) {
                                Log.i(
                                    TAG,
                                    String.format("error: %s", result.getError().getMessage()));
                                return;
                            }
                                DailyStats dailyStats = result.getValue();
                                Log.i(
                                    TAG,
                                    String.format(
                                        "date: %s; active calories: %f",
                                        dailyStats.getDate(),
                                        dailyStats.getActiveCalories()));
                                Log.i(
                                    TAG,
                                    String.format(
                                        "lowest: seconds: %d, metMinutes %f",
                                        dailyStats.getLowest().getSeconds(),
                                        dailyStats.getLowest().getMetMinutes()));
                                Log.i(
                                    TAG,
                                    String.format(
                                        "low: seconds: %d, metMinutes %f",
                                        dailyStats.getLow().getSeconds(),
                                        dailyStats.getLow().getMetMinutes()));
                                Log.i(
                                    TAG,
                                    String.format(
                                        "moderate: seconds: %d, metMinutes %f",
                                        dailyStats.getModerate().getSeconds(),
                                        dailyStats.getModerate().getMetMinutes()));
                                Log.i(
                                    TAG,
                                    String.format(
                                        "high: seconds: %d, metMinutes %f",
                                        dailyStats.getHigh().getSeconds(),
                                        dailyStats.getHigh().getMetMinutes()));

                        }

                        @Override
                        public void onFailure(ApiCall<DailyStats> call, Throwable t) {
                            Log.i(TAG, String.format("error: %s", t.getMessage()));
                        }
                    });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
