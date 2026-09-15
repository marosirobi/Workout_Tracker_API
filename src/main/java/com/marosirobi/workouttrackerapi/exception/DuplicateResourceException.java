package com.marosirobi.workouttrackerapi.exception;

public class DuplicateResourceException extends RuntimeException {
    public DuplicateResourceException(final String message){
        super(message);
    }
}
