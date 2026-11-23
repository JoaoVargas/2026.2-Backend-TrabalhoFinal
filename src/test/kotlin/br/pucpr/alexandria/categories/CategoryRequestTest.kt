package br.pucpr.alexandria.categories

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class CategoryRequestTest {
    @Test
    fun `toCategory maps the request name onto a new category`() {
        val request = CategoryRequest(name = "Fantasy")

        val category = request.toCategory()

        assertEquals("Fantasy", request.name)
        assertEquals("Fantasy", category.name)
    }
}
