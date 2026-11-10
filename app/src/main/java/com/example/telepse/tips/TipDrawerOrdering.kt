package com.example.telepse.tips

import com.example.telepse.data.model.CharacterSetCatalogRow
import com.example.telepse.data.model.TipCharacterCatalogItem

/** Matches web `PUBLIC_STRIP_POOL_MAX` / shuffle tip strip cap. */
const val TIP_STRIP_MAX = 20

data class TipDrawerSet(
    val id: String,
    val name: String,
    val characters: List<TipCharacterCatalogItem>,
)

data class TipDrawerPresentation(
    val drawerSets: List<TipDrawerSet>,
    val stripCharacters: List<TipCharacterCatalogItem>,
)

/**
 * TV tip strip/drawer ordering aligned with web `resolve-tip-drawer-sets` /
 * `shuffleTipStrip({ preserveHostOrder: true })`:
 * Elite → General → Premium → other; Comment pinned first; deterministic (no shuffle).
 */
object TipDrawerOrdering {
    fun isCommentCharacter(character: TipCharacterCatalogItem): Boolean {
        val normalized = character.name.trim().lowercase()
        return normalized == "comment" || normalized == "add comment"
    }

    fun isEliteSetName(name: String?): Boolean =
        name?.trim()?.lowercase()?.contains("elite") == true

    private fun setSortRank(name: String?): Int =
        when {
            isEliteSetName(name) -> 0
            name == "General" -> 1
            name == "Premium" -> 2
            else -> 3
        }

    fun sortDrawerSets(sets: List<CharacterSetCatalogRow>): List<CharacterSetCatalogRow> =
        sets
            .mapIndexed { index, set -> index to set }
            .sortedWith(
                compareBy<Pair<Int, CharacterSetCatalogRow>> { setSortRank(it.second.name) }
                    .thenBy { it.first },
            )
            .map { it.second }

    fun present(sets: List<CharacterSetCatalogRow>): TipDrawerPresentation {
        val comment =
            sets.asSequence()
                .flatMap { it.characters.asSequence() }
                .firstOrNull(::isCommentCharacter)

        val sorted =
            sortDrawerSets(sets)
                .map { set ->
                    val nonComment = set.characters.filterNot(::isCommentCharacter)
                    val characters =
                        if (comment != null) {
                            listOf(comment) + nonComment.filter { it.id != comment.id }
                        } else {
                            nonComment
                        }
                    TipDrawerSet(
                        id = set.id,
                        name = set.name,
                        characters = characters,
                    )
                }
                .filter { it.characters.isNotEmpty() }

        val strip = buildStrip(sorted, comment)
        return TipDrawerPresentation(drawerSets = sorted, stripCharacters = strip)
    }

    private fun buildStrip(
        drawerSets: List<TipDrawerSet>,
        comment: TipCharacterCatalogItem?,
    ): List<TipCharacterCatalogItem> {
        val byId = linkedMapOf<String, TipCharacterCatalogItem>()
        if (comment != null) {
            byId[comment.id] = comment
        }
        for (set in drawerSets) {
            for (character in set.characters) {
                if (!byId.containsKey(character.id)) {
                    byId[character.id] = character
                }
            }
        }
        return byId.values.take(TIP_STRIP_MAX)
    }
}
