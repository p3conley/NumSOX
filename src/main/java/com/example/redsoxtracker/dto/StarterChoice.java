package com.example.redsoxtracker.dto;

import com.example.redsoxtracker.domain.PitcherStatSnapshot;

public class StarterChoice {

    private final String name;
    private final PitcherStatSnapshot snapshot;
    private final boolean confirmed;
    private final boolean predicted;
    private final String note;

    private StarterChoice(String name, PitcherStatSnapshot snapshot, boolean confirmed, boolean predicted, String note) {
        this.name = name;
        this.snapshot = snapshot;
        this.confirmed = confirmed;
        this.predicted = predicted;
        this.note = note;
    }

    public static StarterChoice confirmed(String name, PitcherStatSnapshot snapshot) {
        String note = snapshot == null ? "Confirmed by MLB schedule data; pitcher stats are not loaded yet." : "Confirmed by MLB schedule data.";
        return new StarterChoice(name, snapshot, true, false, note);
    }

    public static StarterChoice predicted(String name, PitcherStatSnapshot snapshot, String note) {
        return new StarterChoice(name, snapshot, false, true, note);
    }

    public static StarterChoice unknown() {
        return new StarterChoice("To be announced", null, false, false, "Probable starter has not been announced and there is not enough recent rotation history to predict it responsibly.");
    }

    public String getName() {
        return name;
    }

    public PitcherStatSnapshot getSnapshot() {
        return snapshot;
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public boolean isPredicted() {
        return predicted;
    }

    public String getNote() {
        return note;
    }
}
