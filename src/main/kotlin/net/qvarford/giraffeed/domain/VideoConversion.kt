package net.qvarford.giraffeed.domain

import java.io.File
import java.net.URI
import java.nio.file.Path
import java.util.*

interface VideoConverter {
    fun convert(url: HlsUrl): Mp4
}

data class Mp4(private val path: Path) {
    fun allBytes(): ByteArray {
        File(path.toString()).inputStream().use {
            return it.readAllBytes()
        }
    }
    fun byteRange(offset: Int, length: Int): ByteArray {
        File(path.toString()).inputStream().use {
            val bytes = ByteArray(length)
            it.read(bytes, offset, length)
            return bytes
        }
    }
}

data class HlsUrl(val value: URI) {
    val mp4Url: Mp4Url
        get() = Mp4Url(URI.create("https://giraffeed.privacy.qvarford.net/hls-to-mp4/${mp4FileName}"))

    val mp4FileName: String
        get() = "${String(Base64.getUrlEncoder().encode(value.toString().toByteArray()))}.mp4"
}

data class Mp4Url(val value: URI)