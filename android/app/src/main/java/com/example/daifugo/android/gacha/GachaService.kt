package com.example.daifugo.android.gacha

import kotlin.random.Random

data class GachaItemDefinition(
    val id: String,
    val displayName: String,
    val category: String,
    val rarity: String,
    val weight: Int,
)

data class GachaPullResult(
    val items: List<GachaItemDefinition>,
    val materialSpent: Int,
)

object GachaRules {
    const val SINGLE_PULL_COST = 10
    val SUPPORTED_PULL_COUNTS = setOf(1, 10)

    fun costFor(count: Int): Int {
        require(count in SUPPORTED_PULL_COUNTS) { "1回または10回で指定してください" }
        return SINGLE_PULL_COST * count
    }
}

object DefaultGachaCatalog {
    // 排出物は次フェーズで決定する。ここへ登録するとUIのガチャが有効になる。
    val items: List<GachaItemDefinition> = emptyList()
}

class GachaService(private val random: Random = Random.Default) {
    fun pull(
        count: Int,
        materialBalance: Int,
        catalog: List<GachaItemDefinition>,
    ): GachaPullResult {
        val cost = GachaRules.costFor(count)
        require(catalog.isNotEmpty()) { "ガチャの排出アイテムがまだ登録されていません" }
        require(materialBalance >= cost) { "ガチャ素材が不足しています" }
        require(catalog.all { it.weight > 0 }) { "排出weightは1以上で指定してください" }

        val totalWeight = catalog.sumOf { it.weight }
        val pulled = List(count) {
            val roll = random.nextInt(totalWeight)
            var cursor = 0
            catalog.first { item ->
                cursor += item.weight
                roll < cursor
            }
        }
        return GachaPullResult(pulled, cost)
    }
}
