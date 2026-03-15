package com.example.carsharing.model;

public enum ServiceCategory {
    SAFETY,
    COMFORT,
    EQUIPMENT,
    INSURANCE;

    public String getDisplayName() {
        switch (this) {
            case SAFETY: return "Безопасность";
            case COMFORT: return "Комфорт";
            case EQUIPMENT: return "Оборудование";
            case INSURANCE: return "Страховка";
            default: return this.name();
        }
    }

    public String getDescription() {
        switch (this) {
            case SAFETY: return "Услуги для безопасности";
            case COMFORT: return "Услуги для комфорта";
            case EQUIPMENT: return "Дополнительное оборудование";
            case INSURANCE: return "Страховые продукты";
            default: return "";
        }
    }
}