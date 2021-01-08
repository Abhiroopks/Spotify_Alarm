package com.example.Spotiflarm;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.provider.AlarmClock;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.Space;
import android.widget.Switch;
import android.widget.TableRow;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.DialogFragment;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Random;

public class MainActivity extends AppCompatActivity {

    // client id used for spotify api
    public static final String CLIENT_ID = "1183302fd87f4e36a3c7702a672fe646";
    // Don't know if we even need a redirect_uri
    public static final String REDIRECT_URI = "https://www.google.com";
    // allow other activities to put HTTP requests into queue
    public static RequestQueue queue;


    // tell TimePickerFragment if we need to add new alarm or change
    public boolean addNewAlarm;
    public Alarm alarmToChange;
    public Button timeButtonToChange;
    public Button musicButtonToChange;

    // used in TimePickerFragment to schedule alarms
    private AlarmManager alarmManager;
    public static TextView label;
    private LinearLayout alarmsLayout;
    private Random rand;
    private ArrayList<Alarm> alarms;

    public static MainActivity mainActivityInstance = null;
    DisplayMetrics displayMetrics;
    int height;
    int width;
    float density;
    int parentWidth;
    int daysWidth;

    String[] daysOfWeekString;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        alarmsLayout = findViewById(R.id.alarmsLayout);
        label = findViewById(R.id.label);

        displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        height = displayMetrics.heightPixels;
        width = displayMetrics.widthPixels;
        density = displayMetrics.density;
        parentWidth = width - Math.round(16*density);
        daysWidth = parentWidth / 7;

        daysOfWeekString = new String[]{"M", "T", "W", "T", "F", "S", "S"};

        mainActivityInstance = this;

        // for random request codes of alarms
        rand = new Random(System.currentTimeMillis());
        
        // initialize manager
        alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

        try{
            AlarmManager.AlarmClockInfo info = alarmManager.getNextAlarmClock();
            Date date = new Date(info.getTriggerTime());
            label.append(date.toString());
        }
        catch (Exception e){
            label.append("No alarms set. ");
        }

        // Instantiate the RequestQueue.
        queue = Volley.newRequestQueue(this);

        // load saved alarms
        loadAlarms();
        listAlarms();

    }



    @Override
    // When app opens
    protected void onStart() {
        super.onStart();
    }


    @Override
    // When app closes
    protected void onStop() {
        super.onStop();
        // save alarms array to file
        try {
            FileWriter writer = new FileWriter(new File(getFilesDir(), "alarmsFile.txt"));
            new Gson().toJson(alarms, writer);
            writer.close();
        } catch (Exception e) {
            //label.append(e.toString());
        }


    }



    // loads the alarms in alarmsFile.txt
    private void loadAlarms(){

        try {
            File file = new File(getFilesDir(),"alarmsFile.txt");
            // file exists, read json array from it
            if (file.exists()) {
                Type listType = new TypeToken<ArrayList<Alarm>>() {}.getType();
                alarms = new Gson().fromJson(new FileReader(file), listType);
                label.append("Read file");
            }
            else {
                file.createNewFile();
                alarms = new ArrayList<>();
                label.append("Created new file + array");
            }

        }
        catch (Exception e){
            label.append(e.toString());
        }

    }

    // display alarms on screen
    private void listAlarms() {
        for(int i = 0; i < alarms.size(); i++) {
            addAlarmToScreen(alarms.get(i));
        }
    }

    // Displays all relevant info of alarm to user
    public void addAlarmToScreen(Alarm alarm){



        // all contents for this alarm will be placed in this
        LinearLayout entry = new LinearLayout(this);
        entry.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        entry.setLayoutParams(layoutParams);


        // First row: time, enable, delete
        int row1Height = height/5;
        layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, row1Height);
        LinearLayout row1 = new LinearLayout(this);
        row1.setOrientation(LinearLayout.HORIZONTAL);
        row1.setLayoutParams(layoutParams);


        // text to show/change time of alarm
        int timeWidth = parentWidth/2;

        layoutParams = new LinearLayout.LayoutParams(timeWidth, LinearLayout.LayoutParams.MATCH_PARENT);
        Button timeButton = new Button(this);
        timeButton.setLayoutParams(layoutParams);
        timeButton.setTextColor(Color.WHITE);
        timeButton.setText(alarm.clockTime);
        timeButton.setGravity(Gravity.CENTER);

        timeButton.setOnClickListener(view -> {
            // show clock dialog to user and change time
            addNewAlarm = false;
            alarmToChange = alarm;
            timeButtonToChange = timeButton;
            showTimePickerDialog(view);
        });


        // column to hold enable/disable stuff
        LinearLayout col1 = new LinearLayout(this);
        col1.setOrientation(LinearLayout.VERTICAL);
        col1.setLayoutParams(layoutParams);
        // enable/disable switch
        int enableHeight = row1Height / 2;
        layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,enableHeight);
        SwitchCompat enableSwitch = new SwitchCompat(this);
        enableSwitch.setLayoutParams(layoutParams);
        enableSwitch.setGravity(Gravity.CENTER);
        enableSwitch.setChecked(alarm.enabled);
        enableSwitch.setOnCheckedChangeListener((compoundButton, b) -> {
            if(b){
                alarm.enabled = true;
                scheduleAlarm(alarm);
            }
            else{
                alarm.enabled = false;
                cancelAlarm(alarm);
            }

        });
        // delete button
        Button deleteButton = new Button(this);
        deleteButton.setLayoutParams(layoutParams);
        deleteButton.setText("Delete");
        deleteButton.setTextColor(Color.WHITE);
        deleteButton.setGravity(Gravity.CENTER);

        //TextView finalMusicTextView = musicTextView;
        deleteButton.setOnClickListener(view -> {
            cancelAlarm(alarm);
            alarms.remove(alarm);
            alarmsLayout.removeView(entry);
        });

        // build col1
        col1.addView(enableSwitch);
        col1.addView(deleteButton);

        // build row1
        row1.addView(timeButton);
        row1.addView(col1);



        // text to show/change spotify resource to play
        layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,enableHeight);
        Button musicButton = new Button(this);
        musicButton.setLayoutParams(layoutParams);
        musicButton.setTextColor(Color.WHITE);
        musicButton.setGravity(Gravity.CENTER);
        musicButton.setText(alarm.spotify_res_name);
        musicButton.setHorizontallyScrolling(true);
        musicButton.setSingleLine();
        musicButton.setEllipsize(TextUtils.TruncateAt.MARQUEE);
        musicButton.setMarqueeRepeatLimit(-1);
        musicButton.setSelected(true);
        musicButton.setOnClickListener(view -> {
            alarmToChange = alarm;
            musicButtonToChange = musicButton;

            Intent intent = new Intent(getApplicationContext(), MusicSelector.class);
            startActivity(intent);
        });


        // days of the week
        LinearLayout daysRow = new LinearLayout(this);
        daysRow.setLayoutParams(layoutParams);
        daysRow.setOrientation(LinearLayout.HORIZONTAL);
        layoutParams = new LinearLayout.LayoutParams(daysWidth,LinearLayout.LayoutParams.MATCH_PARENT);
        for(int i = 0; i < 7; i++){
            Button day = new Button(this);
            day.setLayoutParams(layoutParams);
            day.setText(daysOfWeekString[i]);
            if(alarm.daysOfWeek[i]){
                day.setTextColor(Color.GREEN);
            }
            else{
                day.setTextColor(Color.WHITE);
            }
            day.setGravity(Gravity.CENTER);
            int finalI = i;
            day.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                    // flip day
                    alarm.daysOfWeek[finalI] = ! alarm.daysOfWeek[finalI];

                    // change color
                    if(alarm.daysOfWeek[finalI]){
                        day.setTextColor(Color.GREEN);
                    }
                    else{
                        day.setTextColor(Color.WHITE);
                    }

                    // set repeating boolean
                    alarm.repeating |= alarm.daysOfWeek[finalI];


                }
            });
            daysRow.addView(day);
        }

        TableRow divider = new TableRow(this);
        layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,Math.round(1*density));
        int marginTop = Math.round(8*density);
        layoutParams.setMargins(0, marginTop, 0, marginTop);
        divider.setLayoutParams(layoutParams);
        divider.setBackgroundColor(Color.WHITE);

        entry.addView(row1);
        entry.addView(musicButton);
        entry.addView(daysRow);
        entry.addView(divider);

        alarmsLayout.addView(entry);
    }

    // on Click
    public void newAlarmBtnClick(View view){
        addNewAlarm = true;
        showTimePickerDialog(view);

    }

    // called by TimePickerFragment to add new alarm
    public void addNewAlarm(Alarm newAlarm){
        try {
            // default values
            newAlarm.spotify_res_name = "Rise";
            newAlarm.spotify_res_uri = "spotify:playlist:37i9dQZF1DWUOhRIDwDB7M";
            newAlarm.enabled = true;
            newAlarm.request_code = rand.nextInt();
            newAlarm.daysOfWeek = new boolean[7];
            // not repeating by default
            Arrays.fill(newAlarm.daysOfWeek, false);
            newAlarm.repeating = false;

            alarms.add(newAlarm);
            scheduleAlarm(newAlarm);
            addAlarmToScreen(newAlarm);
        }
        catch (Exception e){
            label.append(e.toString());
        }
    }

    // Open clock activity to set alarm time
    public void showTimePickerDialog(View view) {
        DialogFragment newFragment = new TimePickerFragment();
        newFragment.show(getSupportFragmentManager(), "timePicker");
    }

    // cancel alarm with the given request_code
    public void cancelAlarm(Alarm alarm){
        Intent intent = new Intent(this, MyBroadcastReceiver.class);
        //intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
        //intent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
        //intent.putExtra("spotify_res_uri", alarm.spotify_res_uri);
        intent.setAction("com.example.MyBroadcastReceiver");
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this,alarm.request_code, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        alarmManager.cancel(pendingIntent);
    }

    // schedules alarm at specific time, with specific request_code
    public void scheduleAlarm(Alarm alarm){

        Intent intent = new Intent(this, MyBroadcastReceiver.class);
        intent.putExtra("spotify_res_uri", alarm.spotify_res_uri);
        intent.putExtra("request_code", alarm.request_code);
        intent.setAction("com.example.MyBroadcastReceiver");
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this,alarm.request_code, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        // Alarm Clock way of scheduling (preferred)
        AlarmManager.AlarmClockInfo info = new AlarmManager.AlarmClockInfo(alarm.timeInMillis, pendingIntent);
        alarmManager.setAlarmClock(info, pendingIntent);

    }



}