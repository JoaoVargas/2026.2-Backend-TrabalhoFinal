package br.pucpr.alexandria.books

import br.pucpr.alexandria.authors.Author
import br.pucpr.alexandria.authors.AuthorService
import br.pucpr.alexandria.categories.Category
import br.pucpr.alexandria.categories.CategoryService
import br.pucpr.alexandria.common.ConflictException
import br.pucpr.alexandria.common.InvalidSortDirException
import br.pucpr.alexandria.common.NotFoundException
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.*
import org.springframework.data.domain.Sort
import org.springframework.data.jpa.domain.Specification
import java.math.BigDecimal
import java.util.Optional
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BookServiceTest {
    private val repository = mock<BookRepository>()
    private val authorService = mock<AuthorService>()
    private val categoryService = mock<CategoryService>()
    private val service = BookService(repository, authorService, categoryService)

    private val author = Author(id = 1L, name = "Tolkien")
    private fun book(id: Long = 1L, stock: Int = 10) = Book(
        id = id,
        title = "The Hobbit",
        isbn = "978-0-345-33968-3",
        price = BigDecimal("39.90"),
        publishedYear = 1937,
        stock = stock,
        author = author
    )

    @Test
    fun `search delegates to the repository with the requested sort`() {
        val books = listOf(book())
        whenever(repository.findAll(any<Specification<Book>>(), any<Sort>())).thenReturn(books)

        val result = service.search(null, null, null, null, null, "price", "DESC")

        assertEquals(books, result)
        verify(repository).findAll(any<Specification<Book>>(), eq(Sort.by("price").descending()))
    }

    @Test
    fun `search rejects an unsupported sort field`() {
        assertThrows<InvalidSortFieldException> {
            service.search(null, null, null, null, null, "unknown", "ASC")
        }
    }

    @Test
    fun `search rejects an invalid sort direction`() {
        assertThrows<InvalidSortDirException> {
            service.search(null, null, null, null, null, "title", "SIDEWAYS")
        }
    }

    @Test
    fun `findByIdOrThrow throws NotFoundException when missing`() {
        whenever(repository.findById(1L)).thenReturn(Optional.empty())

        assertThrows<NotFoundException> { service.findByIdOrThrow(1L) }
    }

    @Test
    fun `insert resolves the author and saves the book`() {
        val request = BookRequest(
            title = "The Hobbit", isbn = "978-0-345-33968-3",
            price = BigDecimal("39.90"), publishedYear = 1937, stock = 10, authorId = 1L
        )
        whenever(authorService.findByIdOrThrow(1L)).thenReturn(author)
        whenever(repository.save(any())).thenAnswer { it.arguments[0] }

        val result = service.insert(request)

        assertEquals("The Hobbit", result.title)
        assertEquals(author, result.author)
    }

    @Test
    fun `update overwrites all fields and resolves the new author`() {
        val existing = book()
        val newAuthor = Author(id = 2L, name = "Asimov")
        whenever(repository.findById(1L)).thenReturn(Optional.of(existing))
        whenever(authorService.findByIdOrThrow(2L)).thenReturn(newAuthor)
        whenever(repository.save(existing)).thenReturn(existing)

        val request = BookRequest(
            title = "Foundation", isbn = "978-0-553-29335-0",
            price = BigDecimal("49.90"), publishedYear = 1951, stock = 8, authorId = 2L
        )
        val result = service.update(1L, request)

        assertEquals("Foundation", result.title)
        assertEquals(newAuthor, result.author)
    }

    @Test
    fun `deleteById removes the book`() {
        val existing = book()
        whenever(repository.findById(1L)).thenReturn(Optional.of(existing))

        service.deleteById(1L)

        verify(repository).delete(existing)
    }

    @Test
    fun `addCategory attaches a new category and returns true`() {
        val existing = book()
        val category = Category(id = 1L, name = "Fantasy")
        whenever(repository.findById(1L)).thenReturn(Optional.of(existing))
        whenever(categoryService.findByIdOrThrow(1L)).thenReturn(category)

        val added = service.addCategory(1L, 1L)

        assertTrue(added)
        assertTrue(existing.categories.contains(category))
        verify(repository).save(existing)
    }

    @Test
    fun `addCategory is idempotent when already attached`() {
        val category = Category(id = 1L, name = "Fantasy")
        val existing = book().apply { categories.add(category) }
        whenever(repository.findById(1L)).thenReturn(Optional.of(existing))
        whenever(categoryService.findByIdOrThrow(1L)).thenReturn(category)

        val added = service.addCategory(1L, 1L)

        assertFalse(added)
        verify(repository, never()).save(any())
    }

    @Test
    fun `removeCategory detaches an existing category`() {
        val category = Category(id = 1L, name = "Fantasy")
        val existing = book().apply { categories.add(category) }
        whenever(repository.findById(1L)).thenReturn(Optional.of(existing))

        val removed = service.removeCategory(1L, 1L)

        assertTrue(removed)
        assertTrue(existing.categories.isEmpty())
        verify(repository).save(existing)
    }

    @Test
    fun `removeCategory returns false when the category isn't attached`() {
        val existing = book().apply { categories.add(Category(id = 2L, name = "Classics")) }
        whenever(repository.findById(1L)).thenReturn(Optional.of(existing))

        val removed = service.removeCategory(1L, 99L)

        assertFalse(removed)
        verify(repository, never()).save(any())
    }

    @Test
    fun `reserveStock decrements stock when there's enough`() {
        val existing = book(stock = 10)

        service.reserveStock(existing, 4)

        assertEquals(6, existing.stock)
        verify(repository).save(existing)
    }

    @Test
    fun `reserveStock throws ConflictException when stock is insufficient`() {
        val existing = book(stock = 2)

        assertThrows<ConflictException> { service.reserveStock(existing, 5) }
        assertEquals(2, existing.stock)
        verify(repository, never()).save(any())
    }

    @Test
    fun `releaseStock increments stock back`() {
        val existing = book(stock = 5)

        service.releaseStock(existing, 3)

        assertEquals(8, existing.stock)
        verify(repository).save(existing)
    }
}
