package com.example.projectofinalversioncorta;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class WeatherManager {
    private static final String API_KEY = "e9db4bda05414e8cad9105648252204";
    private static final String BASE_URL = "https://api.weatherapi.com/v1/forecast.json?days=1&q=";

    public static void getWeatherForecast(Context context, String city, TextView consejoTextView) {
        String url = BASE_URL + city + "&key=" + API_KEY;

        OkHttpClient client = new OkHttpClient();

        Request request = new Request.Builder()
                .url(url)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("CLIMA", "Error al obtener clima", e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    Log.e("CLIMA", "Código de error: " + response.code());
                    return;
                }

                String body = response.body().string();
                try {
                    JSONObject json = new JSONObject(body);
                    String condition = json.getJSONObject("current").getJSONObject("condition").getString("text");
                    double tempC = json.getJSONObject("current").getDouble("temp_c");

                    String forecast = "Clima: " + condition + ", Temperatura: " + tempC + "°C";

                    Log.d("CLIMA", forecast);

                    ChatGPTHelper.getAdviceFromForecast(context, forecast, new Callback() {
                        @Override
                        public void onFailure(@NonNull Call call, @NonNull IOException e) {
                            Log.e("CHATGPT", "Error al obtener consejo", e);
                        }

                        @Override
                        public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                            if (response.isSuccessful()) {
                                String responseBody = response.body().string();
                                try {
                                    JSONObject json = new JSONObject(responseBody);
                                    String consejo = json.getJSONArray("choices")
                                            .getJSONObject(0)
                                            .getJSONObject("message")
                                            .getString("content");

                                    Log.d("CONSEJO", consejo);

                                    // Mostrar en TextView desde hilo principal
                                    new android.os.Handler(Looper.getMainLooper()).post(() -> {
                                        consejoTextView.setText(consejo);
                                    });

                                } catch (Exception e) {
                                    Log.e("CHATGPT", "Error al parsear la respuesta", e);
                                }
                            } else {
                                Log.e("CHATGPT", "Respuesta no exitosa: " + response.code());
                            }
                        }
                    });

                } catch (Exception e) {
                    Log.e("CLIMA", "Error al parsear JSON", e);
                }
            }
        });
    }

}

