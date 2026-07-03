package eu.kanade.tachiyomi.source

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import tachiyomi.domain.source.model.StubSource
import tachiyomi.domain.source.repository.StubSourceRepository
import tachiyomi.domain.source.service.SourceManager
import tachiyomi.source.local.LocalSource
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.util.concurrent.ConcurrentHashMap

class AndroidSourceManager(
    private val context: Context,
    private val sourceRepository: StubSourceRepository,
) : SourceManager {

    private val _isInitialized = MutableStateFlow(false)
    override val isInitialized: StateFlow<Boolean> = _isInitialized.asStateFlow()

    private val scope = CoroutineScope(Job() + Dispatchers.IO)

    private val sourcesMapFlow = MutableStateFlow(
        ConcurrentHashMap<Long, Source>(
            mapOf(LocalSource.ID to LocalSource(context, Injekt.get(), Injekt.get())),
        ),
    )

    private val stubSourcesMap = ConcurrentHashMap<Long, StubSource>()

    override val sources: Flow<List<Source>> = sourcesMapFlow.map { it.values.toList() }

    init {
        _isInitialized.value = true
        scope.launch {
            sourceRepository.subscribeAll().collectLatest { sources ->
                sources.forEach { stubSourcesMap[it.id] = it }
            }
        }
    }

    override fun get(sourceKey: Long): Source? = sourcesMapFlow.value[sourceKey]

    override fun getOrStub(sourceKey: Long): Source {
        return sourcesMapFlow.value[sourceKey] ?: stubSourcesMap.getOrPut(sourceKey) {
            runBlocking { createStubSource(sourceKey) }
        }
    }

    override fun getAll(): List<Source> = sourcesMapFlow.value.values.toList()

    override fun getStubSources(): List<StubSource> = stubSourcesMap.values.toList()

    private suspend fun createStubSource(id: Long): StubSource {
        sourceRepository.getStubSource(id)?.let { return it }
        return StubSource(id = id, lang = "", name = "")
    }
}
