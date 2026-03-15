package com.example.carsharing.model;

public enum RentalStatus {
    ACTIVE,
    COMPLETED,
    CANCELLED;

    public String getDisplayName() {
        switch (this) {
            case ACTIVE: return "Активна";
            case COMPLETED: return "Завершена";
            case CANCELLED: return "Отменена";
            default: return this.name();
        }
    }

    public boolean isPayable() {
        return this == COMPLETED;
    }

    public boolean isCancellable() {
        return this == ACTIVE;
    }
}