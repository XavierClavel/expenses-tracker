package com.xavierclavel.services

import com.xavierclavel.config.Configuration
import com.xavierclavel.dtos.SubcategoryIn
import com.xavierclavel.dtos.SubcategoryOut
import com.xavierclavel.exceptions.ForbiddenCause
import com.xavierclavel.exceptions.ForbiddenException
import com.xavierclavel.exceptions.NotFoundCause
import com.xavierclavel.exceptions.NotFoundException
import com.xavierclavel.models.Subcategory
import com.xavierclavel.models.query.QCategory
import com.xavierclavel.models.query.QSubcategory
import com.xavierclavel.models.query.QUser
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class SubcategoryService: KoinComponent {
    val configuration: Configuration by inject()

    private fun getById(id: Long): Subcategory =
        QSubcategory().id.eq(id).findOne() ?: throw NotFoundException(NotFoundCause.SUBCATEGORY_NOT_FOUND)

    /**
     * Throw exception if a user tries to modify a category he does not own
     */
    private fun Subcategory.checkRights(userId: Long): Subcategory {
        if (this.user.id != userId) {
            throw ForbiddenException(ForbiddenCause.MUST_OWN_CATEGORY)
        }
        return this
    }

    fun export(userId: Long, categoryId: Long): SubcategoryOut =
        getById(categoryId)
            .checkRights(userId)
            .toOutput()

    fun list(userId: Long): List<SubcategoryOut> {
        return QSubcategory()
            .user.id.eq(userId)
            .findList()
            .map { it.toOutput() }
    }


    fun create(subcategoryDto: SubcategoryIn, userId: Long): SubcategoryOut {

        val user = QUser().id.eq(userId).findOne() ?: throw NotFoundException(NotFoundCause.USER_NOT_FOUND)
        val category = QCategory().id.eq(subcategoryDto.parentCategory).findOne() ?: throw NotFoundException(NotFoundCause.CATEGORY_NOT_FOUND)

        if (category.user.id != userId) {
            throw ForbiddenException(ForbiddenCause.MUST_OWN_CATEGORY)
        }

        //Create default subcategory
        val subcategory = Subcategory(
            user = user,
            name = subcategoryDto.name,
            icon = subcategoryDto.icon,
            type = subcategoryDto.type,
            parentCategory = category,
        )
        subcategory.insert()
        return subcategory.toOutput()
    }

    fun update(userId: Long, subcategoryId: Long, subcategoryDto: SubcategoryIn): SubcategoryOut {
        val category = QCategory().id.eq(subcategoryDto.parentCategory).findOne() ?: throw NotFoundException(NotFoundCause.CATEGORY_NOT_FOUND)

        if (category.user.id != userId) {
            throw ForbiddenException(ForbiddenCause.MUST_OWN_CATEGORY)
        }

        return getById(subcategoryId)
            .checkRights(userId)
            .apply {
                name = subcategoryDto.name
                icon = subcategoryDto.icon
                parentCategory = category
            }
            .apply { this.update() }
            .toOutput()
    }


    //TODO: prevent deletion if category used
    fun delete(userId: Long, subcategoryId: Long) {
        val result = getById(subcategoryId)
            .checkRights(userId)
            .delete()
        if (!result) {
            throw Exception("Failed to delete subcategory $subcategoryId")
        }
    }

}