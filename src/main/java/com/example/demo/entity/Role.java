package com.example.demo.entity;

public enum Role {
    CENTRAL, BRANCH;

    public static boolean contiene(String valor) {
        for (Role r : values()) {
            if (r.name().equalsIgnoreCase(valor)) return true;
        }
        return false;
    }
}
