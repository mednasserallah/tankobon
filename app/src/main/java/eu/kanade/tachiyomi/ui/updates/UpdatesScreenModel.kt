package eu.kanade.tachiyomi.ui.updates

import android.app.Application
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import eu.kanade.core.preference.asState
import eu.kanade.core.util.addOrRemove
import eu.kanade.core.util.insertSeparators
import eu.kanade.domain.chapter.interactor.SetReadStatus
import eu.kanade.presentation.updates.UpdatesUiModel
import eu.kanade.tachiyomi.data.library.LibraryUpdateJob
import eu.kanade.tachiyomi.util.lang.toLocalDate
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import tachiyomi.core.common.preference.TriState
import tachiyomi.core.common.util.lang.launchIO
import tachiyomi.domain.chapter.interactor.GetChapter
import tachiyomi.domain.chapter.interactor.UpdateChapter
import tachiyomi.domain.chapter.model.ChapterUpdate
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.domain.updates.interactor.GetUpdates
import tachiyomi.domain.updates.model.UpdatesWithRelations
import tachiyomi.domain.updates.service.UpdatesPreferences
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.time.ZonedDateTime

class UpdatesScreenModel(
    private val updateChapter: UpdateChapter = Injekt.get(),
    private val setReadStatus: SetReadStatus = Injekt.get(),
    private val getUpdates: GetUpdates = Injekt.get(),
    private val getChapter: GetChapter = Injekt.get(),
    private val libraryPreferences: LibraryPreferences = Injekt.get(),
    private val updatesPreferences: UpdatesPreferences = Injekt.get(),
    val snackbarHostState: SnackbarHostState = SnackbarHostState(),
) : StateScreenModel<UpdatesScreenModel.State>(State()) {

    private val _events: Channel<Event> = Channel(Int.MAX_VALUE)
    val events: Flow<Event> = _events.receiveAsFlow()

    val lastUpdated by libraryPreferences.lastUpdatedTimestamp.asState(screenModelScope)

    // First and last selected index in list
    private val selectedPositions: Array<Int> = arrayOf(-1, -1)
    private val selectedChapterIds: HashSet<Long> = HashSet()

    init {
        screenModelScope.launchIO {
            // Set date limit for recent chapters
            val limit = ZonedDateTime.now().minusMonths(3).toInstant()

            getUpdatesItemPreferenceFlow()
                .distinctUntilChanged()
                .flatMapLatest {
                    getUpdates.subscribe(
                        limit,
                        unread = it.filterUnread.toBooleanOrNull(),
                        started = it.filterStarted.toBooleanOrNull(),
                        bookmarked = it.filterBookmarked.toBooleanOrNull(),
                        hideExcludedScanlators = it.filterExcludedScanlators,
                    ).distinctUntilChanged()
                }
                .map { it.toUpdateItems() }
                .collectLatest { updateItems ->
                    mutableState.update {
                        it.copy(
                            isLoading = false,
                            items = updateItems,
                        )
                    }
                }
        }

        getUpdatesItemPreferenceFlow()
            .map { prefs ->
                listOf(
                    prefs.filterUnread,
                    prefs.filterStarted,
                    prefs.filterBookmarked,
                )
                    .any { it != TriState.DISABLED }
            }
            .distinctUntilChanged()
            .onEach {
                mutableState.update { state ->
                    state.copy(hasActiveFilters = it)
                }
            }
            .launchIn(screenModelScope)
    }

    private fun List<UpdatesWithRelations>.toUpdateItems(): List<UpdatesItem> {
        return this
            .map { update ->
                UpdatesItem(
                    update = update,
                    selected = update.chapterId in selectedChapterIds,
                )
            }
    }

    fun updateLibrary(): Boolean {
        val started = LibraryUpdateJob.startNow(Injekt.get<Application>())
        screenModelScope.launch {
            _events.send(Event.LibraryUpdateTriggered(started))
        }
        return started
    }

    /**
     * Mark the selected updates list as read/unread.
     * @param updates the list of selected updates.
     * @param read whether to mark chapters as read or unread.
     */
    fun markUpdatesRead(updates: List<UpdatesItem>, read: Boolean) {
        screenModelScope.launchIO {
            setReadStatus.await(
                read = read,
                chapters = updates
                    .mapNotNull { getChapter.await(it.update.chapterId) }
                    .toTypedArray(),
            )
        }
        toggleAllSelection(false)
    }

    /**
     * Bookmarks the given list of chapters.
     * @param updates the list of chapters to bookmark.
     */
    fun bookmarkUpdates(updates: List<UpdatesItem>, bookmark: Boolean) {
        screenModelScope.launchIO {
            updates
                .filterNot { it.update.bookmark == bookmark }
                .map { ChapterUpdate(id = it.update.chapterId, bookmark = bookmark) }
                .let { updateChapter.awaitAll(it) }
        }
        toggleAllSelection(false)
    }

    fun toggleSelection(
        item: UpdatesItem,
        selected: Boolean,
        fromLongPress: Boolean = false,
    ) {
        mutableState.update { state ->
            val newItems = state.items.toMutableList().apply {
                val selectedIndex = indexOfFirst { it.update.chapterId == item.update.chapterId }
                if (selectedIndex < 0) return@apply

                val selectedItem = get(selectedIndex)
                if (selectedItem.selected == selected) return@apply

                val firstSelection = none { it.selected }
                set(selectedIndex, selectedItem.copy(selected = selected))
                selectedChapterIds.addOrRemove(item.update.chapterId, selected)

                if (selected && fromLongPress) {
                    if (firstSelection) {
                        selectedPositions[0] = selectedIndex
                        selectedPositions[1] = selectedIndex
                    } else {
                        // Try to select the items in-between when possible
                        val range: IntRange
                        if (selectedIndex < selectedPositions[0]) {
                            range = selectedIndex + 1..<selectedPositions[0]
                            selectedPositions[0] = selectedIndex
                        } else if (selectedIndex > selectedPositions[1]) {
                            range = (selectedPositions[1] + 1)..<selectedIndex
                            selectedPositions[1] = selectedIndex
                        } else {
                            // Just select itself
                            range = IntRange.EMPTY
                        }

                        range.forEach {
                            val inbetweenItem = get(it)
                            if (!inbetweenItem.selected) {
                                selectedChapterIds.add(inbetweenItem.update.chapterId)
                                set(it, inbetweenItem.copy(selected = true))
                            }
                        }
                    }
                } else if (!fromLongPress) {
                    if (!selected) {
                        if (selectedIndex == selectedPositions[0]) {
                            selectedPositions[0] = indexOfFirst { it.selected }
                        } else if (selectedIndex == selectedPositions[1]) {
                            selectedPositions[1] = indexOfLast { it.selected }
                        }
                    } else {
                        if (selectedIndex < selectedPositions[0]) {
                            selectedPositions[0] = selectedIndex
                        } else if (selectedIndex > selectedPositions[1]) {
                            selectedPositions[1] = selectedIndex
                        }
                    }
                }
            }
            state.copy(items = newItems)
        }
    }

    fun toggleAllSelection(selected: Boolean) {
        mutableState.update { state ->
            val newItems = state.items.map {
                selectedChapterIds.addOrRemove(it.update.chapterId, selected)
                it.copy(selected = selected)
            }
            state.copy(items = newItems)
        }

        selectedPositions[0] = -1
        selectedPositions[1] = -1
    }

    fun invertSelection() {
        mutableState.update { state ->
            val newItems = state.items.map {
                selectedChapterIds.addOrRemove(it.update.chapterId, !it.selected)
                it.copy(selected = !it.selected)
            }
            state.copy(items = newItems)
        }
        selectedPositions[0] = -1
        selectedPositions[1] = -1
    }

    fun setDialog(dialog: Dialog?) {
        mutableState.update { it.copy(dialog = dialog) }
    }

    fun resetNewUpdatesCount() {
        libraryPreferences.newUpdatesCount.set(0)
    }

    private fun getUpdatesItemPreferenceFlow(): Flow<ItemPreferences> {
        return combine(
            updatesPreferences.filterUnread.changes(),
            updatesPreferences.filterStarted.changes(),
            updatesPreferences.filterBookmarked.changes(),
            updatesPreferences.filterExcludedScanlators.changes(),
        ) { unread, started, bookmarked, excludedScanlators ->
            ItemPreferences(
                filterUnread = unread,
                filterStarted = started,
                filterBookmarked = bookmarked,
                filterExcludedScanlators = excludedScanlators,
            )
        }
    }

    fun showFilterDialog() {
        mutableState.update { it.copy(dialog = Dialog.FilterSheet) }
    }

    @Immutable
    private data class ItemPreferences(
        val filterUnread: TriState,
        val filterStarted: TriState,
        val filterBookmarked: TriState,
        val filterExcludedScanlators: Boolean,
    )

    @Immutable
    data class State(
        val isLoading: Boolean = true,
        val hasActiveFilters: Boolean = false,
        val items: List<UpdatesItem> = listOf(),
        val dialog: Dialog? = null,
    ) {
        val selected = items.filter { it.selected }
        val selectionMode = selected.isNotEmpty()

        fun getUiModel(): List<UpdatesUiModel> {
            return items
                .map { UpdatesUiModel.Item(it) }
                .insertSeparators { before, after ->
                    val beforeDate = before?.item?.update?.dateFetch?.toLocalDate()
                    val afterDate = after?.item?.update?.dateFetch?.toLocalDate()
                    when {
                        beforeDate != afterDate && afterDate != null -> UpdatesUiModel.Header(afterDate)
                        // Return null to avoid adding a separator between two items.
                        else -> null
                    }
                }
        }
    }

    sealed interface Dialog {
        data object FilterSheet : Dialog
    }

    sealed interface Event {
        data object InternalError : Event
        data class LibraryUpdateTriggered(val started: Boolean) : Event
    }
}

private fun TriState.toBooleanOrNull(): Boolean? {
    return when (this) {
        TriState.DISABLED -> null
        TriState.ENABLED_IS -> true
        TriState.ENABLED_NOT -> false
    }
}

@Immutable
data class UpdatesItem(
    val update: UpdatesWithRelations,
    val selected: Boolean = false,
)
