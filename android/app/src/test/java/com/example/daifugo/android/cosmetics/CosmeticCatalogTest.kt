package com.example.daifugo.android.cosmetics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class CosmeticCatalogTest {
    @Test
    fun starterCatalogCoversAllFourCategories() {
        CosmeticCategory.entries.forEach { category ->
            assertNotNull(CosmeticCatalog.items.firstOrNull { it.category == category })
        }
        assertEquals(
            "ソフトグロー",
            CosmeticCatalog.find(CosmeticCatalog.EFFECT_SOFT_GLOW)?.displayName,
        )
    }
}
