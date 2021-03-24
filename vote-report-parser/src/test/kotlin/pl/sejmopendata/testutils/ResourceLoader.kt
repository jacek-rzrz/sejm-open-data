package pl.sejmopendata.testutils

import java.io.FileNotFoundException

fun String.loadBytes(): ByteArray {
    val stream = object {}.javaClass.classLoader.getResourceAsStream(this)
        ?: throw FileNotFoundException(this)
    return stream.readAllBytes()
}
