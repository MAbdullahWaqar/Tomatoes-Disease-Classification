package com.tomato.disease.classifier.ui.screens

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.tomato.disease.classifier.ml.TomatoDiseaseClassifier
import com.tomato.disease.classifier.ui.components.PredictionCard
import com.tomato.disease.classifier.utils.CameraHelper

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MainScreen() {
    val context = LocalContext.current
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var selectedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var predictionResult by remember { mutableStateOf<Pair<String, Float>?>(null) }
    var allPredictions by remember { mutableStateOf<Map<String, Float>>(emptyMap()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val cameraPermissionState = rememberPermissionState(android.Manifest.permission.CAMERA)
    val galleryPermissionState = rememberPermissionState(android.Manifest.permission.READ_MEDIA_IMAGES)

    fun runPrediction(bitmap: Bitmap, context: android.content.Context) {
        isLoading = true
        errorMessage = null
        val classifier = TomatoDiseaseClassifier(context)
        Thread {
            try {
                val result = classifier.predict(bitmap)
                predictionResult = result.className to result.confidence
                allPredictions = result.allPredictions.toList().sortedByDescending { it.second }.toMap()
            } catch (e: Exception) {
                errorMessage = "Prediction error: ${e.message}"
            } finally {
                isLoading = false
                classifier.close()
            }
        }.start()
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it
            val bitmap = CameraHelper.uriToBitmap(context, it)
            selectedBitmap = bitmap
            if (bitmap != null) {
                runPrediction(bitmap, context)
            }
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            val bitmap = CameraHelper.getBitmapFromFile(context)
            selectedBitmap = bitmap
            if (bitmap != null) {
                runPrediction(bitmap, context)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                    listOf(
                        Color(0xFF1B5E20),
                        Color(0xFF2E7D32)
                    )
                )
            )
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header
        Text(
            "🍅 Tomato Disease Detector",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(vertical = 16.dp)
        )

        // Image Preview
        if (selectedBitmap != null) {
            AsyncImage(
                model = selectedBitmap,
                contentDescription = "Selected image",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
                    .clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.height(16.dp))
        } else {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
                    .clip(RoundedCornerShape(12.dp)),
                color = Color.White.copy(alpha = 0.1f)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Text(
                        "No image selected\nTap buttons below to choose",
                        color = Color.White,
                        fontSize = 16.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Action Buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Camera Button
            Button(
                onClick = {
                    if (cameraPermissionState.status.isGranted) {
                        val uri = CameraHelper.createImageUri(context)
                        cameraLauncher.launch(uri)
                    } else {
                        cameraPermissionState.launchPermissionRequest()
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFFC107)
                )
            ) {
                Icon(Icons.Default.CameraAlt, contentDescription = "Camera", tint = Color.Black)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Camera", color = Color.Black, fontWeight = FontWeight.SemiBold)
            }

            // Gallery Button
            Button(
                onClick = {
                    if (galleryPermissionState.status.isGranted) {
                        galleryLauncher.launch("image/*")
                    } else {
                        galleryPermissionState.launchPermissionRequest()
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF4CAF50)
                )
            ) {
                Icon(Icons.Default.Image, contentDescription = "Gallery", tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Gallery", color = Color.White, fontWeight = FontWeight.SemiBold)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Loading Indicator
        if (isLoading) {
            CircularProgressIndicator(
                color = Color(0xFFFFC107),
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text("Analyzing image...", color = Color.White, fontSize = 14.sp)
        }

        // Error Message
        errorMessage?.let {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp)),
                color = Color(0xFFE53935)
            ) {
                Text(
                    it,
                    color = Color.White,
                    modifier = Modifier.padding(12.dp),
                    fontSize = 14.sp
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Prediction Results
        predictionResult?.let { (className, confidence) ->
            PredictionCard(
                className = className,
                confidence = confidence
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Top 3 Predictions
            Text(
                "All Predictions",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.align(Alignment.Start)
            )
            Spacer(modifier = Modifier.height(12.dp))

            allPredictions.entries.take(10).forEachIndexed { index, (disease, conf) ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp)),
                    color = Color.White.copy(alpha = 0.15f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "${index + 1}. ${disease.take(30)}",
                            color = Color.White,
                            fontSize = 13.sp,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            "${(conf * 100).toInt()}%",
                            color = Color(0xFFFFC107),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                if (index < allPredictions.size - 1) {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
