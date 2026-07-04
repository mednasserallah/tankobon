package tachiyomi.domain.character.interactor

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.character.model.Character
import tachiyomi.domain.character.repository.CharacterRepository

class GetCharactersByMangaId(
    private val characterRepository: CharacterRepository,
) {

    suspend fun await(mangaId: Long): List<Character> {
        return try {
            characterRepository.getByMangaId(mangaId)
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e)
            emptyList()
        }
    }

    fun subscribe(mangaId: Long): Flow<List<Character>> {
        return try {
            characterRepository.getByMangaIdAsFlow(mangaId)
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e)
            flowOf(emptyList())
        }
    }
}
