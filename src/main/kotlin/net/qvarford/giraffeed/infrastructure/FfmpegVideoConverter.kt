package net.qvarford.giraffeed.infrastructure

import net.qvarford.giraffeed.domain.HlsUrl
import net.qvarford.giraffeed.domain.Mp4
import net.qvarford.giraffeed.domain.VideoConverter
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock
import javax.enterprise.context.ApplicationScoped
import kotlin.concurrent.withLock

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

            Files.createDirectories(Path.of("generated"))

            val process = ProcessBuilder()
                .command("ffmpeg", "-i", url.value.toString(), "-c", "copy", "-movflags", "faststart", path.toString())
                .redirectErrorStream(true)
                .start()

            process.waitFor(30, TimeUnit.SECONDS)

            return Mp4(path)
        }
    }
}