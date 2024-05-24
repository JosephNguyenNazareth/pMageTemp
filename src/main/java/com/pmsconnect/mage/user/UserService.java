package com.pmsconnect.mage.user;

import org.springframework.stereotype.Service;

@Service
public class UserService {
    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User getUser(String userName) {
        return userRepository.findById(userName).orElseThrow(() -> new IllegalStateException("User with userName " + userName + "does not exist."));
    }

    public String addNewUser(String userName, String password, String role) {
        if (userExist(userName))
            return "User with userName" + userName + "already exist. Please choose another username.";

        User user = new User(userName, password, role);
        userRepository.save(user);
        return "Successfully created new user " + userName;
    }

    public boolean userExist(String userName) {
        return userRepository.existsById(userName);
    }
}
