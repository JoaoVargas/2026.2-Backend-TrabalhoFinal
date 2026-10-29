package br.pucpr.alexandria

import br.pucpr.alexandria.authors.Author
import br.pucpr.alexandria.authors.AuthorRepository
import br.pucpr.alexandria.books.Book
import br.pucpr.alexandria.books.BookRepository
import br.pucpr.alexandria.categories.Category
import br.pucpr.alexandria.categories.CategoryRepository
import br.pucpr.alexandria.roles.Role
import br.pucpr.alexandria.roles.RoleRepository
import br.pucpr.alexandria.users.User
import br.pucpr.alexandria.users.UserRepository
import jakarta.transaction.Transactional
import org.springframework.context.ApplicationListener
import org.springframework.context.event.ContextRefreshedEvent
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component
import java.math.BigDecimal

/** Seeds reference data so the API is explorable right after startup, both locally and in Docker. */
@Component
class BootStrapper(
    private val roleRepository: RoleRepository,
    private val userRepository: UserRepository,
    private val authorRepository: AuthorRepository,
    private val categoryRepository: CategoryRepository,
    private val bookRepository: BookRepository,
    private val passwordEncoder: PasswordEncoder
) : ApplicationListener<ContextRefreshedEvent> {

    @Transactional
    override fun onApplicationEvent(event: ContextRefreshedEvent) {
        val adminRole = roleRepository.findByName("ADMIN")
            ?: roleRepository.save(Role(id = null, name = "ADMIN", description = "System administrator"))
        roleRepository.findByName("CUSTOMER")
            ?: roleRepository.save(Role(id = null, name = "CUSTOMER", description = "Bookstore customer"))

        if (userRepository.findByRole("ADMIN").isEmpty()) {
            val admin = User(
                name = "Administrator",
                passwordHash = passwordEncoder.encode("admin123")!!,
                email = "admin@alexandria.com"
            )
            admin.roles.add(adminRole)
            userRepository.save(admin)
        }

        if (authorRepository.count() == 0L) {
            val tolkien = authorRepository.save(Author(name = "J.R.R. Tolkien", bio = "English writer and philologist"))
            val asimov = authorRepository.save(Author(name = "Isaac Asimov", bio = "American writer and biochemist"))

            val fantasy = categoryRepository.save(Category(name = "Fantasy"))
            val sciFi = categoryRepository.save(Category(name = "Science Fiction"))
            val classics = categoryRepository.save(Category(name = "Classics"))

            bookRepository.save(
                Book(
                    title = "The Hobbit",
                    isbn = "978-0-345-33968-3",
                    price = BigDecimal("39.90"),
                    publishedYear = 1937,
                    stock = 10,
                    author = tolkien,
                    categories = mutableSetOf(fantasy, classics)
                )
            )
            bookRepository.save(
                Book(
                    title = "The Lord of the Rings",
                    isbn = "978-0-618-15399-7",
                    price = BigDecimal("89.90"),
                    publishedYear = 1954,
                    stock = 5,
                    author = tolkien,
                    categories = mutableSetOf(fantasy)
                )
            )
            bookRepository.save(
                Book(
                    title = "Foundation",
                    isbn = "978-0-553-29335-0",
                    price = BigDecimal("49.90"),
                    publishedYear = 1951,
                    stock = 8,
                    author = asimov,
                    categories = mutableSetOf(sciFi, classics)
                )
            )
        }
    }
}
