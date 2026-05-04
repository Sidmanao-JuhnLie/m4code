package com.library.ui;

import com.library.model.FeeRecord;
import com.library.model.User;
import com.library.service.FeeHistoryService;

import java.util.List;

public class FeeHistoryUI {

    private final FeeHistoryService feeHistoryService;
    private final User              user;

    public FeeHistoryUI(FeeHistoryService feeHistoryService, User user) {
        this.feeHistoryService = feeHistoryService;
        this.user              = user;
    }

    public void show() {
        ConsoleUI.printHeader("FEE HISTORY");
        System.out.println();
        System.out.printf("  Member : %s  [%s]%n", user.getDisplayName(), user.getMemberId());

        // Fetch records from DB filtered by this user's ID
        List<FeeRecord> records = feeHistoryService.getByUserId(user.getId());

        System.out.printf("  Total Transactions: %d%n%n", records.size());

        if (records.isEmpty()) {
            System.out.println("  No fee transactions found for your account.");
            System.out.println("  Borrow or return a book to generate entries.");
            ConsoleUI.pressEnter();
            return;
        }

        for (int i = 0; i < records.size(); i++) {
            printRecord(i + 1, records.get(i));
        }

        ConsoleUI.pressEnter();
    }

    // -----------------------------------------------------------------------

    private void printRecord(int index, FeeRecord r) {
        String typeLabel = r.getType() == FeeRecord.TransactionType.BORROW ? "BORROW" : "RETURN";

        System.out.println("  " + ConsoleUI.DASH_60);
        System.out.printf("  [%d] %s  |  %s  |  %s%n",
                index, r.getBookTitle(), typeLabel, r.getTimestamp());
        System.out.println("  " + ConsoleUI.DASH_60);
        System.out.printf("    %-28s : $%.2f%n", "Base Price",            r.getBasePrice());

        if (r.getReservationFee() > 0) {
            System.out.printf("    %-28s : $%.2f%n", "Reservation Fee",   r.getReservationFee());
        }
        if (r.getLateFee() > 0) {
            System.out.printf("    %-28s : $%.2f%n", "Late Fee",          r.getLateFee());
        }

        boolean isReturn = r.getType() == FeeRecord.TransactionType.RETURN;
        double taxAmount  = isReturn ? 0.0 : r.getTaxAmount();
        System.out.printf("    %-28s : $%.2f%n", "Tax Amount (12% VAT)",  taxAmount);
        System.out.printf("    %-28s : $%.2f%n", "Total (VAT Inclusive)", r.getTotalVatInclusive());
        System.out.println();
    }
}
