package com.example.projectofinalversioncorta;

import java.util.Locale;

import java.util.List;

public class Event {
    private int id;
    private long dateMillis;
    private int hour;
    private int minute;
    private String description;
    private Ubication ubicacion;
    private List<Tag> etiquetas;
    private List<Participant> participantes;

    // Constructor básico
    public Event(int id, long dateMillis, int hour, int minute, String description) {
        this.id = id;
        this.dateMillis = dateMillis;
        this.hour = hour;
        this.minute = minute;
        this.description = description;
    }

    // Constructor completo
    public Event(int id, long dateMillis, int hour, int minute, String description,
                 Ubication ubicacion, List<Tag> etiquetas, List<Participant> participantes) {
        this.id = id;
        this.dateMillis = dateMillis;
        this.hour = hour;
        this.minute = minute;
        this.description = description;
        this.ubicacion = ubicacion;
        this.etiquetas = etiquetas;
        this.participantes = participantes;
    }

    // Getters y Setters
    public int getId() {
        return id;
    }

    public long getDateMillis() {
        return dateMillis;
    }

    public int getHour() {
        return hour;
    }

    public int getMinute() {
        return minute;
    }

    public String getDescription() {
        return description;
    }

    public Ubication getUbicacion() {
        return ubicacion;
    }

    public List<Tag> getEtiquetas() {
        return etiquetas;
    }

    public List<Participant> getParticipantes() {
        return participantes;
    }

    public void setUbicacion(Ubication ubicacion) {
        this.ubicacion = ubicacion;
    }

    public void setEtiquetas(List<Tag> etiquetas) {
        this.etiquetas = etiquetas;
    }

    public void setParticipantes(List<Participant> participantes) {
        this.participantes = participantes;
    }

    public String getTime() {
        return String.format("%02d:%02d", hour, minute);
    }
}






