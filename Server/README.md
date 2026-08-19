# FoodServer

`FoodServer` is the local Node.js server for the AIGlassFood Android app. It accepts food image and prompt requests from the phone on the same Wi-Fi network, validates the multipart payload in memory, and sends them to the NVIDIA Nemotron OpenAI-compatible API.

## Requirements

- Node.js 22 or newer
- npm 10 or newer
- The Android device and this PC must be on the same Wi-Fi network

## Environment Variables

Copy `.env.example` to `.env` and set the values:

- `NVIDIA_API_KEY`: NVIDIA API authentication key. Keep this only in `.env`.
- `NVIDIA_BASE_URL`: NVIDIA OpenAI-compatible API base URL.
- `NVIDIA_MODEL`: Default model name that will be used for recipe generation.
- `SERVER_PORT`: Port that the Android app will call. Default is `8000`.

## Install

```bash
npm install
```

## Run

Development mode:

```bash
npm run dev
```

Production-style run:

```bash
npm start
```

The server binds to `0.0.0.0`, so other devices on the same network can reach it.

## Verify Local Access

Health check from the PC:

```bash
curl http://localhost:8000/health
```

Health check from another device on the same Wi-Fi:

```text
http://<PC-LAN-IP>:8000/health
```

On Windows, you can find the local network IP with:

```powershell
ipconfig
```

Look for the IPv4 address of the active Wi-Fi adapter.

## Firewall Notes

If the Android device cannot reach the server:

1. Allow Node.js through Windows Defender Firewall.
2. Make sure the active network is marked as a private network.
3. Confirm the phone and PC are on the same Wi-Fi SSID.
4. Re-test `GET /health` before trying `POST /api/recipe`.

## API

### `GET /health`

Returns server status and current configuration summary.

### `POST /api/recipe`

Content type: `multipart/form-data`

Required fields:

- `image`: image file, up to 8 MB
- `prompt`: non-empty text after trimming whitespace

Optional fields:

- `language`: defaults to `ko-KR`

Successful responses return:

- `recipeText`
- `model`
- `elapsedMs`

The uploaded image is forwarded as a base64 data URI in an OpenAI-compatible `chat/completions` request.

## Tests

```bash
npm test
```

The automated tests cover:

- health endpoint
- valid multipart request
- missing image
- missing prompt
- invalid MIME type
- file size over 8 MB
- upstream timeout and auth error mapping
- NVIDIA client success, retry, and empty response handling
