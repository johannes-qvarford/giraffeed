package net.qvarford.giraffeed.domain

import java.io.FileInputStream
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import java.util.*

interface VideoConverter {
    fun convert(url: HlsUrl): Mp4
}

data class Mp4(private val path: Path) {
    fun stream(): FileInputStream {
        return FileInputStream(path.toString())
    }

    fun fileSize(): Long {
        return Files.size(path)
    }
}

data class HlsUrl(val value: URI) {
    val mp4Url: Mp4Url
        get() = Mp4Url(URI.create("https://giraffeed.privacy.qvarford.net/hls-to-mp4/${mp4FileName}"))

    val mp4FileName: String
        get() = "${String(Base64.getUrlEncoder().encode(value.toString().toByteArray()))}.mp4"
}

data class Mp4Url(val value: URI)