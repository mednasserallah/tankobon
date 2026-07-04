package tachiyomi.domain.character.model

import androidx.compose.runtime.Immutable

/**
 * A reader-curated character note, scoped to a single series ([mangaId]).
 *
 * @param note optional free-text note; null when absent.
 * @param portraitPath absolute path to the saved square portrait JPEG on disk; null when absent.
 * @param createdAt epoch millis the character was saved.
 */
@Immutable
data class Character(
    val id: Long,
    val mangaId: Long,
    val name: String,
    val note: String?,
    val portraitPath: String?,
    val createdAt: Long,
) {
    companion object {
        fun create() = Character(
            id = -1L,
            mangaId = -1L,
            name = "",
            note = null,
            portraitPath = null,
            createdAt = 0L,
        )
    }
}
