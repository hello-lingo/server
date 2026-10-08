package com.lingo

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class LingoApplication

fun main(args: Array<String>) {
	runApplication<LingoApplication>(*args)
}
