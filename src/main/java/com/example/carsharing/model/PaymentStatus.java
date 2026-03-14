package com.example.carsharing.model;

public enum PaymentStatus {
    PENDING,
    COMPLETED,
    FAILED,
    REFUNDED;

    public String getDisplayName() {
        switch (this) {
            case PENDING: return "Ожидает";
            case COMPLETED: return "Оплачено";
            case FAILED: return "Ошибка";
            case REFUNDED: return "Возврат";
            default: return this.name();
        }
    }

    public boolean canBeRefunded() {
        return this == COMPLETED;
    }
}