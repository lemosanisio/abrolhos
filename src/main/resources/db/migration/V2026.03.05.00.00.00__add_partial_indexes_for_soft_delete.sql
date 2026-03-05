-- Partial indexes scoped to active (non-deleted) rows.
--
-- All entities extend BaseEntity which carries @SQLRestriction("deleted_at IS NULL"),
-- so Hibernate always appends "AND deleted_at IS NULL" to every generated query.
-- These indexes match that predicate exactly, keeping them smaller than full indexes
-- and letting the planner satisfy the filter purely from the index without a heap recheck.

-- Posts: slug lookup (every post page view)
CREATE INDEX IF NOT EXISTS idx_posts_slug_active
    ON posts(slug)
    WHERE deleted_at IS NULL;

-- Posts: public feed (status + date ordering, the hot read path)
-- Supersedes the full idx_posts_status_published_at for regular queries;
-- that full index is kept for admin/native queries that bypass @SQLRestriction.
CREATE INDEX IF NOT EXISTS idx_posts_status_published_at_active
    ON posts(status, published_at DESC)
    WHERE deleted_at IS NULL;

-- Users: username lookup (every login and JWT validation)
CREATE INDEX IF NOT EXISTS idx_users_username_active
    ON users(username)
    WHERE deleted_at IS NULL;

-- Categories: slug lookup (category page / filter)
CREATE INDEX IF NOT EXISTS idx_categories_slug_active
    ON categories(slug)
    WHERE deleted_at IS NULL;

-- Tags: slug lookup (tag page / filter)
CREATE INDEX IF NOT EXISTS idx_tags_slug_active
    ON tags(slug)
    WHERE deleted_at IS NULL;

ANALYZE posts;
ANALYZE users;
ANALYZE categories;
ANALYZE tags;
