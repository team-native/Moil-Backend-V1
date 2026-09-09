package com.teamnative.moil.domain.image.service

import com.teamnative.moil.global.model.MoilColor
import org.springframework.stereotype.Component
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.IOException
import javax.imageio.ImageIO
import kotlin.math.roundToInt

/** Extracts the dominant RGB cluster and maps its centroid to the nearest app color. */
@Component
class ProfileColorExtractor {

    fun extract(imageBytes: ByteArray): String {
        val image = try {
            ImageIO.read(ByteArrayInputStream(imageBytes))
        } catch (_: IOException) {
            null
        } ?: return FALLBACK_COLOR
        val pixels = collectPixels(image)
        if (pixels.isEmpty()) return FALLBACK_COLOR

        val centroid = largestClusterCentroid(pixels)
        return MoilColor.nearest(
            centroid.red.roundToInt(),
            centroid.green.roundToInt(),
            centroid.blue.roundToInt(),
        ).id
    }

    private fun collectPixels(image: BufferedImage): List<Pixel> {
        val totalPixels = image.width.toLong() * image.height.toLong()
        val step = maxOf(1, kotlin.math.sqrt(totalPixels.toDouble() / MAX_SAMPLES).roundToInt())
        return buildList {
            for (y in 0 until image.height step step) {
                for (x in 0 until image.width step step) {
                    val argb = image.getRGB(x, y)
                    if ((argb ushr 24) >= MIN_ALPHA) {
                        add(
                            Pixel(
                                (argb shr 16 and 0xff).toDouble(),
                                (argb shr 8 and 0xff).toDouble(),
                                (argb and 0xff).toDouble(),
                            ),
                        )
                    }
                }
            }
        }
    }

    private fun largestClusterCentroid(pixels: List<Pixel>): Pixel {
        val clusterCount = minOf(CLUSTER_COUNT, pixels.size)
        var centroids = initialCentroids(pixels, clusterCount)
        var assignments = IntArray(pixels.size)

        repeat(MAX_ITERATIONS) {
            val nextAssignments = IntArray(pixels.size) { index ->
                centroids.indices.minBy { distance(pixels[index], centroids[it]) }
            }
            val nextCentroids = centroids.indices.map { cluster ->
                val members = pixels.indices.filter { nextAssignments[it] == cluster }
                if (members.isEmpty()) centroids[cluster]
                else Pixel(
                    members.sumOf { pixels[it].red }.toDouble() / members.size,
                    members.sumOf { pixels[it].green }.toDouble() / members.size,
                    members.sumOf { pixels[it].blue }.toDouble() / members.size,
                )
            }
            assignments = nextAssignments
            if (nextCentroids == centroids) return@repeat
            centroids = nextCentroids
        }

        val largestCluster = centroids.indices.maxBy { cluster -> assignments.count { it == cluster } }
        return centroids[largestCluster]
    }

    private fun initialCentroids(pixels: List<Pixel>, count: Int): List<Pixel> {
        val result = mutableListOf(pixels.first())
        while (result.size < count) {
            result += pixels.maxBy { pixel -> result.minOf { distance(pixel, it) } }
        }
        return result
    }

    private fun distance(first: Pixel, second: Pixel): Int {
        val red = first.red.roundToInt() - second.red.roundToInt()
        val green = first.green.roundToInt() - second.green.roundToInt()
        val blue = first.blue.roundToInt() - second.blue.roundToInt()
        return red * red + green * green + blue * blue
    }

    private data class Pixel(val red: Double, val green: Double, val blue: Double)

    companion object {
        private const val MAX_SAMPLES = 4096
        private const val CLUSTER_COUNT = 3
        private const val MAX_ITERATIONS = 10
        private const val MIN_ALPHA = 128
        private const val FALLBACK_COLOR = "RED"
    }
}
