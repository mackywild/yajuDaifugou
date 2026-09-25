package com.example.daifugo.android.gacha

import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GachaServiceTest {
    private val catalog = listOf(
        GachaItemDefinition("a", "A", "TEST", "R", 1),
        GachaItemDefinition("b", "B", "TEST", "SR", 1),
    )

    @Test
    fun tenPullConsumesCostAndReturnsTenItems() {
        val result = GachaService(Random(810)).pull(10, 999, catalog)
        assertEquals(10, result.items.size)
        assertEquals(GachaRules.costFor(10), result.materialSpent)
        assertTrue(result.items.all { it.id == "a" || it.id == "b" })
    }

    @Test(expected = IllegalArgumentException::class)
    fun emptyCatalogCannotConsumeMaterial() {
        GachaService(Random(810)).pull(1, 999, emptyList())
    }
}
