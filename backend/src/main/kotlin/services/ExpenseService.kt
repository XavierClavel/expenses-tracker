package com.xavierclavel.services

import com.xavierclavel.config.Configuration
import com.xavierclavel.dtos.ExpenseIn
import com.xavierclavel.exceptions.ForbiddenCause
import com.xavierclavel.exceptions.ForbiddenException
import com.xavierclavel.exceptions.NotFoundCause
import com.xavierclavel.exceptions.NotFoundException
import com.xavierclavel.models.Expense
import com.xavierclavel.models.query.QExpense
import com.xavierclavel.models.query.QUser
import com.xavierclavel.dtos.ExpenseOut
import com.xavierclavel.enums.ExpenseType
import com.xavierclavel.models.query.QSubcategory
import io.ebean.Paging
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
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
                if (expenseType != null) {
                    this.type.eq(expenseType)
                }
                if (from != null) {
                    this.date.ge(from)
                }
                if (to != null) {
                    this.date.le(to)
                }
            }
            .setPaging(paging)
            .orderBy().date.desc()
            .findList()
            .map { it.toOutput() }
    }


    fun create(expenseDto: ExpenseIn, userId: Long): ExpenseOut {
        val user = QUser().id.eq(userId).findOne() ?: throw NotFoundException(NotFoundCause.USER_NOT_FOUND)
        val category = QSubcategory().id.eq(expenseDto.categoryId).findOne()

        val expense = Expense(
            user = user,
            title = expenseDto.title,
            category = category,
            date = expenseDto.date,
            amount = expenseDto.amount,
            currency = expenseDto.currency,
            type = expenseDto.type,
        )
        expense.insert()
        return expense.toOutput()
    }

    fun update(userId: Long, expenseId: Long, expenseDto: ExpenseIn): ExpenseOut {
        val category = QSubcategory().id.eq(expenseDto.categoryId).findOne()

        return getById(expenseId)
            .checkRights(userId)
            .apply {
                this.title = expenseDto.title
                this.category = category
                this.date = expenseDto.date
                this.amount = expenseDto.amount
                this.currency = expenseDto.currency
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