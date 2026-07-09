package com.xavierclavel.enums

/**
 * How an account's balance is decomposed into principal vs earnings.
 *
 * - [CONTRIBUTIONS]: you record deposits/withdrawals (IN/OUT); interest is inferred
 *   as `balance − contributions`. Right for market-valued accounts (e.g. PEA) where
 *   the value drifts and can only be known by reporting the balance.
 * - [INTEREST]: you record the known earnings (INTEREST/FEE); contributions are
 *   inferred as `balance − interest`. Right for fixed-rate/fee accounts (e.g. Livret
 *   A) where the bank tells you the exact interest and you needn't itemise deposits.
 */
enum class AccountTracking {
    CONTRIBUTIONS,
    INTEREST,
}
