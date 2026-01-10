package br.dev.demoraes.abrolhos.domain.entities

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class CategoryValueObjectsTest {

    @Test
    fun `CategoryName should be valid`() {
        val name = "Programming"
        val categoryName = CategoryName(name)
        assertEquals(name, categoryName.value)
    }

    @Test
    fun `CategoryName should throw exception when blank`() {
        assertThrows<IllegalArgumentException> {
            CategoryName("")
        }
        assertThrows<IllegalArgumentException> {
            CategoryName("   ")
        }
    }

    @Test
    fun `CategoryName should throw exception when too short`() {
        assertThrows<IllegalArgumentException> {
            CategoryName("a")
        }
    }

    @Test
    fun `CategoryName should throw exception when too long`() {
        val longName = "a".repeat(101)
        assertThrows<IllegalArgumentException> {
            CategoryName(longName)
        }
    }

    @Test
    fun `CategorySlug should be valid`() {
        val slug = "programming-123"
        val categorySlug = CategorySlug(slug)
        assertEquals(slug, categorySlug.value)
    }

    @Test
    fun `CategorySlug should throw exception when invalid format`() {
        assertThrows<IllegalArgumentException> {
            CategorySlug("Invalid Category")
        }
        assertThrows<IllegalArgumentException> {
            CategorySlug("invalid_category")
        }
    }

    @Test
    fun `CategorySlug should throw exception when too short`() {
        assertThrows<IllegalArgumentException> {
            CategorySlug("a")
        }
    }

    @Test
    fun `CategorySlug should throw exception when too long`() {
        val longSlug = "a".repeat(101)
        assertThrows<IllegalArgumentException> {
            CategorySlug(longSlug)
        }
    }
}
