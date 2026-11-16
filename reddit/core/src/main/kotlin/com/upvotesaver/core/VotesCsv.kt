package com.upvotesaver.core

/**
 * Reads `post_votes.csv` from a Reddit data export (reddit.com/settings/data-request):
 * columns `id,permalink,direction`. Returns the ids of upvoted posts, in file order.
 * This is the only way to reach upvotes older than the ~1000 the API lists.
 */
object VotesCsv {
    fun upvotedIds(csv: String): List<String> {
        val lines = csv.lineSequence().map { it.trim() }.filter { it.isNotEmpty() }.toList()
        if (lines.isEmpty()) return emptyList()
        val header = lines.first().split(',').map { it.trim().trim('"').lowercase() }
        val idCol = header.indexOf("id").takeIf { it >= 0 } ?: 0
        val dirCol = header.indexOf("direction")
        val hasHeader = "id" in header
        return lines.drop(if (hasHeader) 1 else 0).mapNotNull { line ->
            val cells = line.split(',').map { it.trim().trim('"') }
            val id = cells.getOrNull(idCol)?.removePrefix("t3_")?.takeIf { ID.matches(it) } ?: return@mapNotNull null
            if (dirCol >= 0 && cells.getOrNull(dirCol)?.lowercase() != "up") return@mapNotNull null
            id
        }.distinct()
    }

    private val ID = Regex("[a-z0-9]{1,12}")
}
