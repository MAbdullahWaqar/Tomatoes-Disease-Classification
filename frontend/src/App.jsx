import { useMemo, useState } from "react";

function formatLabel(label) {
  return label.replaceAll("__", " - ").replaceAll("_", " ");
}

function App() {
  const [selectedFile, setSelectedFile] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [result, setResult] = useState(null);

  const previewUrl = useMemo(() => {
    if (!selectedFile) return "";
    return URL.createObjectURL(selectedFile);
  }, [selectedFile]);

  async function onPredict() {
    if (!selectedFile) {
      setError("Please select an image first.");
      return;
    }

    setLoading(true);
    setError("");
    setResult(null);

    const formData = new FormData();
    formData.append("file", selectedFile);

    try {
      const response = await fetch("/predict", {
        method: "POST",
        body: formData
      });

      const data = await response.json();
      if (!response.ok) {
        throw new Error(data.detail || "Prediction failed.");
      }
      setResult(data);
    } catch (err) {
      setError(err.message || "Something went wrong.");
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="page">
      <div className="bg-orb bg-orb-1" />
      <div className="bg-orb bg-orb-2" />
      <main className="card">
        <h1>Tomato Disease Predictor</h1>
        <p className="subtitle">Upload a tomato leaf image and get the predicted disease with confidence.</p>

        <label className="upload-box">
          <input
            type="file"
            accept="image/png,image/jpeg,image/webp"
            onChange={(e) => {
              setSelectedFile(e.target.files?.[0] || null);
              setResult(null);
              setError("");
            }}
          />
          <span>{selectedFile ? selectedFile.name : "Choose an image (jpg, png, webp)"}</span>
        </label>

        {previewUrl && <img src={previewUrl} alt="preview" className="preview" />}

        <button type="button" onClick={onPredict} disabled={loading}>
          {loading ? "Predicting..." : "Predict"}
        </button>

        {error && <div className="error">{error}</div>}

        {result && (
          <section className="result">
            <h2>Prediction</h2>
            <p>
              <strong>Class:</strong> {formatLabel(result.predicted_class)}
            </p>
            <p>
              <strong>Confidence:</strong> {(result.confidence * 100).toFixed(2)}%
            </p>

            {Array.isArray(result.top_predictions) && result.top_predictions.length > 0 && (
              <div>
                <h3>Top Predictions</h3>
                <ul>
                  {result.top_predictions.map((item) => (
                    <li key={item.class_name}>
                      {formatLabel(item.class_name)} - {(item.confidence * 100).toFixed(2)}%
                    </li>
                  ))}
                </ul>
              </div>
            )}
          </section>
        )}
      </main>
    </div>
  );
}

export default App;
