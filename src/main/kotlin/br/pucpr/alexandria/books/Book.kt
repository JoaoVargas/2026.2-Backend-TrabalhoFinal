package br.pucpr.alexandria.books

import br.pucpr.alexandria.authors.Author
import br.pucpr.alexandria.categories.Category
import jakarta.persistence.*
import java.math.BigDecimal

@Entity
class Book(
    @Id @GeneratedValue
    var id: Long? = null,

    @Column(nullable = false)
    var title: String,

    @Column(nullable = false, unique = true, length = 20)
    var isbn: String,

    @Column(nullable = false)
    var price: BigDecimal,

    @Column(nullable = false)
    var publishedYear: Int,

    @Column(nullable = false)
    var stock: Int,

    @ManyToOne(optional = false)
    @JoinColumn(name = "idAuthor", nullable = false)
    var author: Author,

    @ManyToMany
    @JoinTable(
        name = "BookCategory",
        joinColumns = [JoinColumn(name = "idBook")],
        inverseJoinColumns = [JoinColumn(name = "idCategory")],
    )
    var categories: MutableSet<Category> = mutableSetOf()
)
