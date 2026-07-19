package com.xavierclavel.services

import com.xavierclavel.config.Configuration
import com.xavierclavel.dtos.ExpenseIn
import com.xavierclavel.exceptions.ForbiddenCause
import com.xavierclavel.exceptions.ForbiddenException
import com.xavierclavel.exceptions.NotFoundCause
import com.xavierclavel.exceptions.NotFoundException
import com.xavierclavel.models.Expense
import com.xavierclavel.models.Subcategory
import com.xavierclavel.models.Tag
import com.xavierclavel.models.query.QExpense
import com.xavierclavel.models.query.QUser
import com.xavierclavel.dtos.ExpenseOut
import com.xavierclavel.enums.ExpenseType
import com.xavierclavel.models.query.QSubcategory
import com.xavierclavel.models.query.QTag
import io.ebean.Paging
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.math.BigDecimal
import java.time.LocalDate

class ExpenseService: KoinComponent {
    val configuration: Configuration by inject()

    private fun getById(id: Long): Expense =
        QExpense().id.eq(id).findOne() ?: throw NotFoundException(NotFoundCause.EXPENSE_NOT_FOUND)

    /**
     * Throw exception if a user tries to modify an expense he does not own
     */
    private fun Expense.checkRights(userId: Long): Expense {
        if (this.user.id != userId) {
            throw ForbiddenException(ForbiddenCause.MUST_OWN_EXPENSE)
        }
        return this
    }

    /**
     * Resolve the subcategory referenced by an expense and ensure the user owns it.
     * A null categoryId is allowed (uncategorized expense).
     */
    private fun resolveOwnedSubcategory(categoryId: Long?, userId: Long): Subcategory? {
        if (categoryId == null) return null
        val subcategory = QSubcategory().id.eq(categoryId).findOne()
            ?: throw NotFoundException(NotFoundCause.SUBCATEGORY_NOT_FOUND)
        if (subcategory.user.id != userId) {
            throw ForbiddenException(ForbiddenCause.MUST_OWN_CATEGORY)
        }
        return subcategory
    }

    /**
     * Resolve the tags referenced by an expense and ensure the user owns all of them.
     */
    private fun resolveOwnedTags(tagIds: List<Long>, userId: Long): MutableList<Tag> {
        if (tagIds.isEmpty()) return mutableListOf()
        val distinctIds = tagIds.distinct()
        val tags = QTag().id.isIn(distinctIds).findList()
        if (tags.size != distinctIds.size) {
            throw NotFoundException(NotFoundCause.TAG_NOT_FOUND)
        }
        tags.forEach {
            if (it.user.id != userId) {
                throw ForbiddenException(ForbiddenCause.MUST_OWN_TAG)
            }
        }
        return tags.toMutableList()
    }

    fun export(userId: Long, expenseId: Long): ExpenseOut =
        getById(expenseId)
            .checkRights(userId)
            .toOutput()

    fun list(
        userId: Long,
        paging: Paging,
        categoryId: Long?,
        subcategoryId: Long?,
        expenseType: ExpenseType?,
        from: LocalDate?,
        to: LocalDate?,
        query: String?,
        minAmount: BigDecimal?,
        maxAmount: BigDecimal?,
        tagId: Long?,
    ): List<ExpenseOut> {
        return QExpense()
            .user.id.eq(userId)
            .apply{
                if (subcategoryId != null) {
                    this.category.id.eq(subcategoryId)
                }
                if (categoryId != null) {
                    this.category.parentCategory.id.eq(categoryId)
                }
                if (tagId != null) {
                    this.tags.id.eq(tagId)
                }
                if (expenseType != null) {
                    this.type.eq(expenseType)
                }
                if (from != null) {
                    this.date.ge(from)
                }
                if (to != null) {
                    this.date.le(to)
                }
                if (!query.isNullOrBlank()) {
                    this.title.icontains(query.trim())
                }
                if (minAmount != null) {
                    this.amount.ge(minAmount)
                }
                if (maxAmount != null) {
                    this.amount.le(maxAmount)
                }
            }
            .setPaging(paging)
            .orderBy().date.desc()
            .findList()
            .map { it.toOutput() }
    }

    fun getOldestActivity(
        userId: Long,
    ): LocalDate? =
        QExpense()
            .user.id.eq(userId)
            .orderBy().date.asc()
            .setMaxRows(1)
            .findList()
            .firstOrNull()
            ?.toOutput()
            ?.date


    fun create(expenseDto: ExpenseIn, userId: Long): ExpenseOut {
        val user = QUser().id.eq(userId).findOne() ?: throw NotFoundException(NotFoundCause.USER_NOT_FOUND)
        val category = resolveOwnedSubcategory(expenseDto.categoryId, userId)
        val tags = resolveOwnedTags(expenseDto.tagIds, userId)

        val expense = Expense(
            user = user,
            title = expenseDto.title,
            category = category,
            date = expenseDto.date,
            amount = expenseDto.amount,
            currency = expenseDto.currency,
            type = expenseDto.type,
            tags = tags,
        )
        expense.insert()
        return expense.toOutput()
    }

    fun update(userId: Long, expenseId: Long, expenseDto: ExpenseIn): ExpenseOut {
        val category = resolveOwnedSubcategory(expenseDto.categoryId, userId)
        val tags = resolveOwnedTags(expenseDto.tagIds, userId)

        return getById(expenseId)
            .checkRights(userId)
            .apply {
                this.title = expenseDto.title
                this.category = category
                this.date = expenseDto.date
                this.amount = expenseDto.amount
                this.currency = expenseDto.currency
                this.tags = tags
            }
            .apply { this.update() }
            .toOutput()
    }


    //TODO: prevent deletion if expense used
    fun delete(userId: Long, expenseId: Long) {
        val result = getById(expenseId)
            .checkRights(userId)
            .delete()
        if (!result) {
            throw Exception("Failed to delete expense $expenseId")
        }
    }
}