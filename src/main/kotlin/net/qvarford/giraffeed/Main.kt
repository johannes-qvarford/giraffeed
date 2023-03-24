package net.qvarford.giraffeed

import io.quarkus.runtime.Quarkus

import io.quarkus.runtime.annotations.QuarkusMain
import java.io.File


@QuarkusMain
object Main {
    @JvmStatic
    fun main(args: Array<String>) {
        if (args.isEmpty() || args[0] != "keep-generated") {
            File("generated").deleteRecursively()
        }

        println("Running main method")
        Quarkus.run(*args)
    }
}