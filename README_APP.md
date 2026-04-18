# Tomato Disease Web App

This app includes:
- FastAPI backend for inference using `tomatoes.keras`
- React frontend for image upload and prediction display

## 1) Backend (FastAPI)

From project root:

```bash
cd backend
python -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
uvicorn app.main:app --reload --host 0.0.0.0 --port 8000
```

Health check:

```bash
curl http://127.0.0.1:8000/health
```

Optional environment variables:
- `MODEL_PATH` (default: `../tomatoes.keras`)
- `CLASS_DIR` (default: `../dataset/train`)
- `IMAGE_SIZE` (default: `256`)

## 2) Frontend (React + Vite)

Open another terminal from project root:

```bash
cd frontend
npm install
npm run dev
```

Open the URL shown by Vite (usually `http://127.0.0.1:5173`).

## 3) Predict

Upload a tomato leaf image and click **Predict**.

The UI shows:
- predicted class
- confidence
- top 3 class probabilities
