package tachiyomi.domain.character.interactor

import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.character.repository.CharacterRepository

class DeleteCharacter(
    private val characterRepository: CharacterRepository,
) {

    suspend fun await(id: Long) {
        try {
            characterRepository.delete(id)
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e)
        }
    }
}
