package com.example.projectofinalversioncorta;

import static java.lang.System.currentTimeMillis;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

public class EventNotificationWorker extends Worker {

    public EventNotificationWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        String description = getInputData().getString("description");

        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), "event_reminder_channel")
                .setSmallIcon(R.drawable.notification_dailyhelp)
                .setContentTitle("Recordatorio de evento")
                .setContentText(description != null ? description : "Tienes un evento pendiente")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(getApplicationContext());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    "event_reminder_channel",
                    "Recordatorios de eventos",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Canal para notificaciones de eventos");
            notificationManager.createNotificationChannel(channel);
        }

        notificationManager.notify((int) currentTimeMillis(), builder.build());

        return Result.success();
    }
}





