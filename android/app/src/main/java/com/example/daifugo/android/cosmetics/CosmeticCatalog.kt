package com.example.daifugo.android.cosmetics

enum class CosmeticCategory {
    AVATAR,
    FRAME,
    CARD_SKIN,
    EFFECT,
}

data class CosmeticItemDefinition(
    val id: String,
    val displayName: String,
    val category: CosmeticCategory,
    val description: String,
)

/**
 * 実画像アセットとは独立したアイテム定義。
 * 画像が未配置でもミッション報酬・所持判定を先行実装できる。
 */
object CosmeticCatalog {
    const val AVATAR_BUSINESS = "avatar_business"
    const val FRAME_SILVER_LINE = "frame_silver_line"
    const val CARD_CLASSIC_RED = "card_classic_red"
    const val EFFECT_SOFT_GLOW = "effect_soft_glow"

    val items: List<CosmeticItemDefinition> = listOf(
        CosmeticItemDefinition(
            id = AVATAR_BUSINESS,
            displayName = "ビジネススーツの野獣",
            category = CosmeticCategory.AVATAR,
            description = "恒常アバターカード。落ち着いたスーツスタイル。",
        ),
        CosmeticItemDefinition(
            id = FRAME_SILVER_LINE,
            displayName = "シルバーライン",
            category = CosmeticCategory.FRAME,
            description = "シンプルな銀色プロフィールフレーム。",
        ),
        CosmeticItemDefinition(
            id = CARD_CLASSIC_RED,
            displayName = "クラシックレッド",
            category = CosmeticCategory.CARD_SKIN,
            description = "赤を基調にしたクラシックなトランプスキン。",
        ),
        CosmeticItemDefinition(
            id = EFFECT_SOFT_GLOW,
            displayName = "ソフトグロー",
            category = CosmeticCategory.EFFECT,
            description = "カード提出時に淡く光るシンプルなプレイエフェクト。",
        ),
    )

    fun find(id: String): CosmeticItemDefinition? = items.firstOrNull { it.id == id }
}
