package com.xavierclavel.services

import com.xavierclavel.exceptions.ForbiddenCause
import com.xavierclavel.exceptions.ForbiddenException
import com.xavierclavel.exceptions.NotFoundCause
import com.xavierclavel.exceptions.NotFoundException
import com.xavierclavel.models.Subcategory
import com.xavierclavel.models.Tag
import com.xavierclavel.models.query.QSubcategory
import com.xavierclavel.models.query.QTag

/**
 * Resolve the subcategory referenced by an expense and ensure the user owns it.
 * A null categoryId is allowed (uncategorized expense).
 */
internal fun resolveOwnedSubcategory(categoryId: Long?, userId: Long): Subcategory? {
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
internal fun resolveOwnedTags(tagIds: List<Long>, userId: Long): MutableList<Tag> {
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
