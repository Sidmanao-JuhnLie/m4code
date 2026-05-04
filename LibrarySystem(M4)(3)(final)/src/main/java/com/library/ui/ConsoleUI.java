package com.library.ui;

import java.util.Scanner;

public class ConsoleUI {

    private static final Scanner scanner = new Scanner(System.in);

    public static final String LINE_60  = "=".repeat(60);
    public static final String LINE_62  = "=".repeat(62);
    public static final String DASH_60  = "-".repeat(60);
    public static final String DASH_62  = "-".repeat(62);

    // -----------------------------------------------------------------------
    // Input helpers
    // -----------------------------------------------------------------------

    public static String readLine(String prompt) {
        System.out.print(prompt);
        return scanner.nextLine().trim();
    }

    public static int readInt(String prompt) {
        while (true) {
            System.out.print(prompt);
            String raw = scanner.nextLine().trim();
            try {
                return Integer.parseInt(raw);
            } catch (NumberFormatException e) {
                System.out.println("  [!] Please enter a valid number.");
            }
        }
    }

    public static String readConfirm(String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim().toUpperCase();
            if (input.equals("Y") || input.equals("N")) return input;
            System.out.println("  [!] Please enter Y or N.");
        }
    }

    // -----------------------------------------------------------------------
    // Display helpers
    // -----------------------------------------------------------------------

    public static void printHeader(String title) {
        System.out.println("\n" + LINE_62);
        System.out.printf("  %-58s%n", title);
        System.out.println(LINE_62);
    }

    public static void printSubHeader(String title) {
        System.out.println();
        System.out.println(LINE_60);
        System.out.printf("  %s%n", title);
        System.out.println(LINE_60);
    }

    public static void success(String msg) {
        System.out.println("\n  >> SUCCESS: " + msg);
    }

    public static void error(String msg) {
        System.out.println("\n  [!] " + msg);
    }

    public static void info(String msg) {
        System.out.println("  >> " + msg);
    }

    public static void notice(String msg) {
        System.out.println("  >> NOTICE: " + msg);
    }

    public static void blank() {
        System.out.println();
    }

    public static void pressEnter() {
        System.out.print("\n  Press [Enter] to continue...");
        scanner.nextLine();
    }

    // -----------------------------------------------------------------------
    // Table helpers
    // -----------------------------------------------------------------------

    public static String col(String text, int width) {
        if (text == null) text = "";
        if (text.length() > width) text = text.substring(0, width - 1) + ".";
        return String.format("%-" + width + "s", text);
    }

    public static String colRight(int num, int width) {
        return String.format("%" + width + "d", num);
    }
}
