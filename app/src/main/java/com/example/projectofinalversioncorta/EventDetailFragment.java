package com.example.projectofinalversioncorta;

import android.app.AlertDialog;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.stream.Collectors;

public class EventDetailFragment extends Fragment {

    private static final String ARG_EVENT_ID = "event_id";

    public static EventDetailFragment newInstance(int eventId) {
        EventDetailFragment fragment = new EventDetailFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_EVENT_ID, eventId);
        fragment.setArguments(args);
        return fragment;
    }

    private TextView tvDescription, tvTime, tvUbicacion, tvEtiquetas, tvParticipantes;
    private Button btnDeleteEvent;

    private EventDatabase db;
    private int eventId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_event_detail, container, false);

        tvDescription = view.findViewById(R.id.tvEventDescription);
        tvTime = view.findViewById(R.id.tvEventTime);
        tvUbicacion = view.findViewById(R.id.tvUbicacion);
        tvEtiquetas = view.findViewById(R.id.tvEtiquetas);
        tvParticipantes = view.findViewById(R.id.tvParticipantes);
        btnDeleteEvent = view.findViewById(R.id.btnDeleteEvent);

        db = new EventDatabase(getContext());

        if (getArguments() != null) {
            eventId = getArguments().getInt(ARG_EVENT_ID);
            loadEventDetails(eventId);
        }

        btnDeleteEvent.setOnClickListener(v -> {
            new AlertDialog.Builder(getContext())
                    .setTitle(R.string.delete_event)
                    .setMessage(R.string.confirm_delete)
                    .setPositiveButton(R.string.yes, (dialog, which) -> {
                        db.deleteEvent(eventId);
                        Toast.makeText(getContext(), R.string.deleted_event, Toast.LENGTH_SHORT).show();
                        getParentFragmentManager().popBackStack();
                    })
                    .setNegativeButton(R.string.no, null)
                    .show();
        });

        Button btnBack = view.findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> {
            requireActivity().getSupportFragmentManager().popBackStack();
        });


        return view;
    }

    private void loadEventDetails(int eventId) {
        Event event = db.getEventById(eventId);
        if (event != null) {
            tvDescription.setText(getString(R.string.description) + event.getDescription());
            tvTime.setText(getString(R.string.hour) + event.getTime());

            if (event.getUbicacion() != null) {
                tvUbicacion.setText(getString(R.string.location) + event.getUbicacion().getNombre());
            }

            String etiquetas = TextUtils.join(", ", event.getEtiquetas()
                    .stream().map(Tag::getNombre).collect(Collectors.toList()));
            tvEtiquetas.setText(getString(R.string.tags) + etiquetas);

            String participantes = TextUtils.join(", ", event.getParticipantes()
                    .stream().map(Participant::getNombre).collect(Collectors.toList()));
            tvParticipantes.setText(getString(R.string.participants) + participantes);
        }
    }
}
