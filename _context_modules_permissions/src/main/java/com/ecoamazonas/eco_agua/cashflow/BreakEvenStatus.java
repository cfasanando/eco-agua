package com.ecoamazonas.eco_agua.cashflow;

/**
 * Simple status for break-even analysis.
 */
public enum BreakEvenStatus {
    BEFORE_BREAK_EVEN, // Units sold < break-even units
    AT_BREAK_EVEN,     // Units sold == break-even units
    AFTER_BREAK_EVEN   // Units sold > break-even units
}
