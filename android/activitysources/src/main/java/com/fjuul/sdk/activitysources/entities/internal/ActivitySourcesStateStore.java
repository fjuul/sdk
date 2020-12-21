package com.fjuul.sdk.activitysources.entities.internal;

import android.annotation.SuppressLint;

import androidx.annotation.NonNull;

import com.fjuul.sdk.activitysources.entities.TrackerConnection;
import com.fjuul.sdk.core.entities.IStorage;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import com.squareup.moshi.Types;
import com.squareup.moshi.adapters.Rfc3339DateJsonAdapter;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Date;
import java.util.List;
import java.util.Optional;

public class ActivitySourcesStateStore {
    @NonNull IStorage storage;
    @NonNull String lookupKey;
    @NonNull JsonAdapter<List<TrackerConnection>> connectionsJsonAdapter;

    public ActivitySourcesStateStore(@NonNull IStorage storage, @NonNull String userToken) {
        this.storage = storage;
        this.lookupKey = String.format("activity-sources-state.%s", userToken);
        Moshi moshi = new Moshi.Builder().add(Date.class, new Rfc3339DateJsonAdapter()).build();
        Type listMyData = Types.newParameterizedType(List.class, TrackerConnection.class);
        JsonAdapter<List<TrackerConnection>> adapter = moshi.adapter(listMyData);
        this.connectionsJsonAdapter = adapter.nullSafe();
    }

    public void setConnections(@NonNull List<TrackerConnection> connections) {
        if (connections == null) {
            storage.set(lookupKey, null);
            return;
        }
        storage.set(lookupKey, connectionsJsonAdapter.toJson(connections));
    }

    @SuppressLint("NewApi")
    @NonNull
    public Optional<List<TrackerConnection>> getConnections() {
        String connectionsJSON = storage.get(lookupKey);
        if (connectionsJSON == null) {
            return Optional.empty();
        }
        try {
            List<TrackerConnection> connections = connectionsJsonAdapter.fromJson(connectionsJSON);
            return Optional.ofNullable(connections);
        } catch (IOException e) {
            return Optional.empty();
        }
    }
}
