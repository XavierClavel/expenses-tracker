package com.xavierclavel.services

import com.xavierclavel.config.Configuration
import com.xavierclavel.dtos.auth.SignupDto
import com.xavierclavel.dtos.UserIn
import com.xavierclavel.dtos.UserOut
import com.xavierclavel.exceptions.BadRequestCause
import com.xavierclavel.exceptions.BadRequestException
import com.xavierclavel.exceptions.NotFoundCause
import com.xavierclavel.exceptions.NotFoundException
import com.xavierclavel.exceptions.UnauthorizedCause
import com.xavierclavel.exceptions.UnauthorizedException
import com.xavierclavel.models.User
import com.xavierclavel.models.query.QUser
import io.ebean.Paging
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class UserService: KoinComponent {
    val encryptionService: EncryptionService by inject()
    val configuration: Configuration by inject()

    private fun getById(id: Long): User =
        QUser().id.eq(id).findOne() ?: throw NotFoundException(NotFoundCause.USER_NOT_FOUND)

    fun export(id: Long): UserOut =
        getById(id).toOutput()

    fun exportByMail(mail: String): UserOut? =
        QUser().emailAddress.eq(mail).findOne()?.toOutput()

    fun exportByGoogleId(googleId: String): UserOut? =
        QUser().googleId.eq(googleId).findOne()?.toOutput()


    fun exportAll(paging: Paging): List<UserOut> =
        QUser()
            .setPaging(paging)
            .findList()
            .map { it.toOutput() }

    fun create(user: SignupDto): UserOut =
        User(
            username = user.username,
            emailAddress = user.emailAddress,
            hashedPassword = encryptionService.encryptPassword(user.password),
        ).apply { save() }
        .toOutput()

    fun edit(id: Long, user: UserIn): UserOut =
        getById(id)
            .apply {
                this.username = user.username
            }
            .apply { save() }
            .toOutput()

    fun deleteById(id: Long) {
        val user = getById(id)
        val result = user.delete()
        if (!result) {
            throw Exception("User with id $id could not be deleted")
        }
    }

    fun checkCredentials(emailAddress: String, password: String) {
        val user = QUser().emailAddress.eq(emailAddress)?.findOne() ?:
            throw UnauthorizedException(UnauthorizedCause.INVALID_CREDENTIALS)
        if (user.hashedPassword == null) {
            throw BadRequestException(BadRequestCause.OAUTH_ONLY)
        }
        if (!encryptionService.isPasswordCorrect(user.hashedPassword!!, password)) {
            throw UnauthorizedException(UnauthorizedCause.INVALID_CREDENTIALS)
        }
    }

    fun existsByEmail(mail: String): Boolean =
        QUser().emailAddress.eq(mail).exists()

    fun existsByUsername(username: String): Boolean =
        QUser().username.eq(username).exists()

    fun setupDefaultAdmin() {
        val dto = SignupDto(
            username = "admin",
            password = configuration.admin.password,
            emailAddress = "admin@mail.com"
        )
    }
}