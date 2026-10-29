package br.pucpr.alexandria.books

import jakarta.validation.constraints.*
import java.math.BigDecimal

data class BookRequest(
    @NotBlank
    var title: String?,

    @NotBlank
    var isbn: String?,

    @NotNull
    @DecimalMin("0.0", inclusive = false)
    var price: BigDecimal?,

    @NotNull
    @Min(1500)
    var publishedYear: Int?,

    @NotNull
    @Min(0)
    var stock: Int?,

    @NotNull
    @Min(1)
    var authorId: Long?
)
