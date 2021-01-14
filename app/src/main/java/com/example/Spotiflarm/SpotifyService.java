package com.example.Spotiflarm;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;

import androidx.core.app.NotificationCompat;

import com.spotify.android.appremote.api.ConnectionParams;
import com.spotify.android.appremote.api.Connector;
import com.spotify.android.appremote.api.PlayerApi;
import com.spotify.android.appremote.api.SpotifyAppRemote;
import com.spotify.protocol.client.CallResult;
import com.spotify.protocol.types.Empty;

import java.net.URI;

public class SpotifyService extends Service {

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        Intent notifIntent = new Intent(this,MainActivity.class);
        NotificationManager manager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            NotificationChannel channel = new NotificationChannel("default","Default",NotificationManager.IMPORTANCE_DEFAULT);
            manager.createNotificationChannel(channel);
        }

        notifIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notifIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        Notification notification = new NotificationCompat.Builder(this, "default")
                .setDefaults(Notification.DEFAULT_ALL)
                .setWhen(System.currentTimeMillis())
                .setSmallIcon(R.mipmap.ic_launcher_round)
                .setContentTitle("WAKE UP")
                .setContentText("CLICK ME")
                .setContentInfo("Info")
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build();


        startForeground(startId + 1, notification);

        // do work
        connectAppRemote(this, intent, intent.getStringExtra("spotify_res_uri"));

        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        // We don't provide binding, so return null
        return null;
    }

    private void connectAppRemote(Context context, Intent serviceIntent, String spotifyResURI){

        // needed for remote connect
        ConnectionParams connectionParams = new ConnectionParams.Builder(MainActivity.CLIENT_ID)
                .setRedirectUri(MainActivity.REDIRECT_URI)
                .showAuthView(true)
                .build();

        SpotifyAppRemote.connect(context, connectionParams,

                // Define anonymous listener
                new Connector.ConnectionListener() {

                    @Override
                    public void onConnected(SpotifyAppRemote spotifyAppRemote) {
                        // play the specified resource

                        spotifyAppRemote.getPlayerApi().play(spotifyResURI, PlayerApi.StreamType.ALARM).setResultCallback(empty -> SpotifyAppRemote.disconnect(spotifyAppRemote));
                        context.stopService(serviceIntent);
                    }

                    @Override
                    public void onFailure(Throwable throwable) {

                        // play default ringtone if failed
                        Uri uri = RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_ALARM);

                        MediaPlayer player = MediaPlayer.create(context, uri);
                        player.start();

                        context.stopService(serviceIntent);
                    }

                });
    }

}