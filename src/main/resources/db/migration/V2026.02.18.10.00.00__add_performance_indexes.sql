-- Posts: status filter + date ordering (composite for the published feed)
CREATE INDEX IF NOT EXISTS idx_posts_status_published_at
    ON posts(status, published_at DESC);

-- Posts: created_at for admin ordering
CREATE INDEX IF NOT EXISTS idx_posts_created_at
    ON posts(created_at DESC);

-- Posts: author_id FK lookup
CREATE INDEX IF NOT EXISTS idx_posts_author_id
    ON posts(author_id);

-- Posts: category_id FK lookup
CREATE INDEX IF NOT EXISTS idx_posts_category_id
    ON posts(category_id);

-- Junction tables: reverse FK lookups
CREATE INDEX IF NOT EXISTS idx_post_tags_tag_id
    ON post_tags(tag_id);

-- Invites: token lookup + expiry filter
CREATE INDEX IF NOT EXISTS idx_invites_token
    ON invites(token);
CREATE INDEX IF NOT EXISTS idx_invites_expiry_date
    ON invites(expiry_date);

ANALYZE posts;
ANALYZE post_tags;
ANALYZE invites;
