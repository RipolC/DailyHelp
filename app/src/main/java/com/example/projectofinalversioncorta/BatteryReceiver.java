package com.example.projectofinalversioncorta;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

public class BatteryReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Toast.makeText(context, "Batería baja. Conéctate a una fuente de energía.", Toast.LENGTH_LONG).show();
    }
}

