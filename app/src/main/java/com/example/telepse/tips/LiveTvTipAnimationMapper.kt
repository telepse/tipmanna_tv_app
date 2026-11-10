package com.example.telepse.tips

import com.example.telepse.data.model.ResolvedTipCharacter
import com.example.telepse.data.model.TipAnimationEvent
import com.example.telepse.data.model.TipCharacterCatalogItem
import com.example.telepse.ui.animations.AnimationTypeMapper
import com.example.telepse.ui.viewmodel.AnimationTrigger

object LiveTvTipAnimationMapper {
    fun fromPaymentEvent(
        event: TipAnimationEvent,
        catalogByLowerName: Map<String, TipCharacterCatalogItem>,
    ): List<AnimationTrigger> {
        val resolved = event.resolvedTips.orEmpty()
        if (resolved.isNotEmpty()) {
            return resolved.mapIndexed { index, tip ->
                toTrigger(
                    prefix = paymentIdPrefix(event),
                    suffix = tip.occurrenceId ?: "${tip.name}_$index",
                    styleName = tip.styleName,
                    imageUrl = tip.iconUrl,
                )
            }
        }
        return event.selectedTips.mapNotNull { tipName ->
            val catalog =
                catalogByLowerName[tipName.trim().lowercase()]
                    ?: catalogByLowerName.entries
                        .firstOrNull { it.key.equals(tipName.trim(), ignoreCase = true) }
                        ?.value
            toTrigger(
                prefix = paymentIdPrefix(event),
                suffix = tipName.trim(),
                styleName = null,
                imageUrl = catalog?.iconUrl.orEmpty(),
            )
        }
    }

    fun preview(character: TipCharacterCatalogItem): AnimationTrigger {
        return toTrigger(
            prefix = "preview",
            suffix = "${character.id}_${java.util.UUID.randomUUID()}",
            styleName = null,
            imageUrl = character.iconUrl.orEmpty(),
        )
    }

    private fun paymentIdPrefix(event: TipAnimationEvent): String {
        val tx = event.transactionId?.trim().orEmpty()
        val bundle = event.id?.trim().orEmpty()
        return when {
            tx.isNotEmpty() -> "pay:$tx"
            bundle.isNotEmpty() -> "pay:$bundle"
            else -> "pay:unknown"
        }
    }

    private fun toTrigger(
        prefix: String,
        suffix: String,
        styleName: String?,
        imageUrl: String,
    ): AnimationTrigger {
        return AnimationTrigger(
            animationType = AnimationTypeMapper.mapStyleName(styleName),
            imageUrl = imageUrl,
            id = "$prefix:$suffix",
        )
    }
}
