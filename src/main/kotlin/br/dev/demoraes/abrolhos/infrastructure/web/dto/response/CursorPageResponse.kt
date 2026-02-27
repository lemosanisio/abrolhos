package br.dev.demoraes.abrolhos.infrastructure.web.dto.response

/**
 * HTTP response DTO for cursor-paginated results.
 *
 * @param T the type of items in the response
 * @property items the list of items for the current page
 * @property nextCursor opaque cursor string to fetch the next page, null if this is the last page
 * @property hasNext whether more pages exist after this one
 */
data class CursorPageResponse<T>(
    val items: List<T>,
    val nextCursor: String?,
    val hasNext: Boolean,
)
