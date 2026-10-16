package com.xavierclavel.enums

/**
 * Kinds of rows a device keeps in sync with the backend.
 *
 * @property table The table holding rows of this kind.
 */
enum class SyncEntityType(val table: String) {
    CATEGORY("categories"),
    SUBCATEGORY("subcategories"),
    TAG("tags"),
    EXPENSE("expenses"),
    RECURRING_EXPENSE("recurring_expenses"),
}
