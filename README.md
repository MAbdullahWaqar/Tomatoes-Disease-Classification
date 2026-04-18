# Tomato Disease Classification

End-to-end tomato leaf disease classification project with three delivery targets:

1. A Python training workflow that produces a Keras model.
2. A FastAPI backend for image upload and inference.
3. Two client applications, a React web UI and an Android app powered by TensorFlow Lite.

The repository is organized so you can train or reuse the model, convert it to TFLite for mobile deployment, and serve the same classifier over HTTP for browser-based use.

## What This Project Does

The classifier predicts one of 10 tomato leaf health or disease categories from an input image. The model accepts RGB images resized to 256 x 256 and returns a prediction label plus confidence scores.

Supported classes are:

- Tomato_Bacterial_spot
- Tomato_Early_blight
- Tomato_Late_blight
- Tomato_Leaf_Mold
- Tomato_Septoria_leaf_spot
- Tomato_Spider_mites_Two_spotted_spider_mite
- Tomato__Target_Spot
- Tomato__Tomato_YellowLeaf__Curl_Virus
- Tomato__Tomato_mosaic_virus
- Tomato_healthy

## Repository Layout

- [Training.ipynb](Training.ipynb) - notebook used to inspect, train, evaluate, and export the model.
- [tomatoes.keras](tomatoes.keras) - saved Keras model used by the backend and conversion script.
- [convert_to_tflite.py](convert_to_tflite.py) - converts the Keras model into a TensorFlow Lite file for Android.
- [dataset/](dataset) - train, validation, and test splits in folder-per-class format.
- [backend/](backend) - FastAPI inference service.
- [frontend/](frontend) - Vite + React client that calls the backend.
- [android/](android) - Jetpack Compose Android app that runs the TFLite model on device.
- [PlantVillage/](PlantVillage) - source image folders for the tomato disease dataset.

## System Overview

The project supports two inference paths:

1. Web / API path: the React frontend uploads an image to the FastAPI backend at `/predict`, and the backend loads `tomatoes.keras` to generate a response.
2. Mobile path: the Android app loads `tomatoes.tflite` from app assets and performs inference locally on the device.

The shared model input size is 256 x 256 x 3, normalized to `[0, 1]`.

## Model And Training Notes

The training notebook builds a custom convolutional neural network and trains it on the tomato dataset using the directory structure under `dataset/`.

Key training characteristics visible in the notebook and exported artifacts:

- Input size: 256 x 256.
- Output classes: 10.
- Label source: directory names from the training set.
- Preprocessing: images are normalized by dividing by 255.0.
- Pipeline shape: TensorFlow `image_dataset_from_directory` for train, validation, and test splits.
- Model type: custom CNN with several `Conv2D` layers followed by dense classification layers.
- Loss: sparse categorical cross-entropy.
- Metric: accuracy.

The notebook also includes data augmentation, evaluation, and plotting cells for inspecting training behavior. The final saved model is [tomatoes.keras](tomatoes.keras).

## Dataset Structure

The dataset is already arranged in the standard TensorFlow directory format:

- `dataset/train/<class_name>/...`
- `dataset/val/<class_name>/...`
- `dataset/test/<class_name>/...`

Each split contains the same 10 class folders. The backend resolves the class list from `dataset/train` when available, and falls back to a built-in label list if the dataset is missing.

## Python Environment

The repository includes a backend requirements file, and the scripts expect a Python environment with TensorFlow installed.

Recommended Python packages for the backend and conversion workflow:

- fastapi
- uvicorn[standard]
- python-multipart
- pillow
- numpy
- tensorflow

## Backend

The FastAPI service lives in [backend/app/main.py](backend/app/main.py).

### Endpoints

#### `GET /health`

Returns a basic readiness response and exposes the resolved model path and class count.

Example response:

```json
{
  "status": "ok",
  "model_path": "/absolute/path/to/tomatoes.keras",
  "num_classes": 10
}
```

#### `POST /predict`

Accepts a multipart form upload with the field name `file`.

Accepted file types:

- `.jpg`
- `.jpeg`
- `.png`
- `.webp`

The backend:

1. Validates the extension.
2. Reads the uploaded bytes.
3. Converts the image to RGB.
4. Resizes it to 256 x 256.
5. Normalizes the pixels to float32 in `[0, 1]`.
6. Runs the Keras model.
7. Returns the top prediction and the top 3 classes.

Example response:

```json
{
  "predicted_class": "Tomato_Bacterial_spot",
  "confidence": 0.9812,
  "top_predictions": [
    {
      "class_name": "Tomato_Bacterial_spot",
      "confidence": 0.9812
    },
    {
      "class_name": "Tomato_Early_blight",
      "confidence": 0.0123
    },
    {
      "class_name": "Tomato_healthy",
      "confidence": 0.0041
    }
  ]
}
```

### Backend Environment Variables

The backend supports these environment variables:

- `MODEL_PATH` - absolute or relative path to the `.keras` model file.
- `CLASS_DIR` - directory used to resolve class names, defaulting to `dataset/train`.
- `IMAGE_SIZE` - image size used for preprocessing, default `256`.

### Run The Backend

From the repository root:

```bash
cd backend
python -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
uvicorn app.main:app --reload --host 127.0.0.1 --port 8000
```

The API will be available at `http://127.0.0.1:8000`.

## Frontend

The web client lives in [frontend/](frontend) and is built with React and Vite.

### Behavior

The UI allows the user to:

- select a local image file,
- preview the image before prediction,
- send the file to the backend,
- display the predicted class, confidence, and top predictions.

The Vite development server proxies `/predict` and `/health` to `http://127.0.0.1:8000`, so the frontend can call the backend with same-origin URLs during local development.

### Run The Frontend

```bash
cd frontend
npm install
npm run dev
```

Open the app at the Vite URL shown in the terminal, typically `http://127.0.0.1:5173`.

### Build For Production

```bash
cd frontend
npm run build
```

## TFLite Conversion

The conversion script is [convert_to_tflite.py](convert_to_tflite.py).

It loads [tomatoes.keras](tomatoes.keras) and writes the converted model to:

- `android/app/src/main/assets/tomatoes.tflite`

Run it from the repository root:

```bash
python convert_to_tflite.py
```

The script first tries the standard Keras-to-TFLite path and falls back to a concrete-function based conversion path if needed. This converted model is what the Android app uses at runtime.

## Android App

The Android client is located in [android/](android) and uses Kotlin, Jetpack Compose, CameraX, and TensorFlow Lite.

### Android Features

- Capture a photo from the camera.
- Select an image from the gallery.
- Run on-device inference with the bundled TFLite model.
- Display the best prediction, confidence, and ranked predictions.
- Handle image rotation and scaling before inference.

### Android Model Integration

The classifier implementation is in [android/app/src/main/java/com/tomato/disease/classifier/ml/TomatoDiseaseClassifier.kt](android/app/src/main/java/com/tomato/disease/classifier/ml/TomatoDiseaseClassifier.kt).

Important implementation details:

- The model file is loaded from `android/app/src/main/assets/tomatoes.tflite`.
- Input images are resized to 256 x 256.
- The interpreter uses the Flex delegate to support Select TF Ops.
- Predictions are mapped to the same 10 tomato classes used by the backend.

The app entry point is [android/app/src/main/java/com/tomato/disease/classifier/MainActivity.kt](android/app/src/main/java/com/tomato/disease/classifier/MainActivity.kt), and the main UI is [android/app/src/main/java/com/tomato/disease/classifier/ui/screens/MainScreen.kt](android/app/src/main/java/com/tomato/disease/classifier/ui/screens/MainScreen.kt).

### Android Permissions

The manifest requests:

- `android.permission.CAMERA`
- `android.permission.READ_MEDIA_IMAGES`
- `android.permission.READ_EXTERNAL_STORAGE`

### Android Build Notes

Project configuration highlights:

- compile SDK: 34
- target SDK: 34
- min SDK: 24
- Kotlin JVM target: 11
- UI toolkit: Jetpack Compose

### Run The Android App

Open the [android/](android) folder in Android Studio and build/run the app from there. Make sure the TFLite model exists in the assets directory before launching the app.

## End-To-End Workflow

If you want to reproduce the full pipeline from model to app, use this order:

1. Prepare or verify the dataset in `dataset/`.
2. Train or refresh the model in `Training.ipynb`.
3. Save the final model as `tomatoes.keras`.
4. Convert the model with `python convert_to_tflite.py`.
5. Start the FastAPI backend from `backend/`.
6. Start the React frontend from `frontend/`.
7. Open the Android app and test the bundled TFLite model.

## Troubleshooting

### Backend fails to load the model

Check that `tomatoes.keras` exists at the project root, or set `MODEL_PATH` to the correct location.

### Predictions fail with an unsupported file type

The API only accepts jpg, jpeg, png, and webp uploads.

### Frontend cannot reach the backend

Make sure the backend is running on `127.0.0.1:8000`. The frontend development server proxies `/predict` and `/health` to that address.

### Android build cannot find the TFLite model

Run [convert_to_tflite.py](convert_to_tflite.py) and confirm that `android/app/src/main/assets/tomatoes.tflite` exists.

### Android inference errors related to Flex ops

The app uses `org.tensorflow:tensorflow-lite-select-tf-ops` and a `FlexDelegate`. If the model was changed, reconvert it and verify that the new model is still compatible with the mobile runtime.

## Notes On Accuracy And Safety

This project is a computer vision classifier, not a medical diagnosis system. Predictions should be treated as decision support only, especially when the image quality is poor, the leaf is partially occluded, or multiple symptoms are present.


