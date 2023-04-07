package net.qvarford.giraffeed.application

import net.qvarford.giraffeed.domain.HlsUrl
import net.qvarford.giraffeed.domain.Mp4
import net.qvarford.giraffeed.domain.VideoConverter
import jakarta.enterprise.context.ApplicationScoped

interface VideoConversionService {
    fun convert(url: HlsUrl): Mp4
}

@ApplicationScoped
class VideoConverterVideoConversionService(private val converter: VideoConverter) : VideoConversionService {
    override fun convert(url: HlsUrl): Mp4 {
        return converter.convert(url)
    }
}