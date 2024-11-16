package com.nortal.activedirectoryrestapi.exceptions;

import lombok.Getter;

import java.time.OffsetDateTime;

@Getter
public class ADCommandExecutionException extends Exception {
    private final OffsetDateTime timestamp;
    public ADCommandExecutionException(String message, OffsetDateTime timestamp) {
        super(message);
        this.timestamp = timestamp;
    }
}
