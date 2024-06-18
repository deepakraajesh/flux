package com.unbxd.auth.exception;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public class AuthSystemException extends Exception{

    public AuthSystemException(String message) {
        super(message);
    }
}

