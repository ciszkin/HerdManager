package by.ciszkin.herdmanager.domain.model

sealed class PullResult {
    data object Starting : PullResult()
    data class Progress(val progress: PullProgress) : PullResult()
    data object Completed : PullResult()
    data class Error(val message: String) : PullResult()
}
