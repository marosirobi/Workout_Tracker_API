package com.marosirobi.workouttrackerapi.exception;

import lombok.Getter;

@Getter
public class DuplicateResourceException extends RuntimeException {

    private final String code;

    public DuplicateResourceException(final String message, final String code){
        super(message);
        this.code = code;
    }
}
