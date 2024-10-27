package org.example.keycloakdemo.handler;

public class KeycloakException extends RuntimeException {
    public KeycloakException(String message, Throwable cause) {
        super(message, cause);
    }
}