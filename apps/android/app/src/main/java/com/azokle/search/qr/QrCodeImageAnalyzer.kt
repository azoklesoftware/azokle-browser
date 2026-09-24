package com.azokle.search.qr

import android.graphics.Bitmap
import android.graphics.ImageFormat
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import java.util.EnumMap

/**
 * High-performance, offline barcode and QR code analyzer for CameraX frames and static bitmaps.
 * Employs ZXing with zero external network or telemetry calls for maximum user privacy.
 *
 * Stride-Safe & De-Googled OS Hardened:
 * Handles Camera HAL row-stride padding (rowStride != width) and non-unit pixelStride
 * across custom privacy ROMs (GrapheneOS, LineageOS, CalyxOS, /e/OS) and varied hardware architectures.
 */
class QrCodeImageAnalyzer(
    private val onQrCodeScanned: (String) -> Unit
) : ImageAnalysis.Analyzer {

    private val reader = MultiFormatReader().apply {
        val hints = EnumMap<DecodeHintType, Any>(DecodeHintType::class.java).apply {
            put(
                DecodeHintType.POSSIBLE_FORMATS,
                listOf(
                    BarcodeFormat.QR_CODE,
                    BarcodeFormat.DATA_MATRIX,
                    BarcodeFormat.AZTEC,
                    BarcodeFormat.EAN_13,
                    BarcodeFormat.EAN_8,
                    BarcodeFormat.CODE_128,
                    BarcodeFormat.CODE_39,
                    BarcodeFormat.UPC_A,
                    BarcodeFormat.UPC_E
                )
            )
            put(DecodeHintType.TRY_HARDER, true)
            put(DecodeHintType.CHARACTER_SET, "UTF-8")
        }
        setHints(hints)
    }

    private var lastScannedTimestamp = 0L
    private val scanDebounceMs = 1500L
    private var isScanningEnabled = true

    fun setScanningEnabled(enabled: Boolean) {
        isScanningEnabled = enabled
    }

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val currentTime = System.currentTimeMillis()
        if (!isScanningEnabled || (currentTime - lastScannedTimestamp < scanDebounceMs)) {
            imageProxy.close()
            return
        }

        try {
            val image = imageProxy.image
            if (image != null && (image.format == ImageFormat.YUV_420_888 ||
                        image.format == ImageFormat.YUV_422_888 ||
                        image.format == ImageFormat.YUV_444_888)
            ) {
                val plane = image.planes[0]
                val buffer = plane.buffer
                val rowStride = plane.rowStride
                val pixelStride = plane.pixelStride
                val width = imageProxy.width
                val height = imageProxy.height
                val rotationDegrees = imageProxy.imageInfo.rotationDegrees

                // 1. Stride-Safe Luminance Extraction:
                // Tightly pack row-by-row Y data into a width*height buffer, removing any HAL row-stride padding
                val yData = ByteArray(width * height)
                val dupBuffer = buffer.duplicate()
                dupBuffer.rewind()

                if (rowStride == width && pixelStride == 1) {
                    dupBuffer.get(yData, 0, width * height)
                } else {
                    val rowBuffer = ByteArray(rowStride)
                    for (row in 0 until height) {
                        val rowStart = row * rowStride
                        if (pixelStride == 1) {
                            dupBuffer.position(rowStart)
                            val bytesToRead = minOf(dupBuffer.remaining(), width)
                            dupBuffer.get(yData, row * width, bytesToRead)
                        } else {
                            dupBuffer.position(rowStart)
                            val bytesToRead = minOf(dupBuffer.remaining(), rowStride)
                            dupBuffer.get(rowBuffer, 0, bytesToRead)
                            for (col in 0 until width) {
                                val offset = col * pixelStride
                                if (offset < bytesToRead) {
                                    yData[row * width + col] = rowBuffer[offset]
                                }
                            }
                        }
                    }
                }

                // 2. Rotate luminance bytes based on camera sensor rotation (portrait: 90 / 270)
                val (finalData, finalWidth, finalHeight) = when (rotationDegrees) {
                    90 -> Triple(rotateYUV90(yData, width, height), height, width)
                    180 -> Triple(rotateYUV180(yData, width, height), width, height)
                    270 -> Triple(rotateYUV270(yData, width, height), height, width)
                    else -> Triple(yData, width, height)
                }

                val source = PlanarYUVLuminanceSource(
                    finalData,
                    finalWidth,
                    finalHeight,
                    0,
                    0,
                    finalWidth,
                    finalHeight,
                    false
                )

                // 3. Dual-Pass Decoding: Standard binarization pass + Inverted pass (for dark mode QR codes)
                var result: com.google.zxing.Result? = null
                try {
                    result = reader.decodeWithState(BinaryBitmap(HybridBinarizer(source)))
                } catch (_: Exception) {
                    // Standard pass failed, attempt inverted pass
                }

                if (result == null) {
                    try {
                        result = reader.decodeWithState(BinaryBitmap(HybridBinarizer(source.invert())))
                    } catch (_: Exception) {
                        // Inverted pass failed
                    }
                }

                if (result != null && result.text.isNotBlank()) {
                    lastScannedTimestamp = currentTime
                    onQrCodeScanned(result.text)
                }
            }
        } catch (_: Exception) {
            // No barcode detected in the current frame; continue stream smoothly
        } finally {
            reader.reset()
            imageProxy.close()
        }
    }

    companion object {
        /**
         * Decode a QR or barcode directly from a Bitmap (e.g., loaded from device gallery).
         */
        fun decodeFromBitmap(bitmap: Bitmap): String? {
            return try {
                val width = bitmap.width
                val height = bitmap.height
                val pixels = IntArray(width * height)
                bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

                val source = RGBLuminanceSource(width, height, pixels)
                val reader = MultiFormatReader().apply {
                    val hints = EnumMap<DecodeHintType, Any>(DecodeHintType::class.java).apply {
                        put(
                            DecodeHintType.POSSIBLE_FORMATS,
                            listOf(
                                BarcodeFormat.QR_CODE,
                                BarcodeFormat.DATA_MATRIX,
                                BarcodeFormat.AZTEC,
                                BarcodeFormat.EAN_13,
                                BarcodeFormat.EAN_8,
                                BarcodeFormat.CODE_128,
                                BarcodeFormat.CODE_39,
                                BarcodeFormat.UPC_A,
                                BarcodeFormat.UPC_E
                            )
                        )
                        put(DecodeHintType.TRY_HARDER, true)
                        put(DecodeHintType.CHARACTER_SET, "UTF-8")
                    }
                    setHints(hints)
                }

                // Dual-Pass: Standard + Inverted
                var result: com.google.zxing.Result? = null
                try {
                    result = reader.decodeWithState(BinaryBitmap(HybridBinarizer(source)))
                } catch (_: Exception) {
                    // Try inverted pass
                }

                if (result == null) {
                    try {
                        result = reader.decodeWithState(BinaryBitmap(HybridBinarizer(source.invert())))
                    } catch (_: Exception) {
                        // Ignore
                    }
                }

                reader.reset()
                result?.text
            } catch (_: Exception) {
                null
            }
        }

        private fun rotateYUV90(data: ByteArray, width: Int, height: Int): ByteArray {
            val rotated = ByteArray(data.size)
            var i = 0
            for (x in 0 until width) {
                for (y in height - 1 downTo 0) {
                    rotated[i++] = data[y * width + x]
                }
            }
            return rotated
        }

        private fun rotateYUV180(data: ByteArray, width: Int, height: Int): ByteArray {
            val rotated = ByteArray(data.size)
            var i = 0
            for (y in height - 1 downTo 0) {
                for (x in width - 1 downTo 0) {
                    rotated[i++] = data[y * width + x]
                }
            }
            return rotated
        }

        private fun rotateYUV270(data: ByteArray, width: Int, height: Int): ByteArray {
            val rotated = ByteArray(data.size)
            var i = 0
            for (x in width - 1 downTo 0) {
                for (y in 0 until height) {
                    rotated[i++] = data[y * width + x]
                }
            }
            return rotated
        }
    }
}
