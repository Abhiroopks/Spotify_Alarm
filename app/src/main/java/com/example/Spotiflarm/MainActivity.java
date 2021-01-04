package com.example.Spotiflarm;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
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
    public ArrayList<Alarm> alarms;

    public static MainActivity mainActivityInstance = null;
    DisplayMetrics displayMetrics;
    int height;
    int width;
    float density;

    String[] daysOfWeek;


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

        daysOfWeek = new String[]{"M", "T", "W", "T", "F", "S", "S"};






        mainActivityInstance = this;


        // for random request codes of alarms
        rand = new Random(System.currentTimeMillis());
        
        // initialize manager
        alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

        // Instantiate the RequestQueue.
        queue = Volley.newRequestQueue(this);

        // load saved alarms
        try {
            loadAlarms();
            listAlarms();
        }
        catch (Exception e){
            label.append(e.toString());
        }
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

            } else {
                file.createNewFile();
                alarms = new ArrayList<>();
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


        int parentWidth = width - Math.round(16*density);

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
            }
            else{
                alarm.enabled = false;
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
            alarms.remove(alarm);
            alarmsLayout.removeView(entry);

            //TODO cancel alarm


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
        int dayWidth = parentWidth / 7;
        layoutParams = new LinearLayout.LayoutParams(dayWidth,LinearLayout.LayoutParams.MATCH_PARENT);
        for(int i = 0; i < 7; i++){
            Button day = new Button(this);
            day.setLayoutParams(layoutParams);
            day.setText(daysOfWeek[i]);
            day.setTextColor(Color.WHITE);
            day.setGravity(Gravity.CENTER);
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
            alarms.add(newAlarm);
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



}