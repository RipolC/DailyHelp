package com.example.projectofinalversioncorta;

import android.content.Context;
import android.content.res.Configuration;

import java.util.Locale;

public class LanguageHelper {

    public static void changeLanguage(Context context, String languageCode) {
        Locale locale = new Locale(languageCode);
        Locale.setDefault(locale);
        Configuration config = new Configuration();
        config.locale = locale;
        context.getResources().updateConfiguration(config,
                context.getResources().getDisplayMetrics());
    }
}

