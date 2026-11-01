# J.A.R.V.I.S. (Just A Rather Very Intelligent System)

An advanced personal AI assistant for Android inspired by Tony Stark's Iron Man interface. Built using Kotlin, Jetpack Compose (Material 3), Room Database, Android SpeechRecognizer, Android TextToSpeech, and an intelligent hybrid neural engine supporting OpenAI API, custom backend proxies, and local device hardware controls.

---

## 1. Key Features

- **Holographic Arc Reactor HUD**: Animated central reactor with multi-layered concentric segmented rings, breathing energy glow, rotation transitions, and state-reactive animations (`IDLE`, `LISTENING`, `PROCESSING`, `SPEAKING`, `ERROR`).
- **Voice Intelligence**:
  - Speech-To-Text using Android's native `SpeechRecognizer`.
  - Real-time audio waveform visualizer synced to input volume RMS.
  - Natural speech output via `TextToSpeech` with adjustable velocity (speed), pitch, and multilingual support (English, Hindi, and Hinglish).
- **Advanced AI Reasoning**:
  - Connects to OpenAI API (`gpt-4o-mini`, `gpt-4o`, etc.) or secure backend proxies.
  - Contextual conversation memory stored in a local **Room Database**.
  - System prompt tuned to J.A.R.V.I.S.'s authentic British wit, politeness, and technical brilliance.
- **Local Device Integrations & Offline Protocols**:
  - **Flashlight Control**: Real torch toggle via `CameraManager`.
  - **Battery Telemetry**: Real-time battery percentage, charging state, and health.
  - **Application Launcher**: Intelligently searches and opens installed apps (e.g. YouTube, Maps, Camera, Chrome, Settings).
  - **Alarms & Reminders**: Creates system alarms via Android's `AlarmClock` provider.
  - **Diagnostics**: Live telemetry report covering uptime, memory reserves, and Android OS release.
  - **Offline Fallback**: Handles math calculations, date/time, and device controls even without internet connectivity.

---

## 2. Project Architecture

- **UI Layer (`com.example.ui`)**:
  - `JarvisMainScreen.kt`: The main cockpit containing the Arc Reactor, HUD telemetry header, quick actions, conversation stream, and bottom voice bar.
  - `components/ArcReactorView.kt`: Custom Canvas graphics with animated arcs and triangular palladium core.
  - `components/JarvisHudHeader.kt`: Clock, date, battery badge, voice toggle, settings, and mission logs.
  - `components/JarvisVoiceInputBar.kt`: Glowing microphone trigger and live audio waveform visualizer.
  - `components/JarvisChatView.kt`: Holographic chat cards with copy and speech replay buttons.
  - `components/JarvisSettingsDialog.kt`: Complete configuration console for API keys, models, voice parameters, and user title.
  - `components/JarvisBackendGuideDialog.kt`: In-app display of proxy server source code.
  - `JarvisViewModel.kt`: MVVM state management and event coordinator.
- **Service Layer (`com.example.service`)**:
  - `JarvisBrain.kt`: Hybrid query processor handling device commands locally and dispatching complex queries to the AI endpoint.
  - `JarvisVoiceManager.kt`: Speech-to-text listener and Text-To-Speech synthesizer.
  - `JarvisDeviceController.kt`: Android hardware APIs (Battery, Torch, Alarms, App Launcher).
- **Data Layer (`com.example.data`)**:
  - `ChatMessage.kt`: Room entity for conversation history.
  - `ChatMessageDao.kt`: Flow-based database access object.
  - `JarvisDatabase.kt`: Room database instance.
  - `JarvisPreferences.kt`: Persistent preferences for API keys, models, speech rates, and custom titles.

---

## 3. Secure Backend Proxy Source Code

To keep your OpenAI API key secure in production, run a lightweight backend proxy server.

### Option A: Node.js (Express)

Create `server.js`:
```javascript
require('dotenv').config();
const express = require('express');
const cors = require('cors');
const { OpenAI } = require('openai');

const app = express();
app.use(cors());
app.use(express.json());

const openai = new OpenAI({
  apiKey: process.env.OPENAI_API_KEY
});

app.get('/health', (req, res) => {
  res.json({ status: 'ONLINE', system: 'JARVIS Neural Core' });
});

app.post('/v1/chat/completions', async (req, res) => {
  try {
    const { messages, model = 'gpt-4o-mini', temperature = 0.7 } = req.body;
    const response = await openai.chat.completions.create({
      model,
      messages,
      temperature,
      max_tokens: 1000
    });
    res.json(response);
  } catch (err) {
    console.error('JARVIS routing error:', err);
    res.status(500).json({ error: err.message });
  }
});

const PORT = process.env.PORT || 8080;
app.listen(PORT, () => {
  console.log(`JARVIS Backend Core operational on port ${PORT}`);
});
```

Install and run:
```bash
npm install express cors dotenv openai
export OPENAI_API_KEY="your-key-here"
node server.js
```

### Option B: Python (FastAPI)

Create `main.py`:
```python
import os
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
from typing import List, Dict, Any
from openai import OpenAI
from dotenv import load_dotenv

load_dotenv()
app = FastAPI(title="JARVIS Neural Gateway")
client = OpenAI(api_key=os.getenv("OPENAI_API_KEY"))

class ChatRequest(BaseModel):
    model: str = "gpt-4o-mini"
    messages: List[Dict[str, Any]]
    temperature: float = 0.7

@app.get("/health")
def health():
    return {"status": "ONLINE", "protocol": "JARVIS v4"}

@app.post("/v1/chat/completions")
def chat_completions(req: ChatRequest):
    try:
        response = client.chat.completions.create(
            model=req.model,
            messages=req.messages,
            temperature=req.temperature,
            max_tokens=1000
        )
        return response.model_dump()
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))
```

Run:
```bash
pip install fastapi uvicorn openai python-dotenv
uvicorn main:app --host 0.0.0.0 --port 8080
```

---

## 4. Android Configuration & Permissions

The following permissions are configured in `AndroidManifest.xml`:
- `INTERNET`: For communicating with the AI backend.
- `ACCESS_NETWORK_STATE`: For detecting network status.
- `RECORD_AUDIO`: Runtime permission requested gracefully when clicking the microphone or Arc Reactor.
- `CAMERA` & `FLASHLIGHT`: For controlling the device torch hardware.
- `SET_ALARM`: For creating alarms and timers.
- Package Visibility (`<queries>`): Allows JARVIS to search installed applications by name and open them.

---

## 5. How to Build APK

From the project root:
```bash
gradle assembleDebug
```
The output APK will be generated at:
`app/build/outputs/apk/debug/app-debug.apk`

To run tests:
```bash
gradle :app:testDebugUnitTest
```
