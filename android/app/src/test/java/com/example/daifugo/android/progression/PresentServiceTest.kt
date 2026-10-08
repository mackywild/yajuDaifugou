package com.example.daifugo.android.progression

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PresentServiceTest {
    @Test
    fun receiveMovesPresentIntoInventory() {
        val present = PresentEntry(
            id = "admin:001",
            title = "管理者配布",
            itemId = "frame_silver_line",
            itemName = "シルバーライン",
            quantity = 1,
            source = "運営",
        )
        val before = PlayerProgress(presents = listOf(present))

        val result = PresentService.receiveOne(before, present.id)

        assertEquals(1, result.progress.inventory[present.itemId])
        assertTrue(result.progress.presents.isEmpty())
        assertEquals(listOf(present), result.received)
    }

    @Test
    fun receiveAllStacksDuplicateItems() {
        val first = PresentEntry("a", "A", "card_classic_red", "クラシックレッド", 1, "運営")
        val second = PresentEntry("b", "B", "card_classic_red", "クラシックレッド", 2, "ミッション")
        val before = PlayerProgress(
            inventory = mapOf("card_classic_red" to 1),
            presents = listOf(first, second),
        )

        val result = PresentService.receiveAll(before)

        assertEquals(4, result.progress.inventory["card_classic_red"])
        assertTrue(result.progress.presents.isEmpty())
    }
}
