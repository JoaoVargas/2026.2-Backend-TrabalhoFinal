package br.pucpr.alexandria.authors

import br.pucpr.alexandria.books.BookRepository
import br.pucpr.alexandria.common.ConflictException
import br.pucpr.alexandria.common.NotFoundException
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.*
import org.springframework.data.domain.Sort
import java.util.Optional
import kotlin.test.assertEquals

class AuthorServiceTest {
    private val repository = mock<AuthorRepository>()
    private val bookRepository = mock<BookRepository>()
    private val service = AuthorService(repository, bookRepository)

    @Test
    fun `findAll returns authors sorted by name`() {
        val authors = listOf(Author(id = 1L, name = "Asimov"))
        whenever(repository.findAll(any<Sort>())).thenReturn(authors)

        val result = service.findAll()

        assertEquals(authors, result)
        verify(repository).findAll(Sort.by("name"))
    }

    @Test
    fun `insert delegates to the repository and returns the saved author`() {
        val input = Author(name = "Tolkien", bio = "writer")
        val saved = Author(id = 1L, name = "Tolkien", bio = "writer")
        whenever(repository.save(input)).thenReturn(saved)

        val result = service.insert(input)

        assertEquals(saved, result)
        verify(repository).save(input)
    }

    @Test
    fun `findByIdOrThrow returns the author when found`() {
        val author = Author(id = 1L, name = "Tolkien")
        whenever(repository.findById(1L)).thenReturn(Optional.of(author))

        assertEquals(author, service.findByIdOrThrow(1L))
    }

    @Test
    fun `findByIdOrThrow throws NotFoundException when missing`() {
        whenever(repository.findById(99L)).thenReturn(Optional.empty())

        assertThrows<NotFoundException> { service.findByIdOrThrow(99L) }
    }

    @Test
    fun `update changes name and bio then saves`() {
        val author = Author(id = 1L, name = "Old", bio = "old bio")
        whenever(repository.findById(1L)).thenReturn(Optional.of(author))
        whenever(repository.save(author)).thenReturn(author)

        val result = service.update(1L, AuthorRequest(name = "New", bio = "new bio"))

        assertEquals("New", result.name)
        assertEquals("new bio", result.bio)
        verify(repository).save(author)
    }

    @Test
    fun `update throws NotFoundException when the author doesn't exist`() {
        whenever(repository.findById(1L)).thenReturn(Optional.empty())

        assertThrows<NotFoundException> { service.update(1L, AuthorRequest(name = "New", bio = null)) }
    }

    @Test
    fun `deleteById removes the author when it has no books`() {
        val author = Author(id = 1L, name = "Tolkien")
        whenever(repository.findById(1L)).thenReturn(Optional.of(author))
        whenever(bookRepository.existsByAuthorId(1L)).thenReturn(false)

        service.deleteById(1L)

        verify(repository).delete(author)
    }

    @Test
    fun `deleteById throws ConflictException when the author still has books`() {
        val author = Author(id = 1L, name = "Tolkien")
        whenever(repository.findById(1L)).thenReturn(Optional.of(author))
        whenever(bookRepository.existsByAuthorId(1L)).thenReturn(true)

        assertThrows<ConflictException> { service.deleteById(1L) }
        verify(repository, never()).delete(any())
    }
}
