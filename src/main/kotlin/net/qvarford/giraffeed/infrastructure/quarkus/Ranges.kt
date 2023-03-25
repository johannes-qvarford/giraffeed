
package net.qvarford.giraffeed.infrastructure.quarkus

/*
    Conforming to Range requests (https://developer.mozilla.org/en-US/docs/Web/HTTP/Range_requests) takes some effort.
    For now, the server will indicate that it doesn't support it, and return the entire file.
*/

/*
import java.io.FileInputStream
import java.io.InputStream
import java.nio.ByteBuffer

public fun fileRangeInputStream(fileInputStream: FileInputStream, ranges: Iterable<Range>): InputStream {
    val streams = ranges.map { RangeFileInputStream(fileInputStream, it) }.toList()
    return SequenceInputStream(streams)
}

class RangeFileInputStream(fileInputStream: FileInputStream, range: Range) : InputStream() {
    val buffer = ByteBuffer.allocate(range.size)

    init {
        fileInputStream.channel.read(buffer, range.min.toLong())
        buffer.rewind()
    }

    override fun read(): Int {
        if (buffer.position() >= buffer.limit()) {
            return -1
        }
        return buffer.get().toInt()
    }

}

sealed interface Range {

    fun bounded(fileSize: Long): BoundedRange

    companion object {
        fun rangesFromRangeHeaderValue(value: String): Iterable<Range> {
            val withoutUnitEquals = value.substring(value.indexOf('=') + 1)
            return withoutUnitEquals.split(", ")
                .map { rangeString -> rangeString.split('-').toTypedArray() }
                .map { fromBounds(it) }
                .toList()
        }

        private fun fromBounds(bounds: Array<String>): Range {
            if (bounds.isEmpty() || bounds.size > 2) {
                throw RequestRangeNotSatiableException()
            }

            return if (bounds[0].isEmpty()) {
                SuffixRange(amount = bounds[1].toLong())
            } else if (bounds[1].isEmpty()) {
                SkipRange(amount = bounds[0].toLong())
            } else {
                val start = bounds[0].toLong()
                val upperInclusive = bounds[1].toLong()
                BoundedRange(start = start, endExclusive = upperInclusive + 1)
            }
        }

        private fun
    }
}

data class SkipRange(val amount: Long): Range {
    override fun bounded(fileSize: Long): BoundedRange {
        if (amount < 0 || amount > fileSize) {
            throw RequestRangeNotSatiableException()
        }

        return BoundedRange(start = amount, endExclusive = fileSize)
    }
}

data class SuffixRange(val amount: Long) : Range {
    override fun bounded(fileSize: Long): BoundedRange {
        if (amount < 0 || amount > fileSize) {
            throw RequestRangeNotSatiableException()
        }

        return BoundedRange(start = fileSize - amount, endExclusive = fileSize)
    }
}

data class BoundedRange(val start: Long, val endExclusive: Long) : Range {
    private val size = endExclusive - start

    override fun bounded(fileSize: Long): BoundedRange {
        if (size < 0 || endExclusive > fileSize) {
            throw RequestRangeNotSatiableException()
        }

        return this
    }
}

class RequestRangeNotSatiableException : Exception()

private class SequenceInputStream(streams: Iterable<InputStream>) : InputStream() {

    val iterator = streams.iterator();
    var current: InputStream? = null;

    override fun read(): Int {
        if (current == null) {
            if (!iterator.hasNext()) {
                return -1
            }
            current = iterator.next()
        }

        while (true) {
            val toWrite = current!!.read()
            if (toWrite != -1) {
                return toWrite
            }
            if (!iterator.hasNext()) {
                return -1
            }
            current = iterator.next()
        }
    }
}*/
