package com.xavierclavel.exceptions

class UnauthorizedException(val reason: UnauthorizedCause): Exception(reason.key)

class ForbiddenException(val reason: ForbiddenCause): Exception(reason.key)

class BadRequestException(val reason: BadRequestCause): Exception(reason.key)

class NotFoundException(val reason: NotFoundCause): Exception(reason.key)

enum class UnauthorizedCause(val key: String) {
    SESSION_NOT_FOUND("session_not_found"),
    INVALID_PASSWORD("invalid_password"),
    INVALID_CREDENTIALS("invalid_mail_or_password"),
    INVALID_TOKEN("invalid_token"),
    OAUTH_FAILED("oauth_failed"),
    OAUTH_NOT_SETUP("oauth_not_setup"),
}

enum class ForbiddenCause(val key: String) {
    MUST_BE_PERFORMED_ON_SELF("must_be_performed_on_self"),
    MUST_OWN_CATEGORY("must own category"),
}

enum class NotFoundCause(val key: String) {
    USER_NOT_FOUND("user_not_found"),
    CATEGORY_NOT_FOUND("category_not_found"),
}

enum class BadRequestCause (val key: String) {
    NOT_APPLICABLE_ON_SELF("not_applicable_on_self"),
    MAIL_ALREADY_USED("mail_already_used"),
    USERNAME_ALREADY_USED("username_already_used"),
    OAUTH_ONLY("oauth_only"),
    INVALID_REQUEST("invalid_request"),
}