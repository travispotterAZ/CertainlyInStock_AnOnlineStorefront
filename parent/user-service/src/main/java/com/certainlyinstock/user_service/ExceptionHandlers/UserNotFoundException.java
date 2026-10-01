package com.certainlyinstock.user_service.ExceptionHandlers;

public class UserNotFoundException extends RuntimeException {

    public UserNotFoundException(Long id) {
        super("User not found with id " + id);
    }
}
