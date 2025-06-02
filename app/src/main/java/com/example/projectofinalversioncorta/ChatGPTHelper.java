package com.example.projectofinalversioncorta;


import android.content.Context;
import android.util.Log;
import okhttp3.*;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Locale;

public class ChatGPTHelper {

    private static final String OPENAI_API_KEY = ""; // reemplaza con tu clave privada
    private static final String OPENAI_URL = "https://api.openai.com/v1/chat/completions";

    public static void getAdviceFromForecast(Context context, String forecast, Callback callback) {
        OkHttpClient client = new OkHttpClient();

        try {
            // Detectar idioma del sistema
            String language = Locale.getDefault().getLanguage();
            String prompt;

            if (language.equals("es")) {
                // Español
                prompt = "El pronóstico del clima es el siguiente (aunque el pronostico este en ingles, muestralo en español: \"" + forecast +
                        "\". Dame un consejo sobre cómo vestirme y qué llevar conmigo. Antes del consejo, muestra un resumen esquematico del pronostico con algun emoji sin usar la palabra resumen, simplemente pronostico. Debe ser de aproximadamente 300 caracteres. Usa español de España";
            } else {
                // Inglés (por defecto)
                prompt = "The weather forecast is the following (even if it's in Spanish, translate and display it in English): \"" + forecast +
                        "\". Give me advice on how to dress and what to bring. Before the advice, show a schematic overview of the forecast with an emoji, only the forecast. Don't use the word summary. The total response should be about 300 characters.";
            }

            JSONObject messageObj = new JSONObject();
            messageObj.put("role", "user");
            messageObj.put("content", prompt);

            JSONArray messagesArray = new JSONArray();
            messagesArray.put(messageObj);

            JSONObject jsonBody = new JSONObject();
            jsonBody.put("model", "gpt-3.5-turbo");
            jsonBody.put("messages", messagesArray);
            jsonBody.put("temperature", 0.7);

            RequestBody body = RequestBody.create(
                    jsonBody.toString(),
                    MediaType.parse("application/json")
            );

            Request request = new Request.Builder()
                    .url(OPENAI_URL)
                    .addHeader("Authorization", "Bearer " + OPENAI_API_KEY)
                    .addHeader("Content-Type", "application/json")
                    .post(body)
                    .build();

            client.newCall(request).enqueue(callback);

        } catch (Exception e) {
            Log.e("CHATGPT", "Error al construir la petición", e);
        }
    }

}

