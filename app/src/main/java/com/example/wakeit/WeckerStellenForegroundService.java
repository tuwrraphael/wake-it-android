package com.example.wakeit;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.provider.AlarmClock;
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
import java.util.concurrent.CountDownLatch;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class WeckerStellenForegroundService extends Service {
    private Looper mServiceLooper;
    private ServiceHandler mServiceHandler;

    private final class ServiceHandler extends Handler {
        private Context applicationContext;
        private Context serviceContext;

        public ServiceHandler(Looper looper, Context context, Context serviceContext) {
            super(looper);
            this.applicationContext = context;
            this.serviceContext = serviceContext;
        }

        @Override
        public void handleMessage(final Message msg) {
            WeckZeit w = (WeckZeit) msg.obj;
            Intent i = new Intent(AlarmClock.ACTION_SET_ALARM);
            i.putExtra(AlarmClock.EXTRA_MESSAGE, "WakeIt");
            i.putExtra(AlarmClock.EXTRA_HOUR, w.Stunde);
            i.putExtra(AlarmClock.EXTRA_MINUTES, w.Minute);
            i.putExtra(AlarmClock.EXTRA_SKIP_UI, true);
            startActivity(i);
            try  {
                OkHttpClient client = new OkHttpClient();
                RequestBody body = RequestBody.create(w.SetAlarmId, MediaType.get("text/plain; charset=utf-8"));
                Request request = new Request.Builder()
                        .url("https://wakeit.grapp.workers.dev/alarm-confirmation")
                        .put(body)
                        .build();
                Response response = client.newCall(request).execute();
                if (!response.isSuccessful()) {
                    Log.d("WakeIt", "alarm confirmation failure");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            finally {
                stopSelf(msg.arg1);
            }
        }
    }

    @Override
    public void onCreate() {
        HandlerThread thread = new HandlerThread("ServiceStartArguments",
                android.os.Process.THREAD_PRIORITY_BACKGROUND);
        thread.start();
        mServiceLooper = thread.getLooper();
        mServiceHandler = new ServiceHandler(mServiceLooper, this.getApplicationContext(), this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent =
                PendingIntent.getActivity(this, 0, notificationIntent, 0);

        Notification notification =
                new NotificationCompat.Builder(this, "default")
                        .setContentTitle(getText(R.string.syncing_notification_title))
                        .setContentText(getText(R.string.syncing_notification_content))
                        .setSmallIcon(R.drawable.ic_stat_access_alarm)
                        .setContentIntent(pendingIntent)
                        .setTicker(getText(R.string.syncing_notification_content))
                        .build();

        startForeground(1, notification);

        // For each start request, send a message to start a job and deliver the
        // start ID so we know which request we're stopping when we finish the job
        Message msg = mServiceHandler.obtainMessage();
        msg.arg1 = startId;
        WeckZeit w = new WeckZeit();
        w.Stunde = intent.getIntExtra("hour",0);
        w.Minute = intent.getIntExtra("minute",0);
        w.SetAlarmId = intent.getStringExtra("set-alarm-id");
        msg.obj = w;
        mServiceHandler.sendMessage(msg);
        // If we get killed, after returning from here, restart
        return START_REDELIVER_INTENT;
    }

    @Override
    public IBinder onBind(Intent intent) {
        // We don't provide binding, so return null
        return null;
    }

    @Override
    public void onDestroy() {

    }

    private class WeckZeit {
        public int Stunde;
        public int Minute;
        public String SetAlarmId;
    }
}

