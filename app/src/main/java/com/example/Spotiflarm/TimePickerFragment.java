package com.example.Spotiflarm;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.Dialog;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.RequiresApi;
import androidx.fragment.app.DialogFragment;

import android.text.format.DateFormat;
import android.widget.Button;
import android.widget.TimePicker;
import android.widget.Toast;

import com.spotify.protocol.client.CallResult;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Calendar;
import java.util.Date;
import java.util.Random;

import static androidx.core.content.ContextCompat.getSystemService;


public class TimePickerFragment extends DialogFragment implements TimePickerDialog.OnTimeSetListener {

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Use the current time as the default values for the picker



        Alarm alarm = MainActivity.mainActivityInstance.alarmToChange;

        final Calendar c = Calendar.getInstance();
        c.setTimeInMillis(alarm.timeInMillis);

        // Create a new instance of TimePickerDialog and return it
        return new TimePickerDialog(getActivity(), this, c.get(Calendar.HOUR), c.get(Calendar.MINUTE),
                DateFormat.is24HourFormat(getActivity()));
    }

    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {

        // instance of the MainActivity - used to call non-static methods in it
        MainActivity mainActivity = MainActivity.mainActivityInstance;

        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(System.currentTimeMillis());

        cal.set(Calendar.HOUR_OF_DAY, hourOfDay);
        cal.set(Calendar.MINUTE, minute);

        // is this time in the past?
        // if so, add 24 hours
        long calTime = cal.getTimeInMillis();
        if (calTime < System.currentTimeMillis()) {
            cal.setTimeInMillis(calTime + 24 * 60 * 60 * 1000);
        }


        // create new alarm
        if (mainActivity.addNewAlarm) {
            Alarm newAlarm = new Alarm(cal);

            mainActivity.addNewAlarm(newAlarm);
        }
        // change existing alarm
        else {
            // cancel old alarm
            mainActivity.cancelAlarm(mainActivity.alarmToChange);

            mainActivity.alarmToChange.setTime(cal.getTimeInMillis());
            // set new alarm
            mainActivity.scheduleAlarm(mainActivity.alarmToChange);
            mainActivity.timeButtonToChange.setText(mainActivity.alarmToChange.clockTime);
        }
    }


}
