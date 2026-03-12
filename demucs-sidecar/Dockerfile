FROM python:3.11-slim

# ffmpeg for FLAC re-encoding
RUN apt-get update && apt-get install -y --no-install-recommends \
    ffmpeg \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

COPY requirements.txt .
# Install torch CPU-only first to keep image smaller; GPU can be enabled via override
RUN pip install --no-cache-dir torch==2.3.0+cpu torchaudio==2.3.0+cpu \
    -f https://download.pytorch.org/whl/torch_stable.html && \
    pip install --no-cache-dir -r requirements.txt

COPY main.py .

# Pre-download the default htdemucs model weights at build time
RUN python -c "from demucs.pretrained import get_model; get_model('htdemucs')"

EXPOSE 8000

ENV AWS_S3_BUCKET=starjamz-uploads
ENV AWS_REGION=us-east-1
ENV DEMUCS_MODEL=htdemucs

CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "8000"]
