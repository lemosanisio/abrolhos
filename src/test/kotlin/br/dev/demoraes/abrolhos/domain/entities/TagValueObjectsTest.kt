package br.dev.demoraes.abrolhos.domain.entities

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class TagValueObjectsTest {

    @Test
    fun `TagName should be valid`() {
        val name = "kotlin"
        val tagName = TagName(name)
        assertEquals(name, tagName.value)
    }

    @Test
    fun `TagName should throw exception when blank`() {
        assertThrows<IllegalArgumentException> {
            TagName("")
        }
        assertThrows<IllegalArgumentException> {
            TagName("   ")
        }
    }

    @Test
    fun `TagName should throw exception when too short`() {
        assertThrows<IllegalArgumentException> {
            TagName("a")
        }
    }

    @Test
    fun `TagName should throw exception when too long`() {
        val longName = "a".repeat(101)
        assertThrows<IllegalArgumentException> {
            TagName(longName)
        }
    }

    @Test
    fun `TagSlug should be valid`() {
        val slug = "kotlin-123"
        val tagSlug = TagSlug(slug)
        assertEquals(slug, tagSlug.value)
    }

    @Test
    fun `TagSlug should throw exception when invalid format`() {
        assertThrows<IllegalArgumentException> {
            TagSlug("Invalid Tag")
        }
        assertThrows<IllegalArgumentException> {
            TagSlug("invalid_tag")
        }
    }

    @Test
    fun `TagSlug should throw exception when too short`() {
        assertThrows<IllegalArgumentException> {
            TagSlug("a")
        }
    }

    @Test
    fun `TagSlug should throw exception when too long`() {
        val longSlug = "a".repeat(101)
        assertThrows<IllegalArgumentException> {
            TagSlug(longSlug)
        }
    }
}
