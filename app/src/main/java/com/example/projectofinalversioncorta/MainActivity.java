package com.example.projectofinalversioncorta;

import static androidx.test.InstrumentationRegistry.getContext;

import android.Manifest;
import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.widget.Button;


import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.view.View;
import android.widget.TextView;


import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.microsoft.identity.client.AuthenticationCallback;
import com.microsoft.identity.client.IAccount;
import com.microsoft.identity.client.IAuthenticationResult;
import com.microsoft.identity.client.exception.MsalException;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {
    private Button btnChangeLanguage;
    private Button btnSignInOutlook;
    private static final String PREFS_NAME = "AppSettings";
    private static final String KEY_LANGUAGE = "App_Lang";

    private TextView consejoTextView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    "eventos_channel",
                    "Recordatorios de eventos",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Canal para notificaciones de eventos");

            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }

        AlarmManager alarmManager = (AlarmManager) this.getSystemService(Context.ALARM_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager != null && !alarmManager.canScheduleExactAlarms()) {
                Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                intent.setData(Uri.parse("package:" + this.getPackageName()));
                this.startActivity(intent);
                return;
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 1001);
            }
        }





        applySavedLanguage();
        setContentView(R.layout.activity_main);

        btnChangeLanguage = findViewById(R.id.btnChangeLanguage);
        btnSignInOutlook = findViewById(R.id.btnSignInOutlook);
        btnSignInOutlook.setVisibility(View.GONE); // Ocultar por defecto
        consejoTextView = findViewById(R.id.consejoTextView);


        btnChangeLanguage.setOnClickListener(v -> changeLanguage());

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new CalendarFragment())
                    .commit();
        }

        registerReceiver(new BatteryReceiver(), new IntentFilter(Intent.ACTION_BATTERY_LOW));

        MsalManager msalManager = MsalManager.getInstance(this);

        msalManager.whenReady(() -> {
            msalManager.loadAccount(new MsalManager.AccountCallback() {
                @Override
                public void onAccountAvailable(IAccount account) {
                    btnSignInOutlook.setVisibility(View.GONE);
                    msalManager.acquireTokenSilent(new MsalManager.TokenCallback() {
                        @Override
                        public void onTokenReceived(String token) {
                            sincronizarEventos(token);
                        }

                        @Override
                        public void onError(Exception e) {
                            Log.e("MSAL", "Error al obtener token en segundo plano", e);
                        }
                    });
                }

                @Override
                public void onAccountUnavailable() {
                    btnSignInOutlook.setVisibility(View.VISIBLE);
                    btnSignInOutlook.setOnClickListener(v -> {
                        msalManager.signIn(MainActivity.this, new AuthenticationCallback() {
                            @Override
                            public void onSuccess(IAuthenticationResult authenticationResult) {
                                btnSignInOutlook.setVisibility(View.GONE);
                                sincronizarEventos(authenticationResult.getAccessToken());
                            }

                            @Override
                            public void onError(MsalException e) {
                                Log.e("MSAL", "Error durante el login", e);
                            }

                            @Override
                            public void onCancel() {
                                Log.d("MSAL", "Login cancelado.");
                            }
                        });
                    });
                }

            });
        });

        // Obtener la ciudad por ubicación y consultar el clima
        LocationHelper locationHelper = new LocationHelper(this, city -> {
            if (city != null) {
                WeatherManager.getWeatherForecast(this, city, consejoTextView);

            } else {
                Log.e("CLIMA", "No se pudo obtener la ciudad.");
            }
        });
        locationHelper.requestCity();


    }


    private void sincronizarEventos(String accessToken) {
        OkHttpClient client = new OkHttpClient();

        Request request = new Request.Builder()
                .url("https://graph.microsoft.com/v1.0/me/events")
                .addHeader("Authorization", "Bearer " + accessToken)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e("SYNC", "Error al sincronizar eventos", e);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful()) {
                    String responseBody = response.body().string();

                    try {
                        JsonObject json = JsonParser.parseString(responseBody).getAsJsonObject();
                        JsonArray events = json.getAsJsonArray("value");

                        EventDatabase db = EventDatabase.getInstance(MainActivity.this);

                        for (JsonElement element : events) {
                            JsonObject event = element.getAsJsonObject();

                            String subject = event.get("subject").getAsString();
                            String startDateTime = event.getAsJsonObject("start").get("dateTime").getAsString();

                            long millis = parseDateTimeToMillis(startDateTime);
                            int hour = extractHour(startDateTime);
                            int minute = extractMinute(startDateTime);

                            // Verificación para evitar duplicados
                            if (db.eventoYaExiste(millis, hour, minute, subject)) {
                                Log.d("SYNC", "Evento ya existe, no se insertó: " + subject);
                                continue;
                            }

                            // Ubicación
                            String ubicacion = "Sin ubicación especificada";
                            if (event.has("location") && event.getAsJsonObject("location").has("displayName")) {
                                ubicacion = event.getAsJsonObject("location").get("displayName").getAsString();
                            }

                            // Participantes
                            List<String> participantes = new ArrayList<>();
                            if (event.has("attendees")) {
                                JsonArray attendees = event.getAsJsonArray("attendees");
                                for (JsonElement attendeeElement : attendees) {
                                    JsonObject attendee = attendeeElement.getAsJsonObject();
                                    if (attendee.has("emailAddress")) {
                                        JsonObject emailAddress = attendee.getAsJsonObject("emailAddress");
                                        if (emailAddress.has("name")) {
                                            participantes.add(emailAddress.get("name").getAsString());
                                        }
                                    }
                                }
                            }

                            // Categorías como etiquetas
                            List<String> etiquetas = new ArrayList<>();
                            etiquetas.add("Outlook");  // Marca de sincronizado
                            if (event.has("categories")) {
                                JsonArray categories = event.getAsJsonArray("categories");
                                for (JsonElement cat : categories) {
                                    etiquetas.add(cat.getAsString());
                                }
                            }

                            // Insertar en SQLite
                            db.addEvent(millis, hour, minute, subject, ubicacion, etiquetas, participantes, getContext());
                        }

                        Log.d("SYNC", "Eventos sincronizados correctamente.");
                    } catch (Exception e) {
                        Log.e("SYNC", "Error al parsear eventos", e);
                    }
                } else {
                    Log.e("SYNC", "Error al obtener eventos. Código: " + response.code());
                }
            }
        });
    }






    private void changeLanguage() {
        String currentLanguage = getResources().getConfiguration().locale.getLanguage();
        String newLanguage = currentLanguage.equals("es") ? "en" : "es";

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(KEY_LANGUAGE, newLanguage);
        editor.apply();

        Intent intent = new Intent(this, MainActivity.class);
        finish();
        startActivity(intent);
    }

    private void applySavedLanguage() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String languageCode = prefs.getString(KEY_LANGUAGE, "es");

        Locale locale = new Locale(languageCode);
        Locale.setDefault(locale);
        Configuration config = new Configuration();
        config.setLocale(locale);
        getResources().updateConfiguration(config, getBaseContext().getResources().getDisplayMetrics());
    }

    private long parseDateTimeToMillis(String dateTimeString) {
        try {
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
            formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
            Date date = formatter.parse(dateTimeString.substring(0, 19)); // cortar microsegundos y Z
            return date.getTime();
        } catch (ParseException e) {
            e.printStackTrace();
            return 0;
        }
    }

    private int extractHour(String dateTimeString) {
        try {
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
            formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
            Date date = formatter.parse(dateTimeString.substring(0, 19));
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(date);
            return calendar.get(Calendar.HOUR_OF_DAY);
        } catch (ParseException e) {
            e.printStackTrace();
            return 0;
        }
    }

    private int extractMinute(String dateTimeString) {
        try {
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
            formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
            Date date = formatter.parse(dateTimeString.substring(0, 19));
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(date);
            return calendar.get(Calendar.MINUTE);
        } catch (ParseException e) {
            e.printStackTrace();
            return 0;
        }
    }




}


