package com.example.projectofinalversioncorta;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class EventDatabase extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "events.db";
    private static final int DATABASE_VERSION = 2; // Incrementado para actualizar la base de datos
    private static final String TABLE_EVENTS = "events";

    public EventDatabase(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_EVENTS +
                " (id INTEGER PRIMARY KEY AUTOINCREMENT, dateMillis LONG, hour INTEGER, minute INTEGER, description TEXT)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_EVENTS);
        onCreate(db);
    }

    // Método para agregar un evento con hora y minuto
    public void addEvent(long dateMillis, int hour, int minute, String description) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("dateMillis", dateMillis);
        values.put("hour", hour);
        values.put("minute", minute);
        values.put("description", description);
        db.insert(TABLE_EVENTS, null, values);
        db.close();
    }

    // Método para obtener eventos por fecha
    public List<Event> getEventsByDate(long dateMillis) {
        List<Event> events = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        // Calcular el inicio y fin del día
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(dateMillis);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        long startOfDay = calendar.getTimeInMillis();

        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        calendar.set(Calendar.MILLISECOND, 999);
        long endOfDay = calendar.getTimeInMillis();

        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_EVENTS + " WHERE dateMillis BETWEEN ? AND ?",
                new String[]{String.valueOf(startOfDay), String.valueOf(endOfDay)});

        if (cursor.moveToFirst()) {
            do {
                int id = cursor.getInt(cursor.getColumnIndexOrThrow("id"));
                long eventDateMillis = cursor.getLong(cursor.getColumnIndexOrThrow("dateMillis"));
                int hour = cursor.getInt(cursor.getColumnIndexOrThrow("hour"));
                int minute = cursor.getInt(cursor.getColumnIndexOrThrow("minute"));
                String description = cursor.getString(cursor.getColumnIndexOrThrow("description"));

                events.add(new Event(id, eventDateMillis, hour, minute, description));
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();
        return events;
    }

    // Método para eliminar un evento por ID
    public void deleteEvent(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_EVENTS, "id=?", new String[]{String.valueOf(id)});
        db.close();
    }
}




