package com.marosirobi.workouttrackerapi.utils;

public final class PasswordPolicy {

    public static final int MIN_LENGTH = 8;
    public static final int MAX_LENGTH = 128;

    // Legalább egy betű, egy számjegy és egy speciális karakter
    public static final String PATTERN = "^(?=.*\\p{L})(?=.*\\d)(?=.*[^\\p{L}\\d\\s]).*$";
    public static final String MESSAGE =
            "Password must contain at least one letter, one number and one special character";

    private PasswordPolicy() { }
}
