package com.example.Spotiflarm;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.PowerManager;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.spotify.android.appremote.api.ConnectionParams;
import com.spotify.android.appremote.api.Connector;
import com.spotify.android.appremote.api.PlayerApi;
import com.spotify.android.appremote.api.SpotifyAppRemote;
import com.spotify.android.appremote.internal.SpotifyLocator;
import com.spotify.protocol.client.CallResult;
import com.spotify.protocol.types.Empty;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import static android.content.Context.POWER_SERVICE;

public class MyBroadcastReceiver extends BroadcastReceiver {

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onReceive(Context context, Intent intent) {



        // connect and retrieve the spotify remote. Play music.
        String spotifyResURI = intent.getStringExtra("spotify_res_uri");
        int request_code = intent.getIntExtra("request_code",-1);

        // Start Foreground Service to play music
        /*
        Intent serviceIntent = new Intent(context,SpotifyService.class);
        serviceIntent.putExtra("spotify_res_uri", spotifyResURI);

        context.startForegroundService(serviceIntent);
         */


        // play music now
        connectAppRemote(context,spotifyResURI);


        
        // Notification - do now
        // Open the MainActivity when notif is clicked
        Intent notifIntent = new Intent(context,MainActivity.class);
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            NotificationChannel channel = new NotificationChannel("default","Default",NotificationManager.IMPORTANCE_DEFAULT);
            manager.createNotificationChannel(channel);
        }

        notifIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, notifIntent, PendingIntent.FLAG_UPDATE_CURRENT);


        Notification notification = new NotificationCompat.Builder(context, "default")
                .setDefaults(Notification.DEFAULT_ALL)
                .setWhen(System.currentTimeMillis())
                .setSmallIcon(R.mipmap.ic_launcher_round)
                .setContentTitle("WAKE UP")
                .setContentText("CLICK ME")
                .setContentInfo("Info")
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build();

        manager.notify(1, notification);


        // load alarms saved in file
        ArrayList<Alarm> alarms = loadAlarms(context);
        // something messed up, can't find file of alarms
        if(alarms == null){
            return;
        }
        // otherwise update alarm / schedule next alarm if necessary
        Alarm alarm;

        for(Alarm a : alarms){

            // find the same alarm as this one
            if(a.request_code == request_code){

                alarm = a;

                // only look for next alarm to schedule if repeating is set (just means atleast 1 day is activated for repeats)
                if(alarm.daysActive > 0) {

                    Calendar currCal = Calendar.getInstance();
                    currCal.setTimeInMillis(alarm.timeInMillis);

                    int currDay = currCal.get(Calendar.DAY_OF_WEEK);

                    // look for next day to set alarm
                    // start with tomorrow and proceed until one week from now
                    for (int i = currDay % 7; i != currDay - 1; i = (i + 1) % 7) {

                        // alarm active for this day of week
                        if (alarm.daysOfWeek[i]) {

                            currCal.set(Calendar.DAY_OF_WEEK, i + 1);

                            // in past - advance by 1 week
                            if (currCal.getTimeInMillis() <= alarm.timeInMillis) {
                                currCal.add(Calendar.MILLISECOND, 7 * 24 * 60 * 60 * 1000);
                            }

                            // schedule the next alarm for this day of week
                            alarm.setTime(currCal.getTimeInMillis());
                            Intent alarmintent = new Intent(context, MyBroadcastReceiver.class);
                            alarmintent.putExtra("spotify_res_uri", alarm.spotify_res_uri);
                            alarmintent.putExtra("request_code", alarm.request_code);
                            alarmintent.setAction("com.example.MyBroadcastReceiver");
                            PendingIntent alarmpendingIntent = PendingIntent.getBroadcast(context, alarm.request_code, alarmintent, PendingIntent.FLAG_UPDATE_CURRENT);

                            // Alarm Clock way of scheduling (preferred)
                            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
                            AlarmManager.AlarmClockInfo info = new AlarmManager.AlarmClockInfo(alarm.timeInMillis, alarmpendingIntent);
                            alarmManager.setAlarmClock(info, alarmpendingIntent);

                            // break out of loop - we only need to set the immediate next alarm only
                            break;
                        }

                    }

                }
                // alarm is not repeating - mark as disabled
                else{
                    alarm.enabled = false;

                    // in case alarm goes off while app is still open - update in UI
                    if(MainActivity.mainActivityInstance != null){
                        MainActivity.mainActivityInstance.disableAlarmCheck(request_code);
                    }
                }

                // break out of outer loop - no need to check for other alarms
                break;
            }
        }

        // write to json file
        saveAlarms(context, alarms);

    }

    private void connectAppRemote(Context context, String spotifyResURI){

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
                        spotifyAppRemote.getPlayerApi().play(spotifyResURI, PlayerApi.StreamType.ALARM).setResultCallback(empty -> {
                            // disconnect once playing
                            SpotifyAppRemote.disconnect(spotifyAppRemote);
                        });
                    }

                    @Override
                    public void onFailure(Throwable throwable) {
                        // play default ringtone if failed
                        Uri uri = RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_ALARM);

                        MediaPlayer player = MediaPlayer.create(context, uri);
                        player.start();

                    }

                });
    }

    // loads the alarms in alarmsFile.txt
    private ArrayList<Alarm> loadAlarms(Context context){

        try {
            File file = new File(context.getFilesDir(),"alarmsFile.txt");
            // file exists, read json array from it
            if (file.exists()) {
                Type listType = new TypeToken<ArrayList<Alarm>>() {}.getType();
                return new Gson().fromJson(new FileReader(file), listType);

            } else {
                file.createNewFile();
                return null;
            }

        }
        catch (Exception e){

        }
        return null;
    }


    // write alarms back to JSON file
    private void saveAlarms(Context context, ArrayList<Alarm> alarms){

        // save alarms array to file
        try {
            FileWriter writer = new FileWriter(new File(context.getFilesDir(), "alarmsFile.txt"));
            new Gson().toJson(alarms, writer);
            writer.close();
        } catch (Exception e) {
            //label.append(e.toString());
        }

    }

}