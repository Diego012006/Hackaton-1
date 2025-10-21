package piatto_pc1.domain;

public enum Role {ROLE_1, ROLE_2;

    public static boolean contiene(String valor) {
        for (Role e : values()) {
            if (e.name().equalsIgnoreCase(valor)) {
                return true;}}
        return false;}}