package com.example.projectofinalversioncorta;

import java.util.Locale;

public class Event {
    private int id;
    private long dateMillis;
    private int hour;
    private int minute;
    private String description;

    public Event(int id, long dateMillis, int hour, int minute, String description) {
        this.id = id;
        this.dateMillis = dateMillis;
        this.hour = hour;
        this.minute = minute;
        this.description = description;
    }

    // Getters
    public int getId() {
        return id;
    }

    public long getDateMillis() {
        return dateMillis;
    }

    public int getHour() {
        return hour;
    }

    public int getMinute() {
        return minute;
    }

    public String getDescription() {
        return description;
    }

    // Método para obtener la hora en formato HH:mm
    public String getTime() {
        return String.format(Locale.getDefault(), "%02d:%02d", hour, minute);
    }
}





