package br.pucpr.alexandria.books

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.stereotype.Repository

@Repository
interface BookRepository : JpaRepository<Book, Long>, JpaSpecificationExecutor<Book> {
    fun existsByAuthorId(authorId: Long): Boolean
    fun existsByCategoriesId(categoryId: Long): Boolean
}
