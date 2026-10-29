package br.pucpr.alexandria

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class AlexandriaApplication

fun main(args: Array<String>) {
    runApplication<AlexandriaApplication>(*args)
}
