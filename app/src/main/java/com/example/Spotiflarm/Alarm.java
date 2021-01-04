package com.example.Spotiflarm;

import java.util.Calendar;

public class Alarm {
    long timeInMillis;
    String clockTime;
    String spotify_res_name;
    String spotify_res_uri;
    boolean enabled;

    Alarm(Calendar cal){
        this.timeInMillis = cal.getTimeInMillis();
        setClockTime(cal.get(Calendar.HOUR), cal.get(Calendar.MINUTE), cal.get(Calendar.AM_PM) == Calendar.AM ? "AM":"PM");
    }

    public void setTime(long time){
        this.timeInMillis = time;

        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(time);
        setClockTime(cal.get(Calendar.HOUR), cal.get(Calendar.MINUTE), cal.get(Calendar.AM_PM) == Calendar.AM ? "AM":"PM");
    }

    private void setClockTime(int hour, int min, String am_pm){

        if(hour == 0){
            hour = 12;
        }

        StringBuilder sb = new StringBuilder();
        sb.append(hour + ":");
        if(min < 10){
            sb.append("0" + min + " ");
        }
        else{
            sb.append(min + " ");
        }

        sb.append(am_pm);

        this.clockTime = sb.toString();
    }




}
