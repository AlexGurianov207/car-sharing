package com.example.carsharing.model;

public enum PaymentMethod {
    CARD,
    CASH,
    APPLE_PAY,
    GOOGLE_PAY;

    public String getDisplayName() {
        switch (this) {
            case CARD: return "Банковская карта";
            case CASH: return "Наличные";
            case APPLE_PAY: return "Apple Pay";
            case GOOGLE_PAY: return "Google Pay";
            default: return this.name();
        }
    }
}