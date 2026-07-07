package com.javascene.gradingfx.exception;

public class ServerDocumentParsingException extends RuntimeException {
    public ServerDocumentParsingException(String message) {
        super(message);
    }

    public ServerDocumentParsingException(String message, Throwable cause) {
        super(message, cause);
    }
}
