package br.pucpr.alexandria.books

import br.pucpr.alexandria.authors.Author
import br.pucpr.alexandria.authors.AuthorRepository
import br.pucpr.alexandria.categories.Category
import br.pucpr.alexandria.categories.CategoryRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import java.math.BigDecimal
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** [BookSpecifications] builds real JPA Criteria predicates, so a mocked repository (as used in
 * [BookServiceTest]) never actually runs them - only `@DataJpaTest`, against a real H2 database,
 * exercises the predicate logic itself. */
@DataJpaTest
class BookSpecificationsTest {
    @Autowired
    lateinit var bookRepository: BookRepository

    @Autowired
    lateinit var authorRepository: AuthorRepository

    @Autowired
    lateinit var categoryRepository: CategoryRepository

    private lateinit var tolkien: Author
    private lateinit var asimov: Author
    private lateinit var fantasy: Category
    private lateinit var sciFi: Category

    @BeforeEach
    fun seed() {
        tolkien = authorRepository.save(Author(name = "J.R.R. Tolkien"))
        asimov = authorRepository.save(Author(name = "Isaac Asimov"))
        fantasy = categoryRepository.save(Category(name = "Fantasy"))
        sciFi = categoryRepository.save(Category(name = "Science Fiction"))

        bookRepository.save(
            Book(
                title = "The Hobbit", isbn = "978-0-345-33968-3", price = BigDecimal("39.90"),
                publishedYear = 1937, stock = 10, author = tolkien, categories = mutableSetOf(fantasy)
            )
        )
        bookRepository.save(
            Book(
                title = "The Lord of the Rings", isbn = "978-0-618-15399-7", price = BigDecimal("89.90"),
                publishedYear = 1954, stock = 5, author = tolkien, categories = mutableSetOf(fantasy)
            )
        )
        bookRepository.save(
            Book(
                title = "Foundation", isbn = "978-0-553-29335-0", price = BigDecimal("49.90"),
                publishedYear = 1951, stock = 8, author = asimov, categories = mutableSetOf(sciFi)
            )
        )
    }

    @Test
    fun `no filters returns every book`() {
        val spec = BookSpecifications.build(null, null, null, null, null)

        assertEquals(3, bookRepository.findAll(spec).size)
    }

    @Test
    fun `filters by a case-insensitive partial title match`() {
        val spec = BookSpecifications.build("hobbit", null, null, null, null)

        val result = bookRepository.findAll(spec)

        assertEquals(1, result.size)
        assertEquals("The Hobbit", result[0].title)
    }

    @Test
    fun `filters by author id`() {
        val spec = BookSpecifications.build(null, asimov.id, null, null, null)

        val result = bookRepository.findAll(spec)

        assertEquals(1, result.size)
        assertEquals("Foundation", result[0].title)
    }

    @Test
    fun `filters by category id`() {
        val spec = BookSpecifications.build(null, null, sciFi.id, null, null)

        val result = bookRepository.findAll(spec)

        assertEquals(1, result.size)
        assertEquals("Foundation", result[0].title)
    }

    @Test
    fun `filters by a minimum price`() {
        val spec = BookSpecifications.build(null, null, null, BigDecimal("50.00"), null)

        val result = bookRepository.findAll(spec)

        assertEquals(1, result.size)
        assertEquals("The Lord of the Rings", result[0].title)
    }

    @Test
    fun `filters by a maximum price`() {
        val spec = BookSpecifications.build(null, null, null, null, BigDecimal("40.00"))

        val result = bookRepository.findAll(spec)

        assertEquals(1, result.size)
        assertEquals("The Hobbit", result[0].title)
    }

    @Test
    fun `combines multiple filters with AND semantics`() {
        val spec = BookSpecifications.build(null, tolkien.id, fantasy.id, BigDecimal("50.00"), BigDecimal("100.00"))

        val result = bookRepository.findAll(spec)

        assertEquals(1, result.size)
        assertEquals("The Lord of the Rings", result[0].title)
    }

    @Test
    fun `a price range with no matches returns nothing`() {
        val spec = BookSpecifications.build(null, null, null, BigDecimal("1000.00"), null)

        assertTrue(bookRepository.findAll(spec).isEmpty())
    }
}
