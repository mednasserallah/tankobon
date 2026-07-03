package tachiyomi.domain.history.model

import java.util.Date

data class HistoryUpdate(
    val volumeId: Long,
    val readAt: Date,
    val sessionReadDuration: Long,
)
