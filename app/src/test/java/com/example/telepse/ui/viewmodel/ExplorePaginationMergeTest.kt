package com.example.telepse.ui.viewmodel

import com.example.telepse.data.model.ExploreFeedEvent
import com.example.telepse.data.model.ExploreFeedItem
import org.junit.Assert.assertEquals
import org.junit.Test

/** Mirrors [ExploreViewModel.loadMore] merge semantics (stable append, de-dupe by id). */
class ExplorePaginationMergeTest {
    @Test
    fun append_keepsOrderAndSkipsDuplicates() {
        val existing =
            listOf(
                sampleItem("a"),
                sampleItem("b"),
            )
        val batch =
            listOf(
                sampleItem("b"),
                sampleItem("c"),
            )
        val added = batch.filter { new -> existing.none { it.id == new.id } }
        val merged = existing + added
        assertEquals(listOf("a", "b", "c"), merged.map { it.id })
    }

    private fun sampleItem(id: String) =
        ExploreFeedItem(
            id = id,
            title = "Title $id",
            url = "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
            isLive = false,
            event = ExploreFeedEvent(eventName = "E", hostName = "H", username = "u"),
        )
}
