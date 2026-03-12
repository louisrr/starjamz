"""
Demucs stem-separation sidecar service.

Accepts an S3 key, downloads the audio file, runs Demucs stem separation,
uploads the resulting stems back to S3, and returns a stem-type → S3-key map.

Endpoints
---------
POST /separate
    Body: { "s3Key": "uploads/audio/…/track.flac",
            "trackId": "<uuid>",
            "uploadId": "<uuid>",
            "tier": 1 | 2 }        # tier 1 = vocal+instrumental, tier 2 = 4-stem
    Response: { "stems": { "vocals": "stems/<trackId>/vocals/<uploadId>.flac", … } }

GET /health
    Response: { "status": "ok" }
"""

import io
import os
import subprocess
import tempfile
import uuid
from pathlib import Path
from typing import Literal

import boto3
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel

app = FastAPI(title="demucs-sidecar", version="1.0.0")

S3_BUCKET   = os.environ.get("AWS_S3_BUCKET", "starjamz-uploads")
AWS_REGION  = os.environ.get("AWS_REGION", "us-east-1")
DEMUCS_MODEL = os.environ.get("DEMUCS_MODEL", "htdemucs")

s3 = boto3.client("s3", region_name=AWS_REGION)


class SeparationRequest(BaseModel):
    s3Key: str
    trackId: str
    uploadId: str
    tier: Literal[1, 2] = 2


class SeparationResponse(BaseModel):
    stems: dict[str, str]


@app.get("/health")
def health():
    return {"status": "ok"}


@app.post("/separate", response_model=SeparationResponse)
def separate(req: SeparationRequest):
    """
    Download audio from S3, run Demucs, upload stems back to S3.
    tier=1 → vocal/instrumental split (faster, uses --two-stems=vocals).
    tier=2 → full 4-stem split (vocals, drums, bass, other→instruments).
    """
    with tempfile.TemporaryDirectory() as tmpdir:
        tmp_path = Path(tmpdir)
        src_file  = tmp_path / f"source_{uuid.uuid4()}.flac"

        # 1. Download from S3
        try:
            s3.download_file(S3_BUCKET, req.s3Key, str(src_file))
        except Exception as exc:
            raise HTTPException(status_code=404,
                                detail=f"S3 download failed: {exc}") from exc

        # 2. Run Demucs
        demucs_out = tmp_path / "demucs_out"
        demucs_out.mkdir()

        cmd = [
            "python", "-m", "demucs",
            "--out", str(demucs_out),
            "-n", DEMUCS_MODEL,
        ]
        if req.tier == 1:
            cmd += ["--two-stems", "vocals"]
        cmd.append(str(src_file))

        result = subprocess.run(cmd, capture_output=True, text=True)
        if result.returncode != 0:
            raise HTTPException(status_code=500,
                                detail=f"Demucs failed: {result.stderr}")

        # 3. Locate output stems (Demucs writes to <out>/<model>/<stem_name>.wav)
        stem_dirs = list(demucs_out.rglob("*.wav"))
        if not stem_dirs:
            raise HTTPException(status_code=500,
                                detail="Demucs produced no output files")

        # Map Demucs output names → canonical StemType names
        demucs_name_map = {
            "vocals":       "vocals",
            "drums":        "drums",
            "bass":         "bass",
            "other":        "instruments",
            "no_vocals":    "instruments",   # two-stem mode
        }

        stems: dict[str, str] = {}
        for wav_file in stem_dirs:
            raw_name   = wav_file.stem.lower()
            stem_type  = demucs_name_map.get(raw_name)
            if stem_type is None:
                continue  # skip unexpected outputs

            s3_key = f"stems/{req.trackId}/{stem_type}/{req.uploadId}.flac"

            # Re-encode to FLAC using ffmpeg for smaller size
            flac_file = wav_file.with_suffix(".flac")
            ffmpeg_cmd = [
                "ffmpeg", "-y", "-i", str(wav_file),
                "-c:a", "flac", str(flac_file)
            ]
            subprocess.run(ffmpeg_cmd, capture_output=True, check=True)

            # 4. Upload to S3
            with open(flac_file, "rb") as f:
                s3.upload_fileobj(f, S3_BUCKET, s3_key,
                                  ExtraArgs={"ContentType": "audio/flac"})

            stems[stem_type] = s3_key

        # Always include full_mix pointing to original upload
        stems["full_mix"] = req.s3Key

        return SeparationResponse(stems=stems)
