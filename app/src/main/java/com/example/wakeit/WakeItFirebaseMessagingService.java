package com.example.wakeit;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.io.IOException;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class WakeItFirebaseMessagingService extends FirebaseMessagingService {
    @Override
    public void onNewToken(String s) {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken("833398424471-5frolmr63e67t0hfup771kih6rh8f583.apps.googleusercontent.com")
                .build();
        GoogleSignInClient mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
        mGoogleSignInClient.silentSignIn().addOnCompleteListener(new OnCompleteListener<GoogleSignInAccount>() {
            @Override
            public void onComplete(@NonNull Task<GoogleSignInAccount> task) {
                try {
                    GoogleSignInAccount result = task.getResult(ApiException.class);
                    handleSignInResult(s, result);
                } catch (ApiException e) {
                }
            }
        });

        super.onNewToken(s);
    }

    private void handleSignInResult(String s, GoogleSignInAccount result) {
        try  {
            OkHttpClient client = new OkHttpClient();
            RequestBody body = RequestBody.create(s, MediaType.get("text/plain; charset=utf-8"));
            Request request = new Request.Builder()
                    .addHeader("Authorization", "Bearer " + result.getIdToken())
                    .url("https://wakeit.grapp.workers.dev/firebase-token")
                    .post(body)
                    .build();
            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    Log.d("WakeIt", "Firebase credential response failure");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        Map<String, String> data = remoteMessage.getData();
        Context context = getApplicationContext();
        Intent i = new Intent(context, WeckerStellenForegroundService.class);
        i.putExtra("hour", Integer.parseInt(data.get("hours")));
        i.putExtra("minute", Integer.parseInt(data.get("minutes")));
        i.putExtra("set-alarm-id", data.get("set-alarm-id"));
        ContextCompat.startForegroundService(context, i);
        super.onMessageReceived(remoteMessage);
    }
}
