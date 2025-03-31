package com.example.projectofinalversioncorta;

import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    private Button btnChangeLanguage;
    private static final String PREFS_NAME = "AppSettings";
    private static final String KEY_LANGUAGE = "App_Lang";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Aplicar el idioma guardado antes de setContentView()
        applySavedLanguage();

        setContentView(R.layout.activity_main);

        btnChangeLanguage = findViewById(R.id.btnChangeLanguage);
        btnChangeLanguage.setOnClickListener(v -> {
            changeLanguage();
        });

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new CalendarFragment())
                    .commit();
        }

        registerReceiver(new BatteryReceiver(), new IntentFilter(Intent.ACTION_BATTERY_LOW));
    }

    // Cambiar el idioma y guardarlo en SharedPreferences
    private void changeLanguage() {
        String currentLanguage = getResources().getConfiguration().locale.getLanguage();
        String newLanguage = currentLanguage.equals("es") ? "en" : "es";

        // Guardar el idioma en SharedPreferences
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(KEY_LANGUAGE, newLanguage);
        editor.apply();

        // Reiniciar la actividad
        Intent intent = new Intent(this, MainActivity.class);
        finish();
        startActivity(intent);
    }

    // Aplicar el idioma guardado en SharedPreferences
    private void applySavedLanguage() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String languageCode = prefs.getString(KEY_LANGUAGE, "es"); // Por defecto español

        Locale locale = new Locale(languageCode);
        Locale.setDefault(locale);
        Configuration config = new Configuration();
        config.setLocale(locale);
        getResources().updateConfiguration(config, getBaseContext().getResources().getDisplayMetrics());
    }
}

