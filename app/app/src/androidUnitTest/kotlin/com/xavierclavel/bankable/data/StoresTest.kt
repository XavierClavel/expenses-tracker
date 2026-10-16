package com.xavierclavel.bankable.data

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StoresTest : LocalDataTest() {

    private suspend fun newCategory(name: String, type: String = "EXPENSE"): String {
        CategoryStore.saveCategory(id = null, name = name, type = type, color = "red", icon = "cart")
        return CategoryStore.observe().first().single { it.name == name }.id
    }

    private suspend fun defaultSubcategoryOf(categoryId: String): String =
        CategoryStore.observe().first().single { it.id == categoryId }.subcategories.single { it.isDefault }.id

    private suspend fun newTag(label: String): String {
        TagStore.save(id = null, label = label)
        return TagStore.observe().first().single { it.label == label }.id
    }

    private suspend fun newExpense(
        title: String,
        cents: Long = 1000,
        subcategoryId: String? = null,
        type: String = "EXPENSE",
        tagIds: Set<String> = emptySet(),
        date: String = "2026-10-01",
    ) = ExpenseStore.save(null, title, cents, "EUR", date, subcategoryId, type, tagIds)

    private suspend fun titles(
        categoryId: String? = null,
        query: String? = null,
        minCents: Long? = null,
        maxCents: Long? = null,
        tagId: String? = null,
    ) = ExpenseStore.observe(
        categoryId = categoryId,
        query = query,
        minCents = minCents,
        maxCents = maxCents,
        tagId = tagId,
        limit = 50,
    ).first().map { it.title }.toSet()

    @Test
    fun `a new category comes with its default subcategory`() = runBlocking {
        val id = newCategory("Groceries")

        val subcategory = CategoryStore.observe().first().single().subcategories.single()
        assertTrue(subcategory.isDefault)
        assertEquals("Groceries - Default", subcategory.name)
        assertEquals(setOf("CATEGORY:$id", "SUBCATEGORY:${subcategory.id}"), pending())
    }

    @Test
    fun `expenses are filtered by category, title, amount and tag`() = runBlocking {
        val foodCategory = newCategory("Food")
        val food = defaultSubcategoryOf(foodCategory)
        newCategory("Rent")
        val trip = newTag("Trip")
        newExpense("Carrefour market", cents = 2500, subcategoryId = food, tagIds = setOf(trip))
        newExpense("Bakery", cents = 300, subcategoryId = food)
        newExpense("Train", cents = 9000, tagIds = setOf(trip))

        assertEquals(setOf("Carrefour market", "Bakery"), titles(categoryId = foodCategory))
        assertEquals(setOf("Carrefour market"), titles(query = "MARKET"))
        assertEquals(setOf("Carrefour market"), titles(minCents = 1000, maxCents = 5000))
        assertEquals(setOf("Carrefour market", "Train"), titles(tagId = trip))
    }

    @Test
    fun `search treats wildcards literally`() = runBlocking {
        newExpense("50% off")
        newExpense("500 off")

        assertEquals(setOf("50% off"), titles(query = "50%"))
    }

    @Test
    fun `expenses come newest first, a page at a time`() = runBlocking {
        newExpense("Old", date = "2026-01-01")
        newExpense("New", date = "2026-03-01")
        newExpense("Middle", date = "2026-02-01")

        val page = ExpenseStore.observe(limit = 2).first()
        assertEquals(listOf("New", "Middle"), page.map { it.title })
    }

    @Test
    fun `a tag totals its incomes minus its expenses`() = runBlocking {
        val tag = newTag("Trip")
        newExpense("Hotel", cents = 12000, tagIds = setOf(tag))
        newExpense("Refund", cents = 2050, type = "INCOME", tagIds = setOf(tag))

        val total = TagStore.observe().first().single()
        assertEquals("-99.50", total.total)
        assertEquals(2, total.expenseCount)
    }

    @Test
    fun `a category whose subcategory has expenses can't be deleted`() = runBlocking {
        val category = newCategory("Food")
        newExpense("Bakery", subcategoryId = defaultSubcategoryOf(category))

        val error = runCatching { CategoryStore.deleteCategory(category) }.exceptionOrNull()

        assertTrue(error is CategoryInUseException)
        assertEquals(1, CategoryStore.observe().first().size)
    }

    @Test
    fun `an expense keeps its type when edited`() = runBlocking {
        newExpense("Salary", type = "INCOME")
        val id = ExpenseStore.observe(limit = 10).first().single().id

        ExpenseStore.save(id, "Salary", 1000, "EUR", "2026-10-01", null, "EXPENSE", emptySet())

        assertEquals("INCOME", ExpenseStore.observe(limit = 10).first().single().type)
    }

    @Test
    fun `duplicating copies an expense and its tags onto the new date`() = runBlocking {
        val tag = newTag("Trip")
        newExpense("Hotel", tagIds = setOf(tag), date = "2026-10-01")
        val original = ExpenseStore.observe(limit = 10).first().single()

        ExpenseStore.duplicate(listOf(original.id), "2026-11-01")

        val copy = ExpenseStore.observe(limit = 10).first().first()
        assertEquals("2026-11-01", copy.date)
        assertEquals(listOf(tag), copy.tagIds)
        assertTrue(copy.id != original.id)
    }

    @Test
    fun `deleting a tag removes it from its expenses`() = runBlocking {
        val tag = newTag("Trip")
        newExpense("Hotel", tagIds = setOf(tag))

        TagStore.delete(tag)

        assertTrue(ExpenseStore.observe(limit = 10).first().single().tagIds.isEmpty())
    }

    @Test
    fun `monthly totals add up each subcategory's expenses of a month`() = runBlocking {
        val category = newCategory("Food")
        val food = defaultSubcategoryOf(category)
        newExpense("Bakery", cents = 1000, subcategoryId = food, date = "2026-09-01")
        newExpense("Market", cents = 250, subcategoryId = food, date = "2026-09-30")
        newExpense("Uncategorized", cents = 500, date = "2026-10-01")

        val totals = Reports.observeMonthlyTotals().first().sortedBy { it.month }

        assertEquals(
            listOf(
                MonthlyTotal(monthIndex(2026, 9), food, category, "Food", "EXPENSE", 1250),
                MonthlyTotal(monthIndex(2026, 10), null, null, null, "EXPENSE", 500),
            ),
            totals,
        )
    }

    private suspend fun pending(): Set<String> =
        readLocal { syncQueries.selectPending().executeAsList().map { "${it.type}:${it.id}" }.toSet() }
}
