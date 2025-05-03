package com.example.projectofinalversioncorta;

import static androidx.core.content.ContentProviderCompat.requireContext;
import static androidx.core.content.ContextCompat.getSystemService;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Build;
import android.util.Log;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class EventDatabase extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "agenda.db";
    private static final int DATABASE_VERSION = 2;
    public static final String TABLE_EVENTS = "eventos";
    public static final String TABLE_LOCATIONS = "ubicaciones";
    public static final String TABLE_TAGS = "etiquetas";
    public static final String TABLE_PARTICIPANTS = "participantes";
    public static final String TABLE_EVENT_TAG = "evento_etiqueta";
    public static final String TABLE_EVENT_PARTICIPANT = "evento_participante";

    public EventDatabase(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_LOCATIONS + " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "nombre TEXT UNIQUE)");

        db.execSQL("CREATE TABLE " + TABLE_TAGS + " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "nombre TEXT UNIQUE)");

        db.execSQL("CREATE TABLE " + TABLE_PARTICIPANTS + " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "nombre TEXT UNIQUE)");

        db.execSQL("CREATE TABLE " + TABLE_EVENTS + " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "dateMillis LONG, " +
                "hour INTEGER, " +
                "minute INTEGER, " +
                "description TEXT, " +
                "ubicacion_id INTEGER, " +
                "FOREIGN KEY(ubicacion_id) REFERENCES " + TABLE_LOCATIONS + "(id))");

        db.execSQL("CREATE TABLE " + TABLE_EVENT_TAG + " (" +
                "evento_id INTEGER, " +
                "etiqueta_id INTEGER, " +
                "PRIMARY KEY (evento_id, etiqueta_id), " +
                "FOREIGN KEY (evento_id) REFERENCES " + TABLE_EVENTS + "(id), " +
                "FOREIGN KEY (etiqueta_id) REFERENCES " + TABLE_TAGS + "(id))");

        db.execSQL("CREATE TABLE " + TABLE_EVENT_PARTICIPANT + " (" +
                "evento_id INTEGER, " +
                "participante_id INTEGER, " +
                "PRIMARY KEY (evento_id, participante_id), " +
                "FOREIGN KEY (evento_id) REFERENCES " + TABLE_EVENTS + "(id), " +
                "FOREIGN KEY (participante_id) REFERENCES " + TABLE_PARTICIPANTS + "(id))");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_EVENT_PARTICIPANT);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_EVENT_TAG);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_EVENTS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_PARTICIPANTS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_TAGS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_LOCATIONS);
        onCreate(db);
    }

    // Métodos para insertar entidades si no existen
    private long getOrInsertUbicacion(SQLiteDatabase db, String nombre) {
        Cursor cursor = db.rawQuery("SELECT id FROM " + TABLE_LOCATIONS + " WHERE nombre = ?", new String[]{nombre});
        if (cursor.moveToFirst()) {
            long id = cursor.getLong(0);
            cursor.close();
            return id;
        } else {
            cursor.close();
            ContentValues values = new ContentValues();
            values.put("nombre", nombre);
            return db.insert(TABLE_LOCATIONS, null, values);
        }
    }

    private long getOrInsertEtiqueta(SQLiteDatabase db, String nombre) {
        Cursor cursor = db.rawQuery("SELECT id FROM " + TABLE_TAGS + " WHERE nombre = ?", new String[]{nombre});
        if (cursor.moveToFirst()) {
            long id = cursor.getLong(0);
            cursor.close();
            return id;
        } else {
            cursor.close();
            ContentValues values = new ContentValues();
            values.put("nombre", nombre);
            return db.insert(TABLE_TAGS, null, values);
        }
    }

    private long getOrInsertParticipante(SQLiteDatabase db, String nombre) {
        Cursor cursor = db.rawQuery("SELECT id FROM " + TABLE_PARTICIPANTS + " WHERE nombre = ?", new String[]{nombre});
        if (cursor.moveToFirst()) {
            long id = cursor.getLong(0);
            cursor.close();
            return id;
        } else {
            cursor.close();
            ContentValues values = new ContentValues();
            values.put("nombre", nombre);
            return db.insert(TABLE_PARTICIPANTS, null, values);
        }
    }

    // Insertar evento completo con relaciones
    public void addEvent(long dateMillis, int hour, int minute, String description,
                         String ubicacion, List<String> etiquetas, List<String> participantes, Context context) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        try {
            long ubicacionId = getOrInsertUbicacion(db, ubicacion);

            ContentValues values = new ContentValues();
            values.put("dateMillis", dateMillis);
            values.put("hour", hour);
            values.put("minute", minute);
            values.put("description", description);
            values.put("ubicacion_id", ubicacionId);
            long eventId = db.insert(TABLE_EVENTS, null, values);

            for (String etiqueta : etiquetas) {
                long etiquetaId = getOrInsertEtiqueta(db, etiqueta);
                ContentValues eeValues = new ContentValues();
                eeValues.put("evento_id", eventId);
                eeValues.put("etiqueta_id", etiquetaId);
                db.insert(TABLE_EVENT_TAG, null, eeValues);
            }

            for (String participante : participantes) {
                long participanteId = getOrInsertParticipante(db, participante);
                ContentValues epValues = new ContentValues();
                epValues.put("evento_id", eventId);
                epValues.put("participante_id", participanteId);
                db.insert(TABLE_EVENT_PARTICIPANT, null, epValues);
            }
            programarAlarma(context, dateMillis, hour, minute, description);



            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
            db.close();
        }
    }

    public Event getEventById(int eventId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Event event = null;

        Cursor cursor = db.rawQuery("SELECT * FROM eventos WHERE id = ?", new String[]{String.valueOf(eventId)});

        if (cursor.moveToFirst()) {
            long dateMillis = cursor.getLong(cursor.getColumnIndexOrThrow("dateMillis"));
            int hour = cursor.getInt(cursor.getColumnIndexOrThrow("hour"));
            int minute = cursor.getInt(cursor.getColumnIndexOrThrow("minute"));
            String description = cursor.getString(cursor.getColumnIndexOrThrow("description"));
            int ubicacionId = cursor.getInt(cursor.getColumnIndexOrThrow("ubicacion_id"));

            event = new Event(eventId, dateMillis, hour, minute, description);

            // Cargar ubicación si existe
            if (ubicacionId != 0) {
                Ubication ubicacion = getUbicacionById(db, ubicacionId);
                event.setUbicacion(ubicacion);
            }

            // Cargar etiquetas
            List<Tag> etiquetas = getEtiquetasByEventId(db, eventId);
            event.setEtiquetas(etiquetas);

            // Cargar participantes
            List<Participant> participantes = getParticipantesByEventId(db, eventId);
            event.setParticipantes(participantes);
        }

        cursor.close();
        db.close();
        return event;
    }



    // Obtener eventos con toda la información relacionada
    public List<Event> getEventsByDate(long dateMillis) {
        List<Event> events = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

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
                int ubicacionId = cursor.getInt(cursor.getColumnIndexOrThrow("ubicacion_id"));

                // Crear evento base
                Event event = new Event(id, eventDateMillis, hour, minute, description);

                // Cargar ubicación
                event.setUbicacion(getUbicacionById(db, ubicacionId));

                // Cargar etiquetas
                event.setEtiquetas(getEtiquetasByEventId(db, id));

                // Cargar participantes
                event.setParticipantes(getParticipantesByEventId(db, id));

                events.add(event);
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();
        return events;
    }


    private Ubication getUbicacionById(SQLiteDatabase db, int id) {
        Cursor cursor = db.rawQuery("SELECT * FROM ubicaciones WHERE id = ?", new String[]{String.valueOf(id)});
        Ubication ubicacion = null;
        if (cursor.moveToFirst()) {
            String nombre = cursor.getString(cursor.getColumnIndexOrThrow("nombre"));
            ubicacion = new Ubication(id, nombre);
        }
        cursor.close();
        return ubicacion;
    }


    private List<Tag> getEtiquetasByEventId(SQLiteDatabase db, int eventId) {
        List<Tag> etiquetas = new ArrayList<>();
        Cursor cursor = db.rawQuery(
                "SELECT e.id, e.nombre FROM etiquetas e " +
                        "JOIN evento_etiqueta ee ON e.id = ee.etiqueta_id " +
                        "WHERE ee.evento_id = ?", new String[]{String.valueOf(eventId)});
        if (cursor.moveToFirst()) {
            do {
                int id = cursor.getInt(cursor.getColumnIndexOrThrow("id"));
                String nombre = cursor.getString(cursor.getColumnIndexOrThrow("nombre"));
                etiquetas.add(new Tag(id, nombre));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return etiquetas;
    }


    private List<Participant> getParticipantesByEventId(SQLiteDatabase db, int eventId) {
        List<Participant> participantes = new ArrayList<>();
        Cursor cursor = db.rawQuery(
                "SELECT p.id, p.nombre FROM participantes p " +
                        "JOIN evento_participante ep ON p.id = ep.participante_id " +
                        "WHERE ep.evento_id = ?", new String[]{String.valueOf(eventId)});
        if (cursor.moveToFirst()) {
            do {
                int id = cursor.getInt(cursor.getColumnIndexOrThrow("id"));
                String nombre = cursor.getString(cursor.getColumnIndexOrThrow("nombre"));
                participantes.add(new Participant(id, nombre));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return participantes;
    }


    // Borrar un evento por ID (incluye relaciones)
    public void deleteEvent(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        try {
            db.delete(TABLE_EVENT_PARTICIPANT, "evento_id = ?", new String[]{String.valueOf(id)});
            db.delete(TABLE_EVENT_TAG, "evento_id = ?", new String[]{String.valueOf(id)});
            db.delete(TABLE_EVENTS, "id = ?", new String[]{String.valueOf(id)});
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
            db.close();
        }
    }
    private static EventDatabase instance;

    public static synchronized EventDatabase getInstance(Context context) {
        if (instance == null) {
            instance = new EventDatabase(context.getApplicationContext());
        }
        return instance;
    }


    public boolean eventoYaExiste(long dateMillis, int hour, int minute, String description) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_EVENTS, null,
                "dateMillis = ? AND hour = ? AND minute = ? AND description = ?",
                new String[]{String.valueOf(dateMillis), String.valueOf(hour), String.valueOf(minute), description},
                null, null, null);
        boolean exists = cursor.moveToFirst();
        cursor.close();
        return exists;
    }

    public void programarAlarma(Context context, long dateMillis, int hour, int minute, String description) {
        // Intent que será recibido por EventReminderReceiver
        Intent intent = new Intent(context, EventReminderReceiver.class);
        intent.putExtra("description", description);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                (int) dateMillis, // ID único
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Fecha y hora del evento
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(dateMillis);
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, 0);

        long triggerAt = calendar.getTimeInMillis();
        Log.d("ALARMA", "Programando alarma para: " + triggerAt);

        if (triggerAt > System.currentTimeMillis()) {
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            if (alarmManager != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            calendar.getTimeInMillis(),
                            pendingIntent
                    );
                } else {
                    // Para versiones anteriores a Marshmallow
                    alarmManager.setExact(
                            AlarmManager.RTC_WAKEUP,
                            calendar.getTimeInMillis(),
                            pendingIntent
                    );
                }

            }
        } else {
            Log.w("ALARMA", "No se programó la alarma porque la fecha/hora ya pasó.");
        }
    }






}








