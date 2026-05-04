package com.library.service;

public abstract class PaymentFramework {

    protected double applyVAT(double amount) {
        double vatRate = 0.12;
        return amount * (1 + vatRate);
    }

    protected double applyDiscount(double amount, double discountPercentage) {
        if (discountPercentage < 0 || discountPercentage > 100) {
            throw new IllegalArgumentException("Invalid discount percentage");
        }
        return amount - (amount * (discountPercentage / 100.0));
    }

    protected abstract boolean validatePayment(double totalAmount);

    protected abstract void finalizeTransaction(double totalAmount);

    public void processInvoice(double baseAmount, double discountPercentage) {
        System.out.println("--- Starting Invoice Processing ---");
        System.out.println("Base Amount: $" + String.format("%.2f", baseAmount));

        double discountedAmount = applyDiscount(baseAmount, discountPercentage);
        if (discountPercentage > 0) {
            System.out.println("Amount after " + discountPercentage
                    + "% discount: $" + String.format("%.2f", discountedAmount));
        }

        double finalAmount = applyVAT(discountedAmount);
        System.out.println("Total Amount (12% VAT Inclusive): $" + String.format("%.2f", finalAmount));

        if (validatePayment(finalAmount)) {
            finalizeTransaction(finalAmount);
            System.out.println("--- Invoice Processing Successful ---");
        } else {
            System.out.println("--- Invoice Processing Failed: Payment Validation Rejected ---");
        }
    }
}
