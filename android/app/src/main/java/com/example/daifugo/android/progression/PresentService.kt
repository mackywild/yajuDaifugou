package com.example.daifugo.android.progression

data class PresentReceiveResult(
    val progress: PlayerProgress,
    val received: List<PresentEntry>,
)

/**
 * プレゼントを所持品へ移す純粋ロジック。
 * ミッション報酬・管理者配布のどちらも同じPresentEntryとして扱う。
 */
object PresentService {
    fun receiveOne(progress: PlayerProgress, presentId: String): PresentReceiveResult {
        val present = progress.presents.firstOrNull { it.id == presentId }
            ?: throw IllegalArgumentException("受け取るプレゼントが存在しません")
        return apply(progress, listOf(present))
    }

    fun receiveAll(progress: PlayerProgress): PresentReceiveResult =
        apply(progress, progress.presents)

    private fun apply(
        progress: PlayerProgress,
        targets: List<PresentEntry>,
    ): PresentReceiveResult {
        if (targets.isEmpty()) {
            return PresentReceiveResult(progress, emptyList())
        }

        val inventory = progress.inventory.toMutableMap()
        targets.forEach { present ->
            inventory[present.itemId] =
                (inventory[present.itemId] ?: 0) + present.quantity
        }

        val ids = targets.mapTo(mutableSetOf()) { it.id }
        return PresentReceiveResult(
            progress = progress.copy(
                inventory = inventory,
                presents = progress.presents.filterNot { it.id in ids },
            ),
            received = targets,
        )
    }
}
