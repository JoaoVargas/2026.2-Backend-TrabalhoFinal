package br.pucpr.alexandria.books

import br.pucpr.alexandria.categories.Category
import org.springframework.data.jpa.domain.Specification
import java.math.BigDecimal

object BookSpecifications {
    fun titleContains(title: String?): Specification<Book>? =
        title?.let { t ->
            Specification { root, _, cb -> cb.like(cb.lower(root.get("title")), "%${t.lowercase()}%") }
        }

    fun hasAuthor(authorId: Long?): Specification<Book>? =
        authorId?.let { id ->
            Specification { root, _, cb -> cb.equal(root.get<Any>("author").get<Long>("id"), id) }
        }

    fun hasCategory(categoryId: Long?): Specification<Book>? =
        categoryId?.let { id ->
            Specification { root, _, cb ->
                val categories = root.join<Book, Category>("categories")
                cb.equal(categories.get<Long>("id"), id)
            }
        }

    fun priceGreaterOrEqual(minPrice: BigDecimal?): Specification<Book>? =
        minPrice?.let { min ->
            Specification { root, _, cb -> cb.ge(root.get("price"), min) }
        }

    fun priceLessOrEqual(maxPrice: BigDecimal?): Specification<Book>? =
        maxPrice?.let { max ->
            Specification { root, _, cb -> cb.le(root.get("price"), max) }
        }

    fun build(
        title: String?,
        authorId: Long?,
        categoryId: Long?,
        minPrice: BigDecimal?,
        maxPrice: BigDecimal?
    ): Specification<Book> =
        Specification.allOf(
            listOfNotNull(
                titleContains(title),
                hasAuthor(authorId),
                hasCategory(categoryId),
                priceGreaterOrEqual(minPrice),
                priceLessOrEqual(maxPrice)
            )
        )
}
