package br.pucpr.alexandria.authors

import br.pucpr.alexandria.books.BookRepository
import br.pucpr.alexandria.common.ConflictException
import br.pucpr.alexandria.common.NotFoundException
import org.springframework.data.domain.Sort
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service

@Service
class AuthorService(
    private val repository: AuthorRepository,
    private val bookRepository: BookRepository
) {
    fun insert(author: Author): Author = repository.save(author)

    fun findAll(): List<Author> = repository.findAll(Sort.by("name"))

    fun findByIdOrThrow(id: Long): Author =
        repository.findByIdOrNull(id) ?: throw NotFoundException("Author $id not found!")

    fun update(id: Long, request: AuthorRequest): Author {
        val author = findByIdOrThrow(id)
        author.name = request.name!!
        author.bio = request.bio
        return repository.save(author)
    }

    fun deleteById(id: Long) {
        val author = findByIdOrThrow(id)
        if (bookRepository.existsByAuthorId(id)) {
            throw ConflictException("Author ${author.name} still has books and cannot be deleted")
        }
        repository.delete(author)
    }
}
