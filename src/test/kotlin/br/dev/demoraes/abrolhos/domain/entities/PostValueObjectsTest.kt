package br.dev.demoraes.abrolhos.domain.entities

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class PostValueObjectsTest {

    @Test
    fun `PostTitle should be valid`() {
        val title = "Valid Post Title"
        val postTitle = PostTitle(title)
        assertEquals(title, postTitle.value)
    }

    @Test
    fun `PostTitle should throw exception when blank`() {
        assertThrows<IllegalArgumentException> {
            PostTitle("")
        }
        assertThrows<IllegalArgumentException> {
            PostTitle("   ")
        }
    }

    @Test
    fun `PostTitle should throw exception when too short`() {
        assertThrows<IllegalArgumentException> {
            PostTitle("ab")
        }
    }

    @Test
    fun `PostTitle should throw exception when too long`() {
        val longTitle = "a".repeat(256)
        assertThrows<IllegalArgumentException> {
            PostTitle(longTitle)
        }
    }

    @Test
    fun `PostSlug should be valid`() {
        val slug = "valid-post-slug-123"
        val postSlug = PostSlug(slug)
        assertEquals(slug, postSlug.value)
    }

    @Test
    fun `PostSlug should throw exception when invalid format`() {
        assertThrows<IllegalArgumentException> {
            PostSlug("Invalid Slug")
        }
        assertThrows<IllegalArgumentException> {
            PostSlug("invalid_slug")
        }
        assertThrows<IllegalArgumentException> {
            PostSlug("slug!")
        }
    }

    @Test
    fun `PostSlug should throw exception when too short`() {
        assertThrows<IllegalArgumentException> {
            PostSlug("ab")
        }
    }

    @Test
    fun `PostSlug should throw exception when too long`() {
        val longSlug = "a".repeat(256)
        assertThrows<IllegalArgumentException> {
            PostSlug(longSlug)
        }
    }

    @Test
    fun `PostContent should be valid`() {
        val content = "This is a valid post content."
        val postContent = PostContent(content)
        assertEquals(content, postContent.value)
    }

    @Test
    fun `PostContent should throw exception when blank`() {
        assertThrows<IllegalArgumentException> {
            PostContent("")
        }
        assertThrows<IllegalArgumentException> {
            PostContent("   ")
        }
    }
}
