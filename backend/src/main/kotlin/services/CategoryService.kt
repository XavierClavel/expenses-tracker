package com.xavierclavel.services

import com.xavierclavel.config.Configuration
import com.xavierclavel.dtos.CategoryIn
import com.xavierclavel.dtos.CategoryOut
import com.xavierclavel.exceptions.ForbiddenCause
import com.xavierclavel.exceptions.ForbiddenException
import com.xavierclavel.exceptions.NotFoundCause
import com.xavierclavel.exceptions.NotFoundException
import com.xavierclavel.models.Category
import com.xavierclavel.models.query.QCategory
import com.xavierclavel.models.query.QUser
import io.ebean.Paging
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class CategoryService: KoinComponent {
    val configuration: Configuration by inject()
    val userService: UserService by inject()

    private fun getById(id: Long): Category =
        QCategory().id.eq(id).findOne() ?: throw NotFoundException(NotFoundCause.USER_NOT_FOUND)

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

    fun list(userId: Long, sessionUserId: Long, paging: Paging): List<CategoryOut> {
        if (sessionUserId != userId) {
            throw ForbiddenException(ForbiddenCause.MUST_BE_PERFORMED_ON_SELF)
        }
        return QCategory()
            .user.id.eq(userId)
            .setPaging(paging)
            .findList()
            .map { it.toOutput() }
    }


    fun create(categoryDto: CategoryIn, userId: Long): CategoryOut {
        val user = QUser().id.eq(userId).findOne() ?: throw NotFoundException(NotFoundCause.USER_NOT_FOUND)
        val category = Category(
            user = user,
            name = categoryDto.name,
        )
        category.insert()
        return category.toOutput()
    }

    fun update(userId: Long, categoryId: Long, categoryDto: CategoryIn): CategoryOut =
        getById(categoryId)
            .checkRights(userId)
            .apply { name = categoryDto.name }
            .apply { this.update() }
            .toOutput()

    //TODO: prevent deletion if category used
    fun delete(userId: Long, categoryId: Long) {
        val result = getById(categoryId)
            .checkRights(userId)
            .delete()
        if (!result) {
            throw Exception("Failed to delete category $categoryId")
        }
    }

}