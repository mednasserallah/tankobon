package tachiyomi.domain.character.interactor

import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.character.model.Character
import tachiyomi.domain.character.repository.CharacterRepository

class UpsertCharacter(
    private val characterRepository: CharacterRepository,
) {

    /**
     * Inserts a new character (when [Character.id] is negative) or updates an
     * existing one. Returns the row id, or -1 on failure.
     */
    suspend fun await(character: Character): Long {
        return try {
            if (character.id < 0) {
                characterRepository.insert(character)
            } else {
                characterRepository.update(character)
                character.id
            }
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e)
            -1L
        }
    }
}
