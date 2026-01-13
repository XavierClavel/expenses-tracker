package com.xavierclavel.services

import com.xavierclavel.config.Configuration
import com.xavierclavel.dtos.investment.AccountReportIn
import com.xavierclavel.dtos.investment.AccountReportOut
import com.xavierclavel.exceptions.ForbiddenCause
import com.xavierclavel.exceptions.ForbiddenException
import com.xavierclavel.exceptions.NotFoundCause
import com.xavierclavel.exceptions.NotFoundException
import com.xavierclavel.models.AccountReport
import com.xavierclavel.models.query.QAccountReport
import com.xavierclavel.models.query.QInvestmentAccount
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class AccountReportService: KoinComponent {
    val configuration: Configuration by inject()

    private fun getById(id: Long): AccountReport =
        QAccountReport().id.eq(id).findOne() ?: throw NotFoundException(NotFoundCause.ACCOUNT_NOT_FOUND)

    /**
     * Throw exception if a user tries to modify an account he does not own
     */
    private fun AccountReport.checkRights(userId: Long): AccountReport {
        if (this.account.owner.id != userId) {
            throw ForbiddenException(ForbiddenCause.MUST_BE_OWNER)
        }
        return this
    }

    fun get(userId: Long, reportId: Long): AccountReportOut =
        getById(reportId)
            .checkRights(userId)
            .toOutput()

    fun list(userId: Long, accountId: Long? = null): List<AccountReportOut> {
        return QAccountReport()
            .account.owner.id.eq(userId)
            .apply{
                if (accountId != null) {
                    this.account.id.eq(accountId)
                }
            }
            .orderBy().date.desc()
            .findList()
            .map { it.toOutput() }
    }


    fun create(reportDto: AccountReportIn, userId: Long, accountId: Long): AccountReportOut {
        val account = QInvestmentAccount().id.eq(accountId).findOne() ?: throw NotFoundException(NotFoundCause.ACCOUNT_NOT_FOUND)
        if (account.owner.id != userId) {
            throw ForbiddenException(ForbiddenCause.MUST_BE_OWNER)
        }
        val report = AccountReport(
            amount = reportDto.amount,
            account = account,
            date = reportDto.date,
        )
        report.insert()

        return report.toOutput()
    }

    fun update(userId: Long, reportId: Long, reportDto: AccountReportIn): AccountReportOut =
        getById(reportId)
            .checkRights(userId)
            .apply {
                amount = reportDto.amount
                date = reportDto.date
            }
            .apply { this.update() }
            .toOutput()

    fun delete(userId: Long, reportId: Long) {
        val result = getById(reportId)
            .checkRights(userId)
            .delete()
        if (!result) {
            throw Exception("Failed to delete report $reportId")
        }
    }

}