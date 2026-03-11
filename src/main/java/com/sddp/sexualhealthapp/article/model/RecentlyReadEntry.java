package com.sddp.sexualhealthapp.article.model;

import java.time.Instant;

/**
 * Persisted state for a recently read article.
 *
 * @param articleId            stable article identifier (markdown filename)
 * @param lastReadSectionIndex zero-based index of the last viewed H2 section
 * @param lastReadAt           timestamp of the last read activity
 */
public record RecentlyReadEntry(
        String articleId,
        int lastReadSectionIndex,
        Instant lastReadAt
) {
}
