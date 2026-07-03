package by.ciszkin.herdmanager.presentation.error

import androidx.compose.runtime.Composable
import by.ciszkin.herdmanager.domain.error.AppError
import by.ciszkin.herdmanager.domain.error.ConnectionError
import by.ciszkin.herdmanager.domain.error.HttpError
import by.ciszkin.herdmanager.domain.error.ModelNotFoundError
import by.ciszkin.herdmanager.domain.error.OllamaUnavailableError
import by.ciszkin.herdmanager.domain.error.ParsingError
import by.ciszkin.herdmanager.domain.error.TimeoutError
import by.ciszkin.herdmanager.domain.error.ValidationError
import by.ciszkin.herdmanager.domain.error.UnexpectedError
import herdmanager.shared.generated.resources.Res
import herdmanager.shared.generated.resources.error_data_parsing
import herdmanager.shared.generated.resources.error_data_validation
import herdmanager.shared.generated.resources.error_http_client_error
import herdmanager.shared.generated.resources.error_http_server_error
import herdmanager.shared.generated.resources.error_http_unknown
import herdmanager.shared.generated.resources.error_network_connection
import herdmanager.shared.generated.resources.error_network_timeout
import herdmanager.shared.generated.resources.error_server_model_not_found
import herdmanager.shared.generated.resources.error_server_ollama_unavailable
import herdmanager.shared.generated.resources.error_unexpected
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource

/**
 * The localized string resource for this error.
 *
 * Single source of truth for the error → message mapping; consumed by both
 * [toUserMessage] (composable) and [toUserMessageString] (suspend) so the two
 * never drift apart.
 */
fun AppError.toMessageResource(): StringResource = when (this) {
    is TimeoutError -> Res.string.error_network_timeout
    is ConnectionError -> Res.string.error_network_connection
    is HttpError -> when (statusCode) {
        in 400..499 -> Res.string.error_http_client_error
        in 500..599 -> Res.string.error_http_server_error
        else -> Res.string.error_http_unknown
    }
    is OllamaUnavailableError -> Res.string.error_server_ollama_unavailable
    is ModelNotFoundError -> Res.string.error_server_model_not_found
    is ParsingError -> Res.string.error_data_parsing
    is ValidationError -> Res.string.error_data_validation
    is UnexpectedError -> Res.string.error_unexpected
}

/** Composable accessor — for use inside @Composable UI (ErrorView, dialogs, inline text). */
@Composable
fun AppError.toUserMessage(): String = stringResource(toMessageResource())

/** Suspend accessor — for use in effect collectors and other non-composable scopes. */
suspend fun AppError.toUserMessageString(): String = getString(toMessageResource())