package com.xavierclavel.services

import com.xavierclavel.config.Configuration
import com.xavierclavel.dtos.investment.InvestmentIn
import com.xavierclavel.dtos.investment.InvestmentOut
import com.xavierclavel.exceptions.ForbiddenCause
import com.xavierclavel.exceptions.ForbiddenException
import com.xavierclavel.exceptions.NotFoundCause
import com.xavierclavel.exceptions.NotFoundException
import com.xavierclavel.models.Investment
import com.xavierclavel.models.query.QInvestment
import com.xavierclavel.models.query.QInvestmentAccount
import com.xavierclavel.models.query.QUser
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class InvestmentService: KoinComponent {
    val configuration: Configuration by inject()

    private fun getById(id: Long): Investment =
        QInvestment().id.eq(id).findOne() ?: throw NotFoundException(NotFoundCause.ACCOUNT_NOT_FOUND)

    /**
     * Throw exception if a user tries to modify a transfer he does not own
     */
    private fun Investment.checkRights(userId: Long): Investment {
        if (this.account.owner.id != userId) {
            throw ForbiddenException(ForbiddenCause.MUST_BE_OWNER)
        }
        return this
    }

    fun get(userId: Long, investmentId: Long): InvestmentOut =
        getById(investmentId)
            .checkRights(userId)
            .toOutput()

    fun list(userId: Long, accountId: Long? = null): List<InvestmentOut> {
        return QInvestment()
            .account.owner.id.eq(userId)
            .apply {
                if (accountId != null) {
                    this.account.id.eq(accountId)
                }
            }
            .orderBy().date.desc()
            .findList()
            .map { it.toOutput() }
    }

    fun create(investmentDto: InvestmentIn, userId: Long, accountId: Long): InvestmentOut {
        val account = QInvestmentAccount().id.eq(accountId).findOne() ?: throw NotFoundException(NotFoundCause.ACCOUNT_NOT_FOUND)
        if (account.owner.id != userId) {
            throw ForbiddenException(ForbiddenCause.MUST_BE_OWNER)
        }
        val user = QUser().id.eq(userId).findOne() ?: throw NotFoundException(NotFoundCause.USER_NOT_FOUND)
        val investment = Investment(
            amount = investmentDto.amount,
            account = account,
            user = user,
            date = investmentDto.date,
            type = investmentDto.type,
        )
        investment.insert()

        return investment.toOutput()
    }

    fun update(userId: Long, investmentId: Long, investmentDto: InvestmentIn): InvestmentOut =
        getById(investmentId)
            .checkRights(userId)
            .apply {
                amount = investmentDto.amount
                date = investmentDto.date
                type = investmentDto.type
            }
            .apply { this.update() }
            .toOutput()

    fun delete(userId: Long, investmentId: Long) {
        val result = getById(investmentId)
            .checkRights(userId)
            .delete()
        if (!result) {
            throw Exception("Failed to delete transfer $investmentId")
        }
    }

    /**
     * Delete several transfers at once. Every transfer must belong to the user.
     */
    fun batchDelete(userId: Long, ids: List<Long>) {
        if (ids.isEmpty()) return
        val investments = QInvestment().id.isIn(ids.distinct()).findList()
        investments.forEach {
            if (it.account.owner.id != userId) {
                throw ForbiddenException(ForbiddenCause.MUST_BE_OWNER)
            }
        }
        investments.forEach { it.delete() }
    }
}
