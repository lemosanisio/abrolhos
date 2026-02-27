package br.dev.demoraes.abrolhos.domain.entities

/**
 * Generic wrapper for cursor-based paginated results.
 *
 * Unlike offset-based pagination, cursor pagination uses a keyset (encoded as [nextCursor]) to
 * efficiently seek to the next page without scanning skipped rows.
 *
 * @param T the type of items in the page
 * @property items the list of items for the current page
 * @property nextCursor opaque cursor string to fetch the next page, null if no more pages
 * @property hasNext whether more pages exist after this one
 */
data class CursorPage<T>(
        val items: List<T>,
        val nextCursor: String?,
        val hasNext: Boolean,
)
