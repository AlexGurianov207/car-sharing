package com.example.carsharing.model;

public enum CarStatus {
    AVAILABLE,
    RENTED,
    SERVICE;

    public String getDisplayName() {
        switch (this) {
            case AVAILABLE: return "Доступна";
            case RENTED: return "В аренде";
            case SERVICE: return "На обслуживании";
            default: return this.name();
        }
    }
}