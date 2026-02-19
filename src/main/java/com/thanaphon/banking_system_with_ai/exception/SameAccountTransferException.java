package com.thanaphon.banking_system_with_ai.exception;

public class SameAccountTransferException extends RuntimeException {

    public SameAccountTransferException() {
        super("Source and destination accounts must be different");
    }
}
