package by.ciszkin.herdmanager.domain.model

import by.ciszkin.herdmanager.domain.error.AppError

sealed class PullResult {
    data object Starting : PullResult()
    data class Progress(val progress: PullProgress) : PullResult()
    data object Completed : PullResult()
    data class Error(val error: AppError) : PullResult()
}
