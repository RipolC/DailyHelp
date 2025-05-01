package com.example.projectofinalversioncorta;


import android.content.Context;
import android.util.Log;
import okhttp3.*;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;

public class ChatGPTHelper {

    private static final String OPENAI_API_KEY = "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"; // reemplaza con tu clave privada
    private static final String OPENAI_URL = "https://api.openai.com/v1/chat/completions";

    public static void getAdviceFromForecast(Context context, String forecast, Callback callback) {
        OkHttpClient client = new OkHttpClient();

        try {
            // Prompt personalizado
            String prompt = "El pronóstico del clima es el siguiente: \"" + forecast +
                    "\". Dame un consejo sobre qué hacer hoy basándote en ese clima.";

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

