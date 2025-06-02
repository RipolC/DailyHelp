package com.example.projectofinalversioncorta;

import static androidx.constraintlayout.motion.widget.Debug.getLocation;
import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
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
import java.time.ZoneId;
import java.time.ZonedDateTime;
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
    private static final String PREFS_NAME = "AppSettings";
    private static final String KEY_LANGUAGE = "App_Lang";
    private boolean permisos = false;
    private TextView consejoTextView;
    // Declarar los lanzadores arriba en tu actividad
    private ActivityResultLauncher<String> requestNotificationPermissionLauncher;
    private ActivityResultLauncher<String> requestLocationPermissionLauncher;


    @Override

    protected void onCreate(Bundle savedInstanceState) {

        ThemeHelper.applyTheme(this);
        super.onCreate(savedInstanceState);


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

        requestNotificationPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    // Luego del permiso de notificaciones, pide el de ubicación
                    requestLocationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
                }
        );

        requestLocationPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        generarConsejoClimatico();
                    } else {
                        Toast.makeText(this, "@string/location_request", Toast.LENGTH_SHORT).show();
                    }
                }
        );

// Inicia el flujo de permisos
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            } else {
                // Si ya tiene el permiso, pedimos ubicación directamente
                requestLocationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
            }
        } else {
            requestLocationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        }



        applySavedLanguage();
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawerLayout = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.navigation_view);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();


        FloatingActionButton fab = findViewById(R.id.fabConsejoClima);
        CardView cardConsejo = findViewById(R.id.card_consejo);
        TextView consejoTextView = findViewById(R.id.consejoTextView);
        SharedPreferences prefs = getSharedPreferences("clima_prefs", MODE_PRIVATE);

// Generar consejo al iniciar la app
        generarConsejoClimatico();

// FAB muestra u oculta el consejo guardado
        fab.setOnClickListener(view -> {
            if (cardConsejo.getVisibility() == View.VISIBLE) {
                cardConsejo.animate().alpha(0f).translationY(100).setDuration(300).withEndAction(() -> {
                    cardConsejo.setVisibility(View.GONE);
                });
            } else {
                String consejoActualizado = prefs.getString("consejo_texto", "@string/no_advice");
                consejoTextView.setText(consejoActualizado);
                mostrarCardConAnimacion(cardConsejo);
            }
        });




        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new CalendarFragment())
                    .commit();
        }

        registerReceiver(new BatteryReceiver(), new IntentFilter(Intent.ACTION_BATTERY_LOW));

        MsalManager msalManager = MsalManager.getInstance(this);

        // Cargar cuenta al iniciar la app
        msalManager.whenReady(() -> {
            msalManager.loadAccount(new MsalManager.AccountCallback() {
                @Override
                public void onAccountAvailable(IAccount account) {
                    // Ya hay sesión iniciada, sincronizamos automáticamente
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
                    Log.d("MSAL", "No hay cuenta MSAL cargada al iniciar.");
                }
            });
        });

        // Acción del menú lateral
        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_outlook) {
                IAccount cuenta = msalManager.getCurrentAccount();
                if (cuenta != null) {
                    Toast.makeText(this, "@string/current_account", Toast.LENGTH_SHORT).show();
                } else {
                    msalManager.signIn(this, new AuthenticationCallback() {
                        @Override
                        public void onSuccess(IAuthenticationResult authenticationResult) {
                            String token = authenticationResult.getAccessToken();
                            sincronizarEventos(token);
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
                }
            } else if (id == R.id.nav_change_language) {
                changeLanguage();
            }
            else if (id == R.id.nav_change_theme) {
                ThemeHelper.toggleTheme(this);
                recreate();
            }
            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });




    }

    private void pedirPermisoUbicacion() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    1002);
        }
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

                            // Convertir la hora de inicio de UTC a local
                            ZonedDateTime startLocal = ZonedDateTime
                                    .parse(startDateTime + "Z")
                                    .withZoneSameInstant(ZoneId.systemDefault());

                            long millis = startLocal.toInstant().toEpochMilli();
                            int hour = startLocal.getHour();
                            int minute = startLocal.getMinute();

                            // Verificación para evitar duplicados
                            if (db.eventoYaExiste(millis, hour, minute, subject)) {
                                Log.d("SYNC", "Evento ya existe, no se insertó: " + subject);
                                continue;
                            }

                            // Ubicación
                            String ubicacion = "@string/without_location";
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
                            db.addEvent(millis, hour, minute, subject, ubicacion, etiquetas, participantes, getApplicationContext());
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

    private void mostrarCardConAnimacion(CardView card) {
        card.setAlpha(0f);
        card.setTranslationY(100);
        card.setVisibility(View.VISIBLE);
        card.animate().alpha(1f).translationY(0).setDuration(300).start();
    }

    private void generarConsejoClimatico() {
        SharedPreferences prefs = getSharedPreferences("clima_prefs", MODE_PRIVATE);
        String fechaActual = new SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(new Date());
        String ultimaFecha = prefs.getString("fecha_consejo", "");
        String consejoGuardado = prefs.getString("consejo_texto", "");

        if (!fechaActual.equals(ultimaFecha) || consejoGuardado.isEmpty()) {
            LocationHelper locationHelper = new LocationHelper(this, city -> {
                if (city != null) {
                    WeatherManager.getWeatherForecast(this, city, textoConsejo -> {
                        prefs.edit()
                                .putString("fecha_consejo", fechaActual)
                                .putString("consejo_texto", textoConsejo)
                                .apply();

                        Log.d("CLIMA", "Consejo generado y guardado: " + textoConsejo);
                    });
                } else {
                    Log.e("CLIMA", "No se pudo obtener la ciudad.");
                }
            });
            locationHelper.requestCity();
        } else {
            Log.d("CLIMA", "Consejo ya generado para hoy. Se usará el guardado.");
        }
    }


}


