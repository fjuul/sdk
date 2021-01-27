package com.fjuul.sdk.user.http;

import static com.fjuul.sdk.user.exceptions.UserApiExceptions.*;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fjuul.sdk.core.http.utils.ApiCallResult;
import com.fjuul.sdk.core.http.utils.DefaultApiResponseTransformer;
import com.fjuul.sdk.user.http.responses.ValidationErrorJSONBodyResponse;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;

import android.annotation.SuppressLint;
import androidx.annotation.NonNull;
import okio.Buffer;
import retrofit2.Response;

@SuppressLint("NewApi")
public class UserApiResponseTransformer<T> extends DefaultApiResponseTransformer<T> {
    private final JsonAdapter<ValidationErrorJSONBodyResponse> validationErrorJsonBodyAdapter;
    static final List<String> USER_VALIDATION_REQUEST_METHODS = Stream.of("POST", "PUT").collect(Collectors.toList());
    private final Pattern userResourcePathPattern = Pattern.compile("/users/v1(/)?[^/]*$");

    public UserApiResponseTransformer() {
        final Moshi moshi = new Moshi.Builder().build();
        validationErrorJsonBodyAdapter = moshi.adapter(ValidationErrorJSONBodyResponse.class);
    }

    @Override
    public ApiCallResult transform(@NonNull Response response) {
        final String requestPath = response.raw().request().url().encodedPath();
        final String requestMethod = response.raw().request().method();
        if (response.code() == HttpURLConnection.HTTP_BAD_REQUEST
            && USER_VALIDATION_REQUEST_METHODS.contains(requestMethod)
            && userResourcePathPattern.matcher(requestPath).find()) {
            ValidationErrorJSONBodyResponse validationErrorJsonBody = null;
            try {
                // NOTE: we make a copy of the buffer (actually, put the whole body in memory) because Okio doesn't
                // allow us to consume the stream of response body twice.
                final Buffer copy = response.errorBody().source().getBuffer().clone();
                validationErrorJsonBody = validationErrorJsonBodyAdapter.fromJson(copy);
            } catch (IOException e) {}
            if (validationErrorJsonBody != null) {
                return ApiCallResult.error(new ValidationErrorBadRequestException(validationErrorJsonBody.getMessage(),
                    validationErrorJsonBody.getErrors()));
            }
        }
        return super.transform(response);
    }
}
