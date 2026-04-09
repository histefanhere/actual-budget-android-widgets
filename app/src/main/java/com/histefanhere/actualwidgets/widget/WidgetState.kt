package com.histefanhere.actualwidgets.widget

import retrofit2.HttpException

/**
 * Values stored under the `STATE_TYPE` preference key by widget workers
 * to drive the state machine rendered by each widget's `provideGlance`.
 */
object WidgetState {
    const val LOADING = "loading"
    const val NOT_CONFIGURED = "not_configured"
    const val ERROR = "error"
    const val SUCCESS = "success"
}

/**
 * Returns a human-readable error message.
 * For HTTP errors, includes the status code and the server's response body (if any).
 */
fun Exception.toErrorMessage(): String {
    if (this is HttpException) {
        val body = runCatching { response()?.errorBody()?.string()?.trim() }.getOrNull()
            ?.takeIf { it.isNotEmpty() }
        return "HTTP ${code()}" + if (body != null) ": $body" else ""
    }
    return message ?: "Network error"
}
