package com.example.projectofinalversioncorta;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Looper;
import android.util.Log;

import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.*;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

public class LocationHelper {
    private final Context context;
    private final Consumer<String> callback;
    private final FusedLocationProviderClient fusedLocationClient;

    public LocationHelper(Context context, Consumer<String> callback) {
        this.context = context;
        this.callback = callback;
        this.fusedLocationClient = LocationServices.getFusedLocationProviderClient(context);
    }

    @SuppressLint("MissingPermission")
    public void requestCity() {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            callback.accept(null);
            return;
        }

        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                getCityFromLocation(location);
            } else {
                LocationRequest request = LocationRequest.create()
                        .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                        .setInterval(5000)
                        .setFastestInterval(2000)
                        .setNumUpdates(1);

                fusedLocationClient.requestLocationUpdates(request, new LocationCallback() {
                    @Override
                    public void onLocationResult(LocationResult locationResult) {
                        fusedLocationClient.removeLocationUpdates(this);
                        Location loc = locationResult.getLastLocation();
                        if (loc != null) getCityFromLocation(loc);
                        else callback.accept(null);
                    }
                }, Looper.getMainLooper());
            }
        });
    }

    private void getCityFromLocation(Location location) {
        Geocoder geocoder = new Geocoder(context, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
            if (!addresses.isEmpty()) {
                String city = addresses.get(0).getLocality();
                callback.accept(city);
            } else {
                callback.accept(null);
            }
        } catch (IOException e) {
            Log.e("LOCATION", "Error al obtener la ciudad", e);
            callback.accept(null);
        }
    }
}
