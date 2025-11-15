package com.xavierclavel.controllers

import com.xavierclavel.ApplicationTest
import com.xavierclavel.dtos.CategoryIn
import com.xavierclavel.utils.CATEGORY_URL
import com.xavierclavel.utils.assertCategoryDoesNotExist
import com.xavierclavel.utils.assertCategoryExists
import com.xavierclavel.utils.createCategory
import com.xavierclavel.utils.deleteCategory
import com.xavierclavel.utils.getCategory
import com.xavierclavel.utils.getMe
import com.xavierclavel.utils.listCategoriesByUser
import com.xavierclavel.utils.updateCategory
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CategoryControllerTest: ApplicationTest() {

    @Test
    fun `get category`() = runTestAsUser {
        val category = CategoryIn(name = "Groceries")
        val result = client.createCategory(category)
        assertEquals(category.name, result.name)
    }

    @Test
    fun `create category`() = runTestAsUser {
        val me = client.getMe()
        val result = client.listCategoriesByUser(me.id)
        assertEquals(0, result.size)

        client.createCategory(CategoryIn(name = "Groceries"))
        val result2 = client.listCategoriesByUser(me.id)
        assertEquals(1, result2.size)
    }

    @Test
    fun `edit category`() = runTestAsUser {
        val category = client.createCategory(CategoryIn(name = "Groceries"))
        val result = client.getCategory(category.id)
        assertEquals("Groceries", result.name)

        client.updateCategory(result.id, CategoryIn(name = "Trips"))
        val result2 = client.getCategory(category.id)
        assertEquals("Trips", result2.name)
    }

    @Test
    fun `list categories`() = runTest{
        runAsUser1 {
            client.createCategory(CategoryIn(name = "Groceries"))
        }
        runAsUser2 {
            client.createCategory(CategoryIn(name = "Trips"))
            client.createCategory(CategoryIn(name = "Restaurants"))
        }

        runAsUser1 {
            val id = client.getMe().id
            val result = client.listCategoriesByUser(id)
            assertEquals(1, result.size)
            assertEquals("Groceries", result[0].name)
        }

        runAsUser2 {
            val id = client.getMe().id
            val result = client.listCategoriesByUser(id)
            assertEquals(2, result.size)
            assertTrue {
                result.any { it.name == "Restaurants" }
            }
            assertTrue {
                result.any { it.name == "Trips" }
            }
        }
    }

    @Test
    fun `delete category`() = runTestAsUser {
        val category = CategoryIn(name = "Groceries")
        val result = client.createCategory(category)
        client.assertCategoryExists(result.id)
        client.deleteCategory(result.id)
        client.assertCategoryDoesNotExist(result.id)
    }

    @Test
    fun `user cannot edit category they do not own`() = runTest {
        var categoryId: Long = 0
        runAsUser1 {
            categoryId = client.createCategory(CategoryIn(name = "Groceries")).id
        }

        runAsUser2 {
            client.put("$CATEGORY_URL/$categoryId"){
                contentType(ContentType.Application.Json)
                header(HttpHeaders.ContentType, ContentType.Application.Json)
                setBody(CategoryIn(name = "Trips"))
            }.apply {
                assertEquals(HttpStatusCode.Forbidden, status)
            }
        }
    }

    @Test
    fun `user cannot delete category they do not own`() = runTest {
        var categoryId: Long = 0
        runAsUser1 {
            categoryId = client.createCategory(CategoryIn(name = "Groceries")).id
        }

        runAsUser2 {
            client.delete("$CATEGORY_URL/$categoryId").apply {
                assertEquals(HttpStatusCode.Forbidden, status)
            }
        }
    }


    @Test
    fun `users cannot see each others categories`() = runTest {
        var userId: Long = 0
        runAsUser1 {
            userId = client.getMe().id
        }

        runAsUser2 {
            client.get("$CATEGORY_URL/user/$userId").apply {
                assertEquals(HttpStatusCode.Forbidden, status)
            }
        }
    }

}