package com.xavierclavel.services

import com.xavierclavel.config.Configuration
import com.xavierclavel.dtos.TagIn
import com.xavierclavel.dtos.TagOut
import com.xavierclavel.enums.ExpenseType
import com.xavierclavel.exceptions.ForbiddenCause
import com.xavierclavel.exceptions.ForbiddenException
import com.xavierclavel.exceptions.NotFoundCause
import com.xavierclavel.exceptions.NotFoundException
import com.xavierclavel.models.Tag
import com.xavierclavel.models.query.QExpense
import com.xavierclavel.models.query.QTag
import com.xavierclavel.models.query.QUser
import io.ebean.DB
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.math.BigDecimal

class TagService: KoinComponent {
    val configuration: Configuration by inject()

    private fun getById(id: Long): Tag =
        QTag().id.eq(id).findOne() ?: throw NotFoundException(NotFoundCause.TAG_NOT_FOUND)

    /**
     * Throw exception if a user tries to modify a tag he does not own
     */
    private fun Tag.checkRights(userId: Long): Tag {
        if (this.user.id != userId) {
            throw ForbiddenException(ForbiddenCause.MUST_OWN_TAG)
        }
        return this
    }

    /**
     * Map a tag to its output, computing the aggregated total spent and the number
     * of expenses it is linked to for the given user.
     */
    private fun Tag.withTotals(userId: Long): TagOut {
        val total = QExpense()
            .select("sum(amount)")
            .user.id.eq(userId)
            .type.eq(ExpenseType.EXPENSE)
            .tags.id.eq(this.id)
            .findSingleAttribute() ?: BigDecimal.ZERO
        val count = QExpense()
            .user.id.eq(userId)
            .tags.id.eq(this.id)
            .findCount()
        return this.toOutput(total, count)
    }

    fun export(userId: Long, tagId: Long): TagOut =
        getById(tagId)
            .checkRights(userId)
            .withTotals(userId)

    fun list(userId: Long): List<TagOut> =
        QTag()
            .user.id.eq(userId)
            .orderBy().label.asc()
            .findList()
            .map { it.withTotals(userId) }

    fun create(tagDto: TagIn, userId: Long): TagOut {
        val user = QUser().id.eq(userId).findOne() ?: throw NotFoundException(NotFoundCause.USER_NOT_FOUND)
        val tag = Tag(
            user = user,
            label = tagDto.label,
        )
        tag.insert()
        return tag.withTotals(userId)
    }

    fun update(userId: Long, tagId: Long, tagDto: TagIn): TagOut =
        getById(tagId)
            .checkRights(userId)
            .apply { label = tagDto.label }
            .apply { this.update() }
            .withTotals(userId)

    fun delete(userId: Long, tagId: Long) {
        val tag = getById(tagId)
            .checkRights(userId)

        // Remove the many-to-many links before deleting the tag itself.
        DB.sqlUpdate("delete from expense_tag where tag_id = :tagId")
            .setParameter("tagId", tagId)
            .execute()

        val result = tag.delete()
        if (!result) {
            throw Exception("Failed to delete tag $tagId")
        }
    }
}
