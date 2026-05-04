package com.library;

import com.library.db.DatabaseConnection;
import com.library.db.DatabaseInitializer;
import com.library.service.AuthService;
import com.library.ui.Dashboard;
import com.library.ui.MainMenu;

public class Main {

    public static void main(String[] args) {
        System.out.println("\n  Initializing system...");
        DatabaseInitializer.initialize();
        System.out.println("  Database ready.\n");

        AuthService authService = new AuthService();
        MainMenu    mainMenu    = new MainMenu(authService);
        Dashboard   dashboard   = new Dashboard(authService);

        // Main application loop
        while (true) {
            boolean loggedIn = mainMenu.show();

            if (!loggedIn) {
                // User chose Exit from main menu
                System.out.println("\n  Thank you for using the Public Library & Media Center System.");
                System.out.println("  Goodbye!\n");
                break;
            }

            // Show dashboard for the logged-in session
            dashboard.show();

            // After logout, loop back to main menu
        }

        DatabaseConnection.close();
        System.exit(0);
    }
}
