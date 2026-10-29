package br.pucpr.alexandria.books

import br.pucpr.alexandria.authors.AuthorService
import br.pucpr.alexandria.categories.CategoryService
import br.pucpr.alexandria.common.ConflictException
import br.pucpr.alexandria.common.NotFoundException
import br.pucpr.alexandria.common.SortDir
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Sort
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.bind.annotation.ResponseStatus
import java.math.BigDecimal

private val SORTABLE_FIELDS = setOf("title", "price", "publishedYear", "stock")

@ResponseStatus(HttpStatus.BAD_REQUEST)
class InvalidSortFieldException(field: String) :
    IllegalArgumentException("Invalid sort field '$field'. Use one of: $SORTABLE_FIELDS")

@Service
class BookService(
    private val repository: BookRepository,
    private val authorService: AuthorService,
    private val categoryService: CategoryService
) {
    private val log = LoggerFactory.getLogger(BookService::class.java)

    fun search(
        title: String?,
        authorId: Long?,
        categoryId: Long?,
        minPrice: BigDecimal?,
        maxPrice: BigDecimal?,
        sortBy: String,
        sortDir: String
    ): List<Book> {
        if (sortBy !in SORTABLE_FIELDS) throw InvalidSortFieldException(sortBy)
        val direction = SortDir.findOrThrow(sortDir)
        val order = if (direction == SortDir.ASC) Sort.by(sortBy) else Sort.by(sortBy).descending()

        val spec = BookSpecifications.build(title, authorId, categoryId, minPrice, maxPrice)
        return repository.findAll(spec, order)
    }

    fun findByIdOrThrow(id: Long): Book =
        repository.findByIdOrNull(id) ?: throw NotFoundException("Book $id not found!")

    fun insert(request: BookRequest): Book {
        val author = authorService.findByIdOrThrow(request.authorId!!)
        val book = Book(
            id = null,
            title = request.title!!,
            isbn = request.isbn!!,
            price = request.price!!,
            publishedYear = request.publishedYear!!,
            stock = request.stock!!,
            author = author
        )
        val saved = repository.save(book)
        log.info("Book '{}' (isbn={}) created", saved.title, saved.isbn)
        return saved
    }

    fun update(id: Long, request: BookRequest): Book {
        val book = findByIdOrThrow(id)
        book.author = authorService.findByIdOrThrow(request.authorId!!)
        book.title = request.title!!
        book.isbn = request.isbn!!
        book.price = request.price!!
        book.publishedYear = request.publishedYear!!
        book.stock = request.stock!!
        return repository.save(book)
    }

    fun deleteById(id: Long) {
        val book = findByIdOrThrow(id)
        repository.delete(book)
        log.info("Book '{}' (isbn={}) deleted", book.title, book.isbn)
    }

    fun addCategory(bookId: Long, categoryId: Long): Boolean {
        val book = findByIdOrThrow(bookId)
        val category = categoryService.findByIdOrThrow(categoryId)
        if (!book.categories.add(category)) return false
        repository.save(book)
        log.info("Category '{}' added to book '{}'", category.name, book.title)
        return true
    }

    fun removeCategory(bookId: Long, categoryId: Long): Boolean {
        val book = findByIdOrThrow(bookId)
        val removed = book.categories.removeAll { it.id == categoryId }
        if (removed) {
            repository.save(book)
            log.info("Category {} removed from book '{}'", categoryId, book.title)
        }
        return removed
    }

    fun reserveStock(book: Book, quantity: Int) {
        if (book.stock < quantity) {
            log.warn("Insufficient stock for book '{}': requested {}, available {}", book.title, quantity, book.stock)
            throw ConflictException("Only ${book.stock} unit(s) of '${book.title}' left in stock")
        }
        book.stock -= quantity
        repository.save(book)
    }

    fun releaseStock(book: Book, quantity: Int) {
        book.stock += quantity
        repository.save(book)
    }
}
