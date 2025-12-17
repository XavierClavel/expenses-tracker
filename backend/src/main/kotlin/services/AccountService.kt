package com.xavierclavel.services

import com.xavierclavel.config.Configuration
import com.xavierclavel.dtos.investment.InvestmentAccountIn
import com.xavierclavel.dtos.investment.InvestmentAccountOut
import com.xavierclavel.exceptions.ForbiddenCause
import com.xavierclavel.exceptions.ForbiddenException
import com.xavierclavel.exceptions.NotFoundCause
import com.xavierclavel.exceptions.NotFoundException
import com.xavierclavel.models.InvestmentAccount
import com.xavierclavel.models.query.QInvestmentAccount
import com.xavierclavel.models.query.QUser
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class AccountService: KoinComponent {
    val configuration: Configuration by inject()
    val userService: UserService by inject()

    private fun getById(id: Long): InvestmentAccount =
        QInvestmentAccount().id.eq(id).findOne() ?: throw NotFoundException(NotFoundCause.ACCOUNT_NOT_FOUND)

    /**
     * Throw exception if a user tries to modify an account he does not own
     */
    private fun InvestmentAccount.checkRights(userId: Long): InvestmentAccount {
        if (this.owner.id != userId) {
            throw ForbiddenException(ForbiddenCause.MUST_BE_OWNER)
        }
        return this
    }

    fun get(userId: Long, accountId: Long): InvestmentAccountOut =
        getById(accountId)
            .checkRights(userId)
            .toOutput()

    fun list(userId: Long): List<InvestmentAccountOut> {
        return QInvestmentAccount()
            .owner.id.eq(userId)
            .findList()
            .map { it.toOutput() }
    }


    fun create(accountDto: InvestmentAccountIn, userId: Long): InvestmentAccountOut {
        val user = QUser().id.eq(userId).findOne() ?: throw NotFoundException(NotFoundCause.USER_NOT_FOUND)
        val account = InvestmentAccount(
            name = accountDto.name,
            owner = user,
        )
        account.insert()

        return account.toOutput()
    }

    fun update(userId: Long, accountId: Long, accountDto: InvestmentAccountIn): InvestmentAccountOut =
        getById(accountId)
            .checkRights(userId)
            .apply {
                name = accountDto.name
            }
            .apply { this.update() }
            .toOutput()

    fun delete(userId: Long, accountId: Long) {
        val result = getById(accountId)
            .checkRights(userId)
            .delete()
        if (!result) {
            throw Exception("Failed to delete account $accountId")
        }
    }

}