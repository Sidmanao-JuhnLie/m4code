package com.library.ui;

import com.library.service.AuthService;
import com.library.service.AuthService.LoginResult;
import com.library.service.AuthService.RegisterResult;

public class MainMenu {

    private final AuthService authService;

    public MainMenu(AuthService authService) {
        this.authService = authService;
    }

    // Returns false when user selects Exit
    public boolean show() {
        while (true) {
            printWelcome();
            String choice = ConsoleUI.readLine("  Enter choice: ");

            switch (choice) {
                case "1" -> handleCreateAccount();
                case "2" -> { if (handleLogin()) return true; }
                case "3" -> { return false; }
                default  -> ConsoleUI.error("Invalid choice. Please enter 1, 2, or 3.");
            }
        }
    }

    // -----------------------------------------------------------------------

    private void printWelcome() {
        System.out.println("\n" + ConsoleUI.LINE_62);
        System.out.println("      PUBLIC LIBRARY & MEDIA CENTER SYSTEM");
        System.out.println(ConsoleUI.LINE_62);
        System.out.println();
        System.out.println("    Welcome!");
        System.out.println();
        System.out.println("    [1] Create Account");
        System.out.println("    [2] Login");
        System.out.println("    [3] Exit");
        System.out.println();
    }

    private void handleCreateAccount() {
        System.out.println("\n" + ConsoleUI.LINE_62);
        System.out.println("  Create your Account!");
        System.out.println(ConsoleUI.LINE_62);
        System.out.println();

        String name = ConsoleUI.readLine("  Enter Name     : ");
        if (name.isBlank()) { ConsoleUI.error("Name cannot be empty."); ConsoleUI.pressEnter(); return; }

        String email = ConsoleUI.readLine("  Enter Email    : ");
        if (!email.contains("@")) { ConsoleUI.error("Invalid email format."); ConsoleUI.pressEnter(); return; }

        String password = ConsoleUI.readLine("  Enter Password : ");
        if (password.length() < 4) { ConsoleUI.error("Password must be at least 4 characters."); ConsoleUI.pressEnter(); return; }

        System.out.println("\n  " + ConsoleUI.DASH_60);

        RegisterResult result = authService.register(email, password, name);

        switch (result) {
            case SUCCESS -> {
                System.out.println("  Account Created Successfully!");
                System.out.printf ("  Member ID : %s%n", authService.getCurrentUser().getMemberId());
                System.out.printf ("  Welcome, %s!%n", authService.getCurrentUser().getDisplayName());
                ConsoleUI.pressEnter();
            }
            case EMAIL_TAKEN -> {
                ConsoleUI.error("That email is already registered. Please log in instead.");
                ConsoleUI.pressEnter();
            }
            default -> {
                ConsoleUI.error("An error occurred. Please try again.");
                ConsoleUI.pressEnter();
            }
        }
    }

    private boolean handleLogin() {
        System.out.println("\n" + ConsoleUI.LINE_62);
        System.out.println("  Login to Your Account");
        System.out.println(ConsoleUI.LINE_62);
        System.out.println();

        String email    = ConsoleUI.readLine("  Enter Email    : ");
        String password = ConsoleUI.readLine("  Enter Password : ");

        System.out.println("\n  " + ConsoleUI.DASH_60);

        LoginResult result = authService.login(email, password);

        switch (result) {
            case SUCCESS -> {
                System.out.println("  Log In Successful");
                System.out.printf ("  Welcome back, %s!%n",
                    authService.getCurrentUser().getDisplayName());
                ConsoleUI.pressEnter();
                return true;
            }
            case INVALID_CREDENTIALS -> {
                ConsoleUI.error("Incorrect email or password.");
                ConsoleUI.pressEnter();
                return false;
            }
            default -> {
                ConsoleUI.error("An error occurred. Please try again.");
                ConsoleUI.pressEnter();
                return false;
            }
        }
    }
}
