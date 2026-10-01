package com.certainlyinstock.user_service.Service;

import java.util.List;

import com.certainlyinstock.user_service.Entity.User;

public interface UserService {

    User createUser(User user);

    User getUserById(Long id);

    List<User> getAllUsers();

    User updateUser(Long id, User user);

    void deleteUser(Long id);
}
