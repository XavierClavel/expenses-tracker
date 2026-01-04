package com.xavierclavel.controllers

import com.xavierclavel.ApplicationTest
import com.xavierclavel.dtos.CategoryIn
import com.xavierclavel.dtos.ExpenseIn
import com.xavierclavel.dtos.SubcategoryIn
import com.xavierclavel.enums.ExpenseType
import com.xavierclavel.utils.SUBCATEGORY_URL
import com.xavierclavel.utils.assertSubcategoryDoesNotExist
import com.xavierclavel.utils.assertSubcategoryExists
import com.xavierclavel.utils.createCategory
import com.xavierclavel.utils.createExpense
import com.xavierclavel.utils.createSubcategory
import com.xavierclavel.utils.deleteSubcategory
import com.xavierclavel.utils.getSubcategory
import com.xavierclavel.utils.listSubcategoriesByUser
import com.xavierclavel.utils.updateSubcategory
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import kotlin.test.assertEquals

class SubcategoryControllerTest: ApplicationTest() {

    val categoryInTemplate = CategoryIn(name = "Groceries", type = ExpenseType.EXPENSE, color = "", icon = "")

    val subcategoryTemplate = SubcategoryIn(
        name = "Supermarket",
        type = ExpenseType.EXPENSE,
        icon = "",
        parentCategory = -1,
    )

    val expense = ExpenseIn(
        title = "Carrefour",
        amount = BigDecimal("25.00"),
        currency = "eur",
        date = LocalDate.parse("2020-06-06"),
        categoryId = null,
        type = ExpenseType.EXPENSE,
    )
    @Test
    fun `get subcategory`() = runTestAsUser {
        val category = client.createCategory(categoryInTemplate)
        val subcategory = client.createSubcategory(subcategoryTemplate.copy(parentCategory = category.id))

        assertEquals(subcategoryTemplate.name, subcategory.name)
    }

    @Test
    fun `create subcategory`() = runTestAsUser {
        val result = client.listSubcategoriesByUser()
        assertEquals(0, result.size)

        val category = client.createCategory(categoryInTemplate)
        val subcategory = client.createSubcategory(subcategoryTemplate.copy(parentCategory = category.id))
        val result2 = client.listSubcategoriesByUser()
        assertEquals(2, result2.size)
    }

    @Test
    fun `edit subcategory`() = runTestAsUser {
        val category = client.createCategory(categoryInTemplate)
        val input = subcategoryTemplate.copy(parentCategory = category.id)
        val subcategory = client.createSubcategory(input)

        val result = client.getSubcategory(subcategory.id)
        assertEquals(subcategoryTemplate.name, result.name)
        val newName = "Markets"

        client.updateSubcategory(subcategory.id, input.copy(name = newName))
        val result2 = client.getSubcategory(subcategory.id)
        assertEquals(newName, result2.name)
    }

    @Test
    fun `list subcategories`() = runTest{
        runAsUser1 {
            val category = client.createCategory(categoryInTemplate.copy(name = "Groceries"))
            val subcategory = client.createSubcategory(subcategoryTemplate.copy(parentCategory = category.id))
        }
        runAsUser2 {
            client.createCategory(categoryInTemplate.copy(name = "Trips"))
            client.createCategory(categoryInTemplate.copy(name = "Restaurants"))
        }

        runAsUser1 {
            val result = client.listSubcategoriesByUser()
            assertEquals(2, result.size)
            assertEquals(subcategoryTemplate.name, result[1].name)
        }

        runAsUser2 {
            val result = client.listSubcategoriesByUser()
            assertEquals(2, result.size)
        }
    }

    @Test
    fun `delete subcategory`() = runTestAsUser {
        val category = client.createCategory(categoryInTemplate)
        val subcategory = client.createSubcategory(subcategoryTemplate.copy(parentCategory = category.id))
        client.assertSubcategoryExists(subcategory.id)
        client.deleteSubcategory(subcategory.id)
        client.assertSubcategoryDoesNotExist(subcategory.id)
    }

    @Test
    fun `cannot delete used subcategory`() = runTestAsUser {
        val category = client.createCategory(categoryInTemplate)
        val subcategory = client.createSubcategory(subcategoryTemplate.copy(parentCategory = category.id))

        client.createExpense(expense.copy(categoryId = subcategory.id))
        client.delete("${SUBCATEGORY_URL}/${subcategory.id}").apply {
            assertEquals(HttpStatusCode.Companion.Forbidden, status)
        }
    }

    @Test
    fun `user cannot edit subcategory they do not own`() = runTest {
        var subcategoryId: Long = 0
        val input =
        runAsUser1 {
            val categoryId = client.createCategory(categoryInTemplate.copy(name = "Groceries")).id
            subcategoryId = client.createSubcategory(subcategoryTemplate.copy(parentCategory = categoryId)).id
        }

        runAsUser2 {
            val categoryId = client.createCategory(categoryInTemplate.copy(name = "Groceries")).id
            client.put("${SUBCATEGORY_URL}/$subcategoryId"){
                contentType(ContentType.Application.Json)
                setBody(subcategoryTemplate.copy(parentCategory = categoryId))
            }.apply {
                assertEquals(HttpStatusCode.Companion.Forbidden, status)
            }
        }
    }

    @Test
    fun `user cannot delete subcategory they do not own`() = runTest {
        var subcategoryId: Long = 0
        runAsUser1 {
            val categoryId = client.createCategory(categoryInTemplate.copy(name = "Groceries")).id
            subcategoryId = client.createSubcategory(subcategoryTemplate.copy(parentCategory = categoryId)).id
        }

        runAsUser2 {
            client.delete("${SUBCATEGORY_URL}/$subcategoryId").apply {
                assertEquals(HttpStatusCode.Companion.Forbidden, status)
            }
        }
    }


    @Test
    fun `users cannot see each others subcategories`() = runTest {
        var subcategoryId: Long = 0
        runAsUser1 {
            val categoryId = client.createCategory(categoryInTemplate).id
            subcategoryId = client.createSubcategory(subcategoryTemplate.copy(parentCategory = categoryId)).id
        }

        runAsUser2 {
            client.get("${SUBCATEGORY_URL}/$subcategoryId").apply {
                assertEquals(HttpStatusCode.Companion.Forbidden, status)
            }
        }
    }

}