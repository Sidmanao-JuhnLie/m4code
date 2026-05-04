package com.library.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Encapsulates the fee breakdown for a single borrow or return transaction.
 *
 * Two constructors exist:
 *   - new transaction  : timestamp is set to now (used in LibraryPaymentProcessor)
 *   - loaded from DB   : timestamp is passed as a string (used in FeeTransactionRepository)
 *
 * VAT derivation (from PaymentFramework.applyVAT logic):
 *   totalVatInclusive = discountedBase * 1.12
 *   taxAmount         = totalVatInclusive - (totalVatInclusive / 1.12)
 */
public class FeeRecord {

    public enum TransactionType { BORROW, RETURN }

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    // -----------------------------------------------------------------------
    private final String          bookTitle;
    private final TransactionType type;
    private final double          basePrice;
    private final double          reservationFee;
    private final double          lateFee;
    private final double          totalVatInclusive;
    private final String          timestamp;        // stored as String for DB compatibility

    // -----------------------------------------------------------------------
    // Constructor A: fresh transaction – timestamp = now
    // -----------------------------------------------------------------------
    public FeeRecord(String bookTitle,
                     TransactionType type,
                     double basePrice,
                     double reservationFee,
                     double lateFee,
                     double totalVatInclusive) {
        this(bookTitle, type, basePrice, reservationFee, lateFee, totalVatInclusive,
             LocalDateTime.now().format(FORMATTER));
    }

    // -----------------------------------------------------------------------
    // Constructor B: loaded from DB – timestamp supplied by caller
    // -----------------------------------------------------------------------
    public FeeRecord(String bookTitle,
                     TransactionType type,
                     double basePrice,
                     double reservationFee,
                     double lateFee,
                     double totalVatInclusive,
                     String timestamp) {
        this.bookTitle         = bookTitle;
        this.type              = type;
        this.basePrice         = basePrice;
        this.reservationFee    = reservationFee;
        this.lateFee           = lateFee;
        this.totalVatInclusive = totalVatInclusive;
        this.timestamp         = timestamp;
    }

    // -----------------------------------------------------------------------
    // Derived values
    // -----------------------------------------------------------------------

    /** Tax portion: Total - (Total / 1.12), per the task formula. */
    public double getTaxAmount() {
        return totalVatInclusive - (totalVatInclusive / 1.12);
    }

    // -----------------------------------------------------------------------
    // Getters
    // -----------------------------------------------------------------------
    public String          getBookTitle()         { return bookTitle; }
    public TransactionType getType()              { return type; }
    public double          getBasePrice()         { return basePrice; }
    public double          getReservationFee()    { return reservationFee; }
    public double          getLateFee()           { return lateFee; }
    public double          getTotalVatInclusive() { return totalVatInclusive; }
    public String          getTimestamp()         { return timestamp; }
}
