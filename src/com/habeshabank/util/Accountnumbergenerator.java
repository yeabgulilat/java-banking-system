package com.habeshabank.util;

import java.time.Year;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Generates unique, formatted Habesha Bank account numbers.
 *
 * Format:  ETH-{YEAR}-{SEQUENCE:05d}
 * Example: ETH-2024-00142
 *
 * The sequence counter is in-memory for now; a database-backed sequence
 * (AUTO_INCREMENT or SQLite ROWID) will replace it in the persistence phase.
 */
public final class AccountNumberGenerator {

    private static final AtomicLong sequence = new AtomicLong(100L);

    private AccountNumberGenerator() { /* utility class */ }

    /** Returns a new unique account number string. */
    public static String next() {
        int  year = Year.now().getValue();
        long seq  = sequence.incrementAndGet();
        return String.format("ETH-%d-%05d", year, seq);
    }

    /**
     * Sets the seed (used when loading existing records from the database
     * to avoid collisions with previously issued numbers).
     */
    public static void seed(long lastUsedSequence) {
        sequence.set(lastUsedSequence);
    }
}
