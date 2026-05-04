package com.library.service;

import com.library.model.User;
import com.library.repository.UserRepository;

import java.sql.SQLException;

public class AuthService {

    private final UserRepository userRepo = new UserRepository();

    public enum RegisterResult { SUCCESS, EMAIL_TAKEN, ERROR }
    public enum LoginResult    { SUCCESS, INVALID_CREDENTIALS, ERROR }

    private User loggedInUser;

    // -----------------------------------------------------------------------

    public RegisterResult register(String email, String password, String name) {
        try {
            if (userRepo.emailExists(email)) return RegisterResult.EMAIL_TAKEN;
            loggedInUser = userRepo.createUser(email, password, name);
            return RegisterResult.SUCCESS;
        } catch (SQLException e) {
            System.err.println("Register error: " + e.getMessage());
            return RegisterResult.ERROR;
        }
    }

    public LoginResult login(String email, String password) {
        try {
            User user = userRepo.findByEmail(email);
            if (user == null || !user.getPassword().equals(password)) {
                return LoginResult.INVALID_CREDENTIALS;
            }
            loggedInUser = user;
            return LoginResult.SUCCESS;
        } catch (SQLException e) {
            System.err.println("Login error: " + e.getMessage());
            return LoginResult.ERROR;
        }
    }

    public void logout() {
        loggedInUser = null;
    }

    public User getCurrentUser() {
        return loggedInUser;
    }

    public boolean isLoggedIn() {
        return loggedInUser != null;
    }
}
