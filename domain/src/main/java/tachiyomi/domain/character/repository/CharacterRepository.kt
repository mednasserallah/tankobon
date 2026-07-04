package tachiyomi.domain.character.repository

import kotlinx.coroutines.flow.Flow
import tachiyomi.domain.character.model.Character

interface CharacterRepository {

    suspend fun getById(id: Long): Character?

    suspend fun getByMangaId(mangaId: Long): List<Character>

    fun getByMangaIdAsFlow(mangaId: Long): Flow<List<Character>>

    suspend fun insert(character: Character): Long

    suspend fun update(character: Character)

    suspend fun delete(id: Long)
}
