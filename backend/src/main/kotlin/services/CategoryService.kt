package com.xavierclavel.services

import com.xavierclavel.config.Configuration
import com.xavierclavel.dtos.CategoryIn
import com.xavierclavel.dtos.CategoryOut
import com.xavierclavel.exceptions.ForbiddenCause
import com.xavierclavel.exceptions.ForbiddenException
import com.xavierclavel.exceptions.NotFoundCause
import com.xavierclavel.exceptions.NotFoundException
import com.xavierclavel.models.Category
import com.xavierclavel.models.Subcategory
import com.xavierclavel.models.query.QCategory
import com.xavierclavel.models.query.QExpense
import com.xavierclavel.models.query.QSubcategory
import com.xavierclavel.models.query.QUser
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class CategoryService: KoinComponent {
    val configuration: Configuration by inject()
    val userService: UserService by inject()

    private fun getById(id: Long): Category =
        QCategory().id.eq(id).findOne() ?: throw NotFoundException(NotFoundCause.CATEGORY_NOT_FOUND)

    /**
     * Throw exception if a user tries to modify a category he does not own
     */
    private fun Category.checkRights(userId: Long): Category {
        if (this.user.id != userId) {
            throw ForbiddenException(ForbiddenCause.MUST_OWN_CATEGORY)
        }
        return this
    }

    fun export(userId: Long, categoryId: Long): CategoryOut =
        getById(categoryId)
            .checkRights(userId)
            .toOutput()

    fun list(userId: Long): List<CategoryOut> {
        return QCategory()
            .user.id.eq(userId)
            .findList()
            .map { it.toOutput() }
    }


    fun create(categoryDto: CategoryIn, userId: Long): CategoryOut {
        val user = QUser().id.eq(userId).findOne() ?: throw NotFoundException(NotFoundCause.USER_NOT_FOUND)
        val category = Category(
            user = user,
            name = categoryDto.name,
            color = categoryDto.color,
            icon = categoryDto.icon,
            type = categoryDto.type,
        )
        category.insert()

        //Create default subcategory
        Subcategory(
            user = user,
            name = categoryDto.name,
            icon = categoryDto.icon,
            type = categoryDto.type,
            parentCategory = category,
            isDefault = true,
        ).insert()
        return category.toOutput()
    }

    fun update(userId: Long, categoryId: Long, categoryDto: CategoryIn): CategoryOut =
        getById(categoryId)
            .checkRights(userId)
            .apply {
                name = categoryDto.name
                icon = categoryDto.icon
                color = categoryDto.color
            }
            .apply { this.update() }
            .toOutput()

    //TODO: prevent deletion if category used
    fun delete(userId: Long, categoryId: Long) {
        val category = getById(categoryId)
            .checkRights(userId)

        val isCategoryUsed = QExpense()
            .category.parentCategory.id.eq(categoryId)
            .exists()

        if(isCategoryUsed) {
            throw ForbiddenException(ForbiddenCause.MUST_OWN_CATEGORY)
        }

        QSubcategory().parentCategory.id.eq(categoryId).delete()

        val result = category.delete()
        if (!result) {
            throw Exception("Failed to delete category $categoryId")
        }
    }

}