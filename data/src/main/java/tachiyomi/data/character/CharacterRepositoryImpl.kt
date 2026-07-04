package tachiyomi.data.character

import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.async.coroutines.awaitAsOne
import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import kotlinx.coroutines.flow.Flow
import tachiyomi.data.Database
import tachiyomi.data.subscribeToList
import tachiyomi.domain.character.model.Character
import tachiyomi.domain.character.repository.CharacterRepository

class CharacterRepositoryImpl(
    private val database: Database,
) : CharacterRepository {

    override suspend fun getById(id: Long): Character? {
        return database.charactersQueries
            .getCharacterById(id, ::mapCharacter)
            .awaitAsOneOrNull()
    }

    override suspend fun getByMangaId(mangaId: Long): List<Character> {
        return database.charactersQueries
            .getCharactersByMangaId(mangaId, ::mapCharacter)
            .awaitAsList()
    }

    override fun getByMangaIdAsFlow(mangaId: Long): Flow<List<Character>> {
        return database.charactersQueries
            .getCharactersByMangaId(mangaId, ::mapCharacter)
            .subscribeToList()
    }

    override suspend fun insert(character: Character): Long {
        return database.charactersQueries.insertReturningId(
            mangaId = character.mangaId,
            name = character.name,
            note = character.note,
            portraitPath = character.portraitPath,
            createdAt = character.createdAt,
        ).awaitAsOne()
    }

    override suspend fun update(character: Character) {
        database.charactersQueries.update(
            name = character.name,
            note = character.note,
            portraitPath = character.portraitPath,
            id = character.id,
        )
    }

    override suspend fun delete(id: Long) {
        database.charactersQueries.delete(id = id)
    }

    private fun mapCharacter(
        id: Long,
        mangaId: Long,
        name: String,
        note: String?,
        portraitPath: String?,
        createdAt: Long,
    ): Character {
        return Character(
            id = id,
            mangaId = mangaId,
            name = name,
            note = note,
            portraitPath = portraitPath,
            createdAt = createdAt,
        )
    }
}
