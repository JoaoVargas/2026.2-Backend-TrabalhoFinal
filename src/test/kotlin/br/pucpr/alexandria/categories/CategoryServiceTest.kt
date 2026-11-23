package br.pucpr.alexandria.categories

import br.pucpr.alexandria.books.BookRepository
import br.pucpr.alexandria.common.ConflictException
import br.pucpr.alexandria.common.NotFoundException
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.*
import org.springframework.data.domain.Sort
import java.util.Optional
import kotlin.test.assertEquals

class CategoryServiceTest {
    private val repository = mock<CategoryRepository>()
    private val bookRepository = mock<BookRepository>()
    private val service = CategoryService(repository, bookRepository)

    @Test
    fun `findAll returns categories sorted by name`() {
        val categories = listOf(Category(id = 1L, name = "Fantasy"))
        whenever(repository.findAll(any<Sort>())).thenReturn(categories)

        val result = service.findAll()

        assertEquals(categories, result)
        verify(repository).findAll(Sort.by("name"))
    }

    @Test
    fun `insert saves a category with a unique name`() {
        val category = Category(name = "Fantasy")
        val saved = Category(id = 1L, name = "Fantasy")
        whenever(repository.findByNameIgnoreCase("Fantasy")).thenReturn(null)
        whenever(repository.save(category)).thenReturn(saved)

        val result = service.insert(category)

        assertEquals(saved, result)
    }

    @Test
    fun `insert throws ConflictException when the name is already taken`() {
        val category = Category(name = "Fantasy")
        whenever(repository.findByNameIgnoreCase("Fantasy")).thenReturn(Category(id = 1L, name = "fantasy"))

        assertThrows<ConflictException> { service.insert(category) }
        verify(repository, never()).save(any())
    }

    @Test
    fun `findByIdOrThrow throws NotFoundException when missing`() {
        whenever(repository.findById(1L)).thenReturn(Optional.empty())

        assertThrows<NotFoundException> { service.findByIdOrThrow(1L) }
    }

    @Test
    fun `deleteById removes the category when unused`() {
        val category = Category(id = 1L, name = "Fantasy")
        whenever(repository.findById(1L)).thenReturn(Optional.of(category))
        whenever(bookRepository.existsByCategoriesId(1L)).thenReturn(false)

        service.deleteById(1L)

        verify(repository).delete(category)
    }

    @Test
    fun `deleteById throws ConflictException when books still use the category`() {
        val category = Category(id = 1L, name = "Fantasy")
        whenever(repository.findById(1L)).thenReturn(Optional.of(category))
        whenever(bookRepository.existsByCategoriesId(1L)).thenReturn(true)

        assertThrows<ConflictException> { service.deleteById(1L) }
        verify(repository, never()).delete(any())
    }
}
