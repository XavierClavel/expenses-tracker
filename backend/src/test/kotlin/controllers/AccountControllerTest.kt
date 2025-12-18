package com.xavierclavel.controllers

import com.xavierclavel.ApplicationTest
import com.xavierclavel.dtos.investment.InvestmentAccountIn
import com.xavierclavel.utils.createAccount
import com.xavierclavel.utils.getAccount
import kotlin.test.Test

class AccountControllerTest: ApplicationTest() {

    val accountDto = InvestmentAccountIn(
        name = "PEA"
    )

}