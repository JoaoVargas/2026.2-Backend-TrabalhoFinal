package br.pucpr.alexandria.categories

import br.pucpr.alexandria.books.BookRepository
import br.pucpr.alexandria.common.ConflictException
import br.pucpr.alexandria.common.NotFoundException
import org.springframework.data.domain.Sort
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service

@Service
class CategoryService(
    private val repository: CategoryRepository,
    private val bookRepository: BookRepository
) {
    fun insert(category: Category): Category {
        if (repository.findByNameIgnoreCase(category.name) != null) {
            throw ConflictException("Category ${category.name} already exists")
        }
        return repository.save(category)
    }

    fun findAll(): List<Category> = repository.findAll(Sort.by("name"))

    fun findByIdOrThrow(id: Long): Category =
        repository.findByIdOrNull(id) ?: throw NotFoundException("Category $id not found!")

    fun deleteById(id: Long) {
        val category = findByIdOrThrow(id)
        if (bookRepository.existsByCategoriesId(id)) {
            throw ConflictException("Category ${category.name} is still assigned to books")
        }
        repository.delete(category)
    }
}
