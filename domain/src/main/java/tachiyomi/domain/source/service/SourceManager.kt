package tachiyomi.domain.source.service

import eu.kanade.tachiyomi.source.Source
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface SourceManager {

    val isInitialized: StateFlow<Boolean>

    val sources: Flow<List<Source>>

    fun get(sourceKey: Long): Source?

    fun getOrStub(sourceKey: Long): Source

    fun getAll(): List<Source>
}
