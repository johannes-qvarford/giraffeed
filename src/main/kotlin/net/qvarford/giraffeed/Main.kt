package net.qvarford.giraffeed

import io.quarkus.runtime.Quarkus

import io.quarkus.runtime.annotations.QuarkusMain
import java.io.File
import java.nio.file.Files
import java.nio.file.Path


@QuarkusMain
object Main {
    @JvmStatic
    fun main(args: Array<String>) {
        if (args.isEmpty() || args[0] != "keep-generated") {
            File("generated").deleteRecursively()
        }

        Files.createDirectories(Path.of("generated"))

        println("Running main method")
        Quarkus.run(*args)
    }
}