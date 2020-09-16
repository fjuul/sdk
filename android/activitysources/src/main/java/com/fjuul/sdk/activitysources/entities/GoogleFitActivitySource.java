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
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.tasks.Task;

import java.util.Set;

@SuppressLint("NewApi")
public class GoogleFitActivitySource {
    public Intent buildIntentRequestingPermissions(@NonNull Context context, @NonNull boolean requestOfflineAccess) {
        GoogleSignInClient signInClient = GoogleSignIn.getClient(context, buildGoogleSignInOptions(requestOfflineAccess));
//        System.out.println("REQUESTING SCOPES: ");
//        buildGoogleSignInOptions(requestOfflineAccess).getScopes().forEach(scope -> {
//            System.out.println("SCOPE: " + scope.toString());
//        });
//        System.out.println("REQUESTING NEW AUTH");
        return signInClient.getSignInIntent();
    }

    public boolean arePermissionsGranted(Context context, boolean offlineAccess) {
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(context);
        return arePermissionsGranted(account, offlineAccess);
    }

    public void handleSignInResult(@NonNull Intent intent, boolean offlineAccessRequested, @NonNull HandleSignInResultCallback callback) {
        try {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(intent);
            GoogleSignInAccount account = task.getResult(ApiException.class);
            boolean permissionsGranted = arePermissionsGranted(account, offlineAccessRequested);
            if (!permissionsGranted) {
                callback.onResult(new Error("Not all permissions were granted"), false);
                return;
            }
            if (!offlineAccessRequested) {
                callback.onResult(null, true);
            }
            String authCode = account.getServerAuthCode();
            if (offlineAccessRequested && authCode == null) {
                callback.onResult(new Error("No server auth code for the requested offline access"), false);
            }
            System.out.println("NEW SERVER AUTH CODE: " + account.getServerAuthCode());
            // TODO: send auth code to the back-end
        } catch (ApiException exc) {
            System.out.println("SIGN IN ERROR: " + exc.getStatusCode());
            exc.printStackTrace();
            callback.onResult(new Error("ApiException: " + exc.getMessage()), false);
        }
    }

    private GoogleSignInOptions buildGoogleSignInOptions(boolean offlineAccess) {
        GoogleSignInOptions.Builder builder = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestScopes(new Scope(Scopes.FITNESS_ACTIVITY_READ), new Scope(Scopes.FITNESS_LOCATION_READ), new Scope(Scopes.FITNESS_BODY_READ));
        if (offlineAccess) {
            // TODO: take serverClientId from string resources
            String serverClientId = "590491392732-6msol3d9s34hcnq5aelnp7a6h51d53n8.apps.googleusercontent.com";
            builder = builder.requestProfile().requestServerAuthCode(serverClientId, true);
        }
        return builder.build();
    }

    private boolean arePermissionsGranted(@Nullable GoogleSignInAccount account, boolean offlineAccess) {
        if (account == null) {
            return false;
        }
        Set<Scope> grantedScopes = account.getGrantedScopes();
        return grantedScopes.containsAll(buildGoogleSignInOptions(offlineAccess).getScopes());
    }

    public interface HandleSignInResultCallback {
        public void onResult(@Nullable Error error, boolean success);
    }
}
