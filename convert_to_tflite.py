"""Convert Keras model to TensorFlow Lite format for Android."""
import os
import tensorflow as tf
from pathlib import Path

MODEL_PATH = Path("tomatoes.keras")
TFLITE_PATH = Path("android/app/src/main/assets/tomatoes.tflite")

if not MODEL_PATH.exists():
    raise FileNotFoundError(f"Model not found at {MODEL_PATH}")

print(f"Loading model from {MODEL_PATH}...")
model = tf.keras.models.load_model(MODEL_PATH)
print(f"Model input shape: {model.input_shape}")
print(f"Model output shape: {model.output_shape}")

print("\nConverting to TensorFlow Lite...")
# Work around Keras model conversion issue by using concrete_func
try:
    # Attempt standard conversion first
    converter = tf.lite.TFLiteConverter.from_keras_model(model)
    converter.target_spec.supported_ops = [
        tf.lite.OpsSet.TFLITE_BUILTINS,
        tf.lite.OpsSet.SELECT_TF_OPS,
    ]
    converter.optimizations = [tf.lite.Optimize.DEFAULT]
    tflite_model = converter.convert()
except AttributeError:
    # Fallback: Convert via concrete function with TF Select
    print("Using fallback conversion method with TF Select...")
    import numpy as np
    
    # Create a concrete function with input specs
    @tf.function(input_signature=[
        tf.TensorSpec(shape=[1, 256, 256, 3], dtype=tf.float32)
    ])
    def model_func(x):
        return model(x, training=False)
    
    concrete_func = model_func.get_concrete_function()
    converter = tf.lite.TFLiteConverter.from_concrete_functions([concrete_func])
    converter.target_spec.supported_ops = [
        tf.lite.OpsSet.TFLITE_BUILTINS,
        tf.lite.OpsSet.SELECT_TF_OPS,
    ]
    converter.optimizations = [tf.lite.Optimize.DEFAULT]
    tflite_model = converter.convert()

# Ensure output directory exists
TFLITE_PATH.parent.mkdir(parents=True, exist_ok=True)

with open(TFLITE_PATH, "wb") as f:
    f.write(tflite_model)

print(f"✓ Model converted and saved to {TFLITE_PATH}")
print(f"✓ File size: {TFLITE_PATH.stat().st_size / 1024 / 1024:.2f} MB")
