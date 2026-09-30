"""mitmproxy addon: forwards every HTTP(S) transaction to API Atlas (POST /api/traffic/ingest).

Run:  mitmdump -s atlas_addon.py
Env:  ATLAS_INGEST_URL, ATLAS_INGEST_TOKEN (required), ATLAS_SESSION_ID (optional discovery session id)
"""
import json
import os
import threading
import urllib.request

INGEST_URL = os.environ.get("ATLAS_INGEST_URL", "http://localhost:8080/api/traffic/ingest")
TOKEN = os.environ["ATLAS_INGEST_TOKEN"]
SESSION_ID = int(os.environ["ATLAS_SESSION_ID"]) if os.environ.get("ATLAS_SESSION_ID") else None
MAX_BODY = 1024 * 1024
BATCH_SIZE = 20


def _text(message):
    try:
        raw = message.get_content(strict=False) or b""
        return raw[:MAX_BODY].decode("utf-8", errors="replace")
    except Exception:
        return None


class AtlasForwarder:
    def __init__(self):
        self.batch = []
        self.lock = threading.Lock()

    def response(self, flow):
        req, res = flow.request, flow.response
        item = {
            "sessionId": SESSION_ID,
            "url": req.pretty_url,
            "method": req.method,
            "requestHeaders": {k.lower(): v for k, v in req.headers.items()},
            "requestBody": _text(req),
            "statusCode": res.status_code,
            "responseHeaders": {k.lower(): v for k, v in res.headers.items()},
            "responseBody": _text(res),
            "contentType": res.headers.get("content-type"),
            "responseTimeMs": int((res.timestamp_end - req.timestamp_start) * 1000) if res.timestamp_end else 0,
        }
        with self.lock:
            self.batch.append(item)
            if len(self.batch) < BATCH_SIZE:
                return
            payload, self.batch = self.batch, []
        threading.Thread(target=self._send, args=(payload,), daemon=True).start()

    def done(self):
        with self.lock:
            payload, self.batch = self.batch, []
        if payload:
            self._send(payload)

    @staticmethod
    def _send(payload):
        request = urllib.request.Request(
            INGEST_URL,
            data=json.dumps(payload).encode("utf-8"),
            headers={"Content-Type": "application/json", "X-Ingest-Token": TOKEN},
            method="POST",
        )
        try:
            urllib.request.urlopen(request, timeout=10).read()
        except Exception as exc:  # never break the proxied traffic
            print(f"[atlas] ingest failed: {exc}")


addons = [AtlasForwarder()]
