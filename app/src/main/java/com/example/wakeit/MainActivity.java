package com.example.wakeit;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.content.Intent;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.messaging.FirebaseMessaging;

import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.default_channel_name);
            String description = getString(R.string.default_channel_description);
            int importance = NotificationManager.IMPORTANCE_LOW;
            NotificationChannel channel = new NotificationChannel("default", name, importance);
            channel.setDescription(description);
            channel.setSound(null, null);
            channel.enableVibration(false);
            channel.enableLights(false);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken("833398424471-5frolmr63e67t0hfup771kih6rh8f583.apps.googleusercontent.com")
                .build();
        GoogleSignInClient mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
        mGoogleSignInClient.silentSignIn().addOnCompleteListener(this, new OnCompleteListener<GoogleSignInAccount>() {
            @Override
            public void onComplete(@NonNull Task<GoogleSignInAccount> task) {
                try {
                    GoogleSignInAccount result = task.getResult(ApiException.class);
                    handleSignInResult(result);
                } catch (ApiException e) {
                    Intent signInIntent = mGoogleSignInClient.getSignInIntent();
                    startActivityForResult(signInIntent, 1);
                }
            }
        });
    }

    private void handleSignInResult(GoogleSignInAccount result) {
     FirebaseMessaging.getInstance().getToken().addOnCompleteListener(this, new OnCompleteListener<String>() {
         @Override
         public void onComplete(@NonNull Task<String> task) {
             String token = task.getResult();
             Thread thread = new Thread(new Runnable() {
                 @Override
                 public void run() {
                     try  {
                         OkHttpClient client = new OkHttpClient();
                         RequestBody body = RequestBody.create(token, MediaType.get("text/plain; charset=utf-8"));
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
             });
             thread.start();

         }
     });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Result returned from launching the Intent from GoogleSignInClient.getSignInIntent(...);
        if (requestCode == 1) {
            // The Task returned from this call is always completed, no need to attach
            // a listener.
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            task.addOnCompleteListener(this, new OnCompleteListener<GoogleSignInAccount>() {
                @Override
                public void onComplete(@NonNull Task<GoogleSignInAccount> task) {
                    handleSignInResult(task.getResult());
                }
            });
        }
    }
}

