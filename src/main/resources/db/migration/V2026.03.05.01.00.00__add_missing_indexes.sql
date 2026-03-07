-- Posts: slug lookup (UNIQUE constraint provides an implicit index, but explicit name aids management)
CREATE INDEX IF NOT EXISTS idx_posts_slug
    ON posts (slug);

-- Users: username lookup (same rationale as slug above)
CREATE INDEX IF NOT EXISTS idx_users_username
    ON users (username);

-- Categories: slug lookup for category-filtered post queries
CREATE INDEX IF NOT EXISTS idx_categories_slug
    ON categories (slug);

-- Tags: slug lookup for tag-filtered post queries
CREATE INDEX IF NOT EXISTS idx_tags_slug
    ON tags (slug);

-- post_tags: reverse FK lookup from post → tags (tag_id FK was already indexed)
CREATE INDEX IF NOT EXISTS idx_post_tags_post_id
    ON post_tags (post_id);

ANALYZE posts, users, categories, tags, post_tags;
