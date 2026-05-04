package com.library.service;

import com.library.model.FeeRecord;
import com.library.repository.FeeTransactionRepository;

import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

/**
 * Service layer for fee history.
 *
 * Delegates all persistence to FeeTransactionRepository (SQL).
 * The in-memory list from Part 2 is replaced by user-filtered DB queries
 * so history persists across sessions and is always scoped to the current user.
 */
public class FeeHistoryService {

    private final FeeTransactionRepository repo = new FeeTransactionRepository();

    /**
     * Returns all fee records for the given user from the database,
     * ordered oldest-first.
     *
     * @param userId the logged-in user's database ID
     */
    public List<FeeRecord> getByUserId(int userId) {
        try {
            return repo.findByUserId(userId);
        } catch (SQLException e) {
            System.err.println("[FeeHistoryService] Failed to load history: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Returns the total number of fee transactions for this user.
     *
     * @param userId the logged-in user's database ID
     */
    public int countByUserId(int userId) {
        try {
            return repo.countByUserId(userId);
        } catch (SQLException e) {
            return 0;
        }
    }
}
