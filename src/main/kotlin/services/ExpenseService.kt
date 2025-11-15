package com.xavierclavel.services

import com.xavierclavel.config.Configuration
import com.xavierclavel.dtos.CategorySummary
import com.xavierclavel.dtos.ExpenseIn
import com.xavierclavel.exceptions.ForbiddenCause
import com.xavierclavel.exceptions.ForbiddenException
import com.xavierclavel.exceptions.NotFoundCause
import com.xavierclavel.exceptions.NotFoundException
import com.xavierclavel.models.Expense
import com.xavierclavel.models.query.QCategory
import com.xavierclavel.models.query.QExpense
import com.xavierclavel.models.query.QUser
import com.xavierclavel.dtos.ExpenseOut
import com.xavierclavel.dtos.MonthSummary
import io.ebean.DB
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

    fun export(userId: Long, expenseId: Long): ExpenseOut =
        getById(expenseId)
            .checkRights(userId)
            .toOutput()

    fun list(userId: Long, sessionUserId: Long, paging: Paging): List<ExpenseOut> {
        if (sessionUserId != userId) {
            throw ForbiddenException(ForbiddenCause.MUST_BE_PERFORMED_ON_SELF)
        }
        return QExpense()
            .user.id.eq(userId)
            .setPaging(paging)
            .findList()
            .map { it.toOutput() }
    }


    fun create(expenseDto: ExpenseIn, userId: Long): ExpenseOut {
        val user = QUser().id.eq(userId).findOne() ?: throw NotFoundException(NotFoundCause.USER_NOT_FOUND)
        val category = if (expenseDto.categoryId == null) {
            null
        } else {
            QCategory().id.eq(expenseDto.categoryId).findOne() ?: throw NotFoundException(NotFoundCause.CATEGORY_NOT_FOUND)
        }
        val expense = Expense(
            user = user,
            label = expenseDto.label,
            category = category,
            date = expenseDto.date,
            amount = expenseDto.amount,
            currency = expenseDto.currency,
        )
        expense.insert()
        return expense.toOutput()
    }

    fun update(userId: Long, expenseId: Long, expenseDto: ExpenseIn): ExpenseOut =
        getById(expenseId)
            .checkRights(userId)
            .apply {
                this.label = expenseDto.label
                this.category = category
                this.date = expenseDto.date
                this.amount = expenseDto.amount
                this.currency = expenseDto.currency
            }
            .apply { this.update() }
            .toOutput()

    //TODO: prevent deletion if expense used
    fun delete(userId: Long, expenseId: Long) {
        val result = getById(expenseId)
            .checkRights(userId)
            .delete()
        if (!result) {
            throw Exception("Failed to delete expense $expenseId")
        }
    }

    fun summaryOfDay(userId: Long, year: Int, month: Int, day: Int): MonthSummary {
        val start = LocalDate.of(year, month, 1)
        val end = start.plusDays(1)
        val (total, byCategory) =  summary(userId, start, end)
        TODO()
    }


    fun summaryOfMonth(userId: Long, year: Int, month: Int): MonthSummary {
        val start = LocalDate.of(year, month, 1)
        val end = start.plusMonths(1)
        val (total, byCategory) =  summary(userId, start, end)
        return MonthSummary(
            year = year,
            month = month,
            totalExpenses = total,
            byCategory = byCategory,
        )
    }

    fun summaryOfYear(userId: Long, year: Int, month: Int): MonthSummary {
        val start = LocalDate.of(year, 1, 1)
        val end = start.plusYears(1)
        val (total, byCategory) =  summary(userId, start, end)
        TODO()
    }

    fun summary(userId: Long, start: LocalDate, end: LocalDate): Pair<BigDecimal, List<CategorySummary>> {
        val total = QExpense()
            .select("sum(amount)")
            .user.id.eq(userId)
            .date.ge(start)
            .date.lt(end)
            .findSingleAttribute() ?: BigDecimal.ZERO

        val summaryByCategory = DB.findDto(CategorySummary::class.java,
            """
            select 
              e.category_id as categoryId,
              c.name as categoryName,
              sum(e.amount) as total
            from expenses e
            left join categories c on c.id = e.category_id
            where e.user_id = :userId 
              and e.date >= :start 
              and e.date < :end
            group by e.category_id, c.name
            """
        )
            .setParameter("userId", userId)
            .setParameter("start", start)
            .setParameter("end", end)
            .findList()

        return Pair(total, summaryByCategory)
    }
}