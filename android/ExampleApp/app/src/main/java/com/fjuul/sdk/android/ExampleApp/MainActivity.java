package com.fjuul.sdk.android.ExampleApp;

import com.fjuul.sdk.analytics.Analytics;
import com.fjuul.sdk.analytics.entities.DailyStats;
import com.fjuul.sdk.analytics.services.AnalyticsService;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        TextView textView = findViewById(R.id.main_text);
        textView.setText(new Analytics().getText());

        String userToken = "cef799c2-d189-49ea-9621-4457dca83655";
        String secret = "a1bdbf49-0966-43d4-af33-a7bdfa4b9857";
        FjuulSDKApiHttpClientBuilder clientBuilder = new FjuulSDKApiHttpClientBuilder(
            "https://dev.api.fjuul.com",
            "c1e51fc6-d253-4961-ab9a-5d91560bae75");
        UserSigningService signingService = new UserSigningService(clientBuilder, new UserCredentials(userToken, secret));

        try {
            signingService.issueKey()
                .firstElement()
                .flatMap(signingKeyResult -> {
                    if (!signingKeyResult.isError()) {
                        Log.i("FJUUL_SDK", String.format("error: %s", signingKeyResult.error().toString()));
                        return Maybe.empty();
                    }
                    Response<SigningKey> response = signingKeyResult.response();
                    Log.i("FJUUL_SDK", "parse signing key");
                    SigningKey key = response.body();
                    return Maybe.just(key);
                }).flatMapObservable(signingKey -> {
                    AnalyticsService analyticsService = new AnalyticsService(clientBuilder, new SigningKeychain(signingKey));
                    return analyticsService.getDailyStats(userToken, "2020-02-30");
                }).subscribeOn(Schedulers.io())
                .observeOn(Schedulers.newThread())
//                .unsubscribeOn(Schedulers.io());
                .subscribe(result -> {
                     DailyStats dailyStats = result.response().body();
                Log.i("FJUUL_SDK", String.format("date: %s; active calories: %d", dailyStats.getDate(), dailyStats.getActiveCalories()));
            }, error -> {
                    Log.i("FJUUL_SDK", String.format("error: %s", error.getMessage()));
                });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
