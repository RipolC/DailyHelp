package com.example.projectofinalversioncorta;

import android.app.AlertDialog;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CalendarView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CalendarFragment extends Fragment {

    private CalendarView calendarView;
    private Button btnAddEvent, btnDeleteEvent;
    private ListView lvEventList;
    private EventDatabase db;
    private long selectedDateMillis;
    private Event selectedEvent = null;
    private List<Event> events = new ArrayList<>();
    private ArrayAdapter<String> adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_calendar, container, false);

        calendarView = view.findViewById(R.id.calendarView);
        btnAddEvent = view.findViewById(R.id.btnAddEvent);
        lvEventList = view.findViewById(R.id.lvEventList);

        db = new EventDatabase(getContext());

        calendarView.setOnDateChangeListener((view1, year, month, dayOfMonth) -> {
            Calendar calendar = Calendar.getInstance();
            calendar.set(year, month, dayOfMonth);
            selectedDateMillis = calendar.getTimeInMillis();
            loadEvents();
        });

        btnAddEvent.setOnClickListener(v -> {
            AddEventFragment addEventFragment = new AddEventFragment();
            Bundle bundle = new Bundle();
            bundle.putLong("selectedDateMillis", selectedDateMillis);
            addEventFragment.setArguments(bundle);

            FragmentTransaction transaction = getParentFragmentManager().beginTransaction();
            transaction.replace(R.id.fragment_container, addEventFragment);
            transaction.addToBackStack(null);
            transaction.commit();
        });

        lvEventList.setOnItemClickListener((parent, view1, position, id) -> {
            selectedEvent = events.get(position);
            btnDeleteEvent.setVisibility(View.VISIBLE);
        });

        lvEventList.setOnItemClickListener((parent, view1, position, id) -> {
            selectedEvent = events.get(position);

            // Abrir EventDetailFragment pasando el ID del evento
            EventDetailFragment detailFragment = EventDetailFragment.newInstance(selectedEvent.getId());

            getParentFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, detailFragment)
                    .addToBackStack(null)
                    .commit();
        });



        return view;
    }

    private void loadEvents() {
        events = db.getEventsByDate(selectedDateMillis);
        List<String> eventDescriptions = new ArrayList<>();
        for (Event event : events) {
            eventDescriptions.add(event.getDescription() + " - " + event.getTime());
        }
        adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_list_item_1, eventDescriptions);
        lvEventList.setAdapter(adapter);
    }



}





