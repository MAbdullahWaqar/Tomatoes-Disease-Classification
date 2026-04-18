package com.tomato.disease.classifier.ml

import android.content.Context
import android.graphics.Bitmap
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.flex.FlexDelegate
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.support.image.ops.ResizeOp.ResizeMethod
import org.tensorflow.lite.support.image.ops.TransformToGrayscaleOp
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

data class PredictionResult(
    val className: String,
    val confidence: Float,
    val allPredictions: Map<String, Float>
)

class TomatoDiseaseClassifier(context: Context) {
    private lateinit var interpreter: Interpreter
    private var imageProcessor: ImageProcessor
    private val classNames = listOf(
        "Tomato Bacterial spot",
        "Tomato Early blight",
        "Tomato Late blight",
        "Tomato Leaf Mold",
        "Tomato Septoria leaf spot",
        "Tomato Spider mites Two spotted spider mite",
        "Tomato Target Spot",
        "Tomato Tomato YellowLeaf Curl Virus",
        "Tomato mosaic virus",
        "Tomato healthy"
    )

    companion object {
        private const val MODEL_PATH = "tomatoes.tflite"
        private const val IMAGE_SIZE = 256
    }

    init {
        try {
            val model = loadModelFile(context, MODEL_PATH)
            interpreter = Interpreter(model, Interpreter.Options().apply {
                addDelegate(FlexDelegate())
                setNumThreads(4)
            })
        } catch (e: Exception) {
            throw RuntimeException("Failed to load TFLite model: ${e.message}")
        }

        imageProcessor = ImageProcessor.Builder()
            .add(ResizeOp(IMAGE_SIZE, IMAGE_SIZE, ResizeMethod.BILINEAR))
            .build()
    }

    private fun loadModelFile(context: Context, modelPath: String): MappedByteBuffer {
        val assetFileDescriptor = context.assets.openFd(modelPath)
        val fileInputStream = FileInputStream(assetFileDescriptor.fileDescriptor)
        val fileChannel = fileInputStream.channel
        val startOffset = assetFileDescriptor.startOffset
        val declaredLength = assetFileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    fun predict(bitmap: Bitmap): PredictionResult {
        // Convert bitmap to tensor
        var tensorImage = TensorImage(org.tensorflow.lite.DataType.FLOAT32)
        tensorImage.load(bitmap)
        tensorImage = imageProcessor.process(tensorImage)

        // Run inference
        val outputBuffer = ByteBuffer.allocateDirect(classNames.size * 4)
        outputBuffer.order(ByteOrder.nativeOrder())
        interpreter.run(tensorImage.buffer, outputBuffer)

        // Process results
        outputBuffer.rewind()
        val predictions = FloatArray(classNames.size)
        outputBuffer.asFloatBuffer().get(predictions)

        val maxIdx = predictions.indices.maxByOrNull { predictions[it] } ?: 0
        val maxConfidence = predictions[maxIdx]

        // Create prediction map
        val allPredictions = classNames.mapIndexed { idx, name ->
            name to predictions[idx]
        }.toMap()

        return PredictionResult(
            className = classNames[maxIdx],
            confidence = maxConfidence,
            allPredictions = allPredictions
        )
    }

    fun close() {
        interpreter.close()
    }
}
