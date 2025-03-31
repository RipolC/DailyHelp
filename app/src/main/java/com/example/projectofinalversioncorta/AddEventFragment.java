package com.example.projectofinalversioncorta;

import android.app.TimePickerDialog;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Calendar;
import java.util.Locale;

public class AddEventFragment extends Fragment {

    private EditText etEventDescription;
    private Button btnPickTime, btnSaveEvent, btnCancel;
    private TextView tvSelectedTime;
    private long selectedDateMillis;
    private int selectedHour = -1, selectedMinute = -1;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_add_event, container, false);

        etEventDescription = view.findViewById(R.id.etEventDescription);
        btnPickTime = view.findViewById(R.id.btnPickTime);
        btnSaveEvent = view.findViewById(R.id.btnSaveEvent);
        tvSelectedTime = view.findViewById(R.id.tvSelectedTime);
        btnCancel = view.findViewById(R.id.btnCancel);

        if (getArguments() != null) {
            selectedDateMillis = getArguments().getLong("selectedDateMillis", -1);
        }

        btnPickTime.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            int hour = calendar.get(Calendar.HOUR_OF_DAY);
            int minute = calendar.get(Calendar.MINUTE);

            TimePickerDialog timePickerDialog = new TimePickerDialog(getContext(), (view1, hourOfDay, minute1) -> {
                selectedHour = hourOfDay;
                selectedMinute = minute1;
                tvSelectedTime.setText(String.format(Locale.getDefault(), "%02d:%02d", selectedHour, selectedMinute));
            }, hour, minute, true);
            timePickerDialog.show();
        });

        btnSaveEvent.setOnClickListener(v -> {
            String description = etEventDescription.getText().toString().trim();
            if (description.isEmpty() || selectedHour == -1 || selectedMinute == -1) {
                Toast.makeText(getContext(), "Debes ingresar una descripción y seleccionar la hora", Toast.LENGTH_SHORT).show();
                return;
            }

            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(selectedDateMillis);
            calendar.set(Calendar.HOUR_OF_DAY, selectedHour);
            calendar.set(Calendar.MINUTE, selectedMinute);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);
            long eventDateTimeMillis = calendar.getTimeInMillis();

            EventDatabase db = new EventDatabase(getContext());
            db.addEvent(eventDateTimeMillis, selectedHour, selectedMinute, description);

            Toast.makeText(getContext(), R.string.Saved_event, Toast.LENGTH_SHORT).show();
            getParentFragmentManager().popBackStack();
        });

        btnCancel.setOnClickListener(v -> getParentFragmentManager().popBackStack());

        return view;
    }
}



