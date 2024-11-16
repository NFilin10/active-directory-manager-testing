package com.nortal.activedirectoryrestapi.misc;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@RequiredArgsConstructor
@Getter
@Setter
public class ErrorObject {
    private final String errorMessage;
    private final int httpStatusCode;
    private final String timestamp;
}
