package com.fjuul.sdk.activitysources.entities;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.api.Scope;

import java.util.Set;

@SuppressLint("NewApi")
public final class GoogleFitActivitySource {
    private boolean requestOfflineAccess;

    public GoogleFitActivitySource(boolean requestOfflineAccess) {
        this.requestOfflineAccess = requestOfflineAccess;
    }

    protected Intent buildIntentRequestingPermissions(@NonNull Context context) {
        GoogleSignInClient signInClient = GoogleSignIn.getClient(context, buildGoogleSignInOptions(requestOfflineAccess));
        return signInClient.getSignInIntent();
    }

    public boolean arePermissionsGranted(@NonNull Context context) {
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(context);
        return GoogleFitActivitySource.arePermissionsGranted(account, requestOfflineAccess);
    }

    public boolean arePermissionsGranted(@NonNull GoogleSignInAccount account) {
        return GoogleFitActivitySource.arePermissionsGranted(account, requestOfflineAccess);
    }

    public boolean isOfflineAccessRequested() {
        return requestOfflineAccess;
    }

    // TODO: add method checking if google fit is installed in the system

    private static GoogleSignInOptions buildGoogleSignInOptions(boolean offlineAccess) {
        GoogleSignInOptions.Builder builder = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestScopes(new Scope(Scopes.FITNESS_ACTIVITY_READ), new Scope(Scopes.FITNESS_LOCATION_READ), new Scope(Scopes.FITNESS_BODY_READ));
        if (offlineAccess) {
            // TODO: take serverClientId from string resources
            String serverClientId = "590491392732-6msol3d9s34hcnq5aelnp7a6h51d53n8.apps.googleusercontent.com";
            builder = builder.requestProfile().requestServerAuthCode(serverClientId, true);
        }
        return builder.build();
    }

    static boolean arePermissionsGranted(@Nullable GoogleSignInAccount account, boolean offlineAccess) {
        if (account == null) {
            return false;
        }
        Set<Scope> grantedScopes = account.getGrantedScopes();
        return grantedScopes.containsAll(buildGoogleSignInOptions(offlineAccess).getScopes());
    }

    // TODO: use general callback interface
    public interface HandleSignInResultCallback {
        void onResult(@Nullable Error error, boolean success);
    }
}
