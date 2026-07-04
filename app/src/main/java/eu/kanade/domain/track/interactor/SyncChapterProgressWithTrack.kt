package eu.kanade.domain.track.interactor

import eu.kanade.tachiyomi.data.track.Tracker
import tachiyomi.domain.track.model.Track

class SyncChapterProgressWithTrack {

    suspend fun await(
        mangaId: Long,
        remoteTrack: Track,
        tracker: Tracker,
    ) {
        // No-op: enhanced trackers were the only consumers that synced local chapter
        // progress from a remote track, and enhanced trackers have been removed.
    }
}
