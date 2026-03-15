package com.example.carsharing.model;

public enum UserStatus {
    ACTIVE,
    BLOCKED,
    DELETED;

    public String getDisplayName() {
        switch (this) {
            case ACTIVE: return "Активен";
            case BLOCKED: return "Заблокирован";
            case DELETED: return "Удалён";
            default: return this.name();
        }
    }
}