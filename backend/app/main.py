import io
import os
from pathlib import Path

import numpy as np
import tensorflow as tf
from fastapi import FastAPI, File, HTTPException, UploadFile
from fastapi.middleware.cors import CORSMiddleware
from PIL import Image


IMAGE_SIZE = int(os.getenv("IMAGE_SIZE", "256"))
ALLOWED_EXTENSIONS = {".jpg", ".jpeg", ".png", ".webp"}


def _resolve_model_path() -> Path:
    env_model_path = os.getenv("MODEL_PATH")
    if env_model_path:
        return Path(env_model_path).expanduser().resolve()
    return (Path(__file__).resolve().parents[2] / "tomatoes.keras").resolve()


def _resolve_class_names() -> list[str]:
    class_dir = Path(os.getenv("CLASS_DIR", Path(__file__).resolve().parents[2] / "dataset" / "train"))
    if class_dir.exists():
        classes = sorted([p.name for p in class_dir.iterdir() if p.is_dir()])
        if classes:
            return classes

    # Fallback class names if dataset directory is unavailable.
    return [
        "Tomato_Bacterial_spot",
        "Tomato_Early_blight",
        "Tomato_Late_blight",
        "Tomato_Leaf_Mold",
        "Tomato_Septoria_leaf_spot",
        "Tomato_Spider_mites_Two_spotted_spider_mite",
        "Tomato__Target_Spot",
        "Tomato__Tomato_YellowLeaf__Curl_Virus",
        "Tomato__Tomato_mosaic_virus",
        "Tomato_healthy",
    ]


MODEL_PATH = _resolve_model_path()
CLASS_NAMES = _resolve_class_names()
app = FastAPI(title="Tomato Disease Predictor", version="1.0.0")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


try:
    model = tf.keras.models.load_model(MODEL_PATH)
except Exception as exc:
    raise RuntimeError(f"Failed to load model from {MODEL_PATH}: {exc}") from exc


def preprocess_image(image_bytes: bytes) -> np.ndarray:
    try:
        image = Image.open(io.BytesIO(image_bytes)).convert("RGB")
    except Exception as exc:
        raise HTTPException(status_code=400, detail=f"Invalid image file: {exc}") from exc

    image = image.resize((IMAGE_SIZE, IMAGE_SIZE))
    image_array = np.array(image, dtype=np.float32) / 255.0
    return np.expand_dims(image_array, axis=0)


@app.get("/health")
def health() -> dict:
    return {
        "status": "ok",
        "model_path": str(MODEL_PATH),
        "num_classes": len(CLASS_NAMES),
    }


@app.post("/predict")
async def predict(file: UploadFile = File(...)) -> dict:
    file_ext = Path(file.filename or "").suffix.lower()
    if file_ext not in ALLOWED_EXTENSIONS:
        raise HTTPException(
            status_code=400,
            detail="Unsupported file type. Please upload jpg, jpeg, png, or webp.",
        )

    contents = await file.read()
    if not contents:
        raise HTTPException(status_code=400, detail="Uploaded file is empty.")

    image_batch = preprocess_image(contents)
    predictions = model.predict(image_batch, verbose=0)[0]

    predicted_idx = int(np.argmax(predictions))
    confidence = float(predictions[predicted_idx])

    top_k = min(3, len(predictions))
    top_indices = np.argsort(predictions)[::-1][:top_k]
    top_predictions = [
        {
            "class_name": CLASS_NAMES[int(idx)] if int(idx) < len(CLASS_NAMES) else f"Class_{int(idx)}",
            "confidence": float(predictions[int(idx)]),
        }
        for idx in top_indices
    ]

    class_name = CLASS_NAMES[predicted_idx] if predicted_idx < len(CLASS_NAMES) else f"Class_{predicted_idx}"
    return {
        "predicted_class": class_name,
        "confidence": confidence,
        "top_predictions": top_predictions,
    }
