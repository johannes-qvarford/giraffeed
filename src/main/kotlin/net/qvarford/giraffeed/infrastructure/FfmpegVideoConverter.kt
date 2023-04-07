package net.qvarford.giraffeed.infrastructure

import net.qvarford.giraffeed.domain.HlsUrl
import net.qvarford.giraffeed.domain.Mp4
import net.qvarford.giraffeed.domain.VideoConverter
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock
import jakarta.enterprise.context.ApplicationScoped
import kotlin.concurrent.withLock

// TODO: Add File root argument to constructor
//  record temporary files downloaded by ffmpeg and host them with mockserver or wiremock. Check NOTES.md
//  Maybe this isn't worth it if we need to install ffmpeg on the runner
//  Also, all internal links need to rewritten to the mockserver base url
@ApplicationScoped
class FfmpegVideoConverter : VideoConverter {
    val locks = ConcurrentHashMap<HlsUrl, ReentrantLock>()

    override fun convert(url: HlsUrl): Mp4 {
        val lock = locks.computeIfAbsent(url) { ReentrantLock() }
        val path = Path.of("generated", url.mp4FileName);

        lock.withLock {
            if (Files.exists(path)) {
                return Mp4(path)
            }

            val process = ProcessBuilder()
                .command("ffmpeg", "-i", url.value.toString(), "-c", "copy", "-movflags", "faststart", path.toString())
                .redirectOutput(ProcessBuilder.Redirect.INHERIT)
                .redirectError(ProcessBuilder.Redirect.INHERIT)
                .start()

            process.waitFor(30, TimeUnit.SECONDS)

            return Mp4(path)
        }
    }
}