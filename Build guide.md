# AceSAT Education AI-Agent — Build Guide

> **Hackathon:** AceSAT Education AI-Agent
> **Deadline:** Aug 15, 2026 @ 5:00 PM EDT
> **Prize:** $100 (1st place)
> **Platform:** Android app (Kotlin) + NVIDIA Nemotron 3 Super (AI)
> **CI/CD:** GitHub Actions (build APK, downloadable from Releases)
> **Design Reference:** `C:\Users\user\Downloads\new\assets\design_inspire.webp` (MUST follow this design exactly)

---

## 1. What We Are Building

An **AI education agent** (NOT a chatbot) for underserved students that:

- **Diagnoses** where a student is struggling (SAT sections, math gaps, reading level)
- **Creates** a personalized study plan
- **Adapts** practice questions in real time based on performance
- **Tracks** progress over time and makes decisions on the student's behalf

Judge focus: *"agent takes action, adapts, tracks progress"* — that's our differentiator.

---

## 2. Tech Stack

| Layer | Choice | Why |
|---|---|---|
| App | Android + Kotlin (Jetpack Compose) | Mobile-first, native APK |
| AI Model | `nvidia/nemotron-3-super-120b-a12b` | Agentic reasoning, 1M context, function calling, reasoning ON/OFF |
| AI Access | NVIDIA NIM free endpoint | OpenAI-compatible, no GPU/infra needed |
| API Key Safety | Small backend proxy | NEVER embed API key in APK |
| Local Tracking | Room database | Student progress, weak areas, history |
| UI/Design | Follow `assets\design_inspire.webp` | The app UI MUST match this exact design |
| CI/CD | GitHub Actions | Build APK → upload artifact + attach to GitHub Release |
| Networking | Retrofit / OkHttp | OpenAI-compatible REST calls |

---

## 3. Critical Setup — Do This Before Building

### 3.1 Backend Proxy — Pick One Host (before Day 1)
The NVIDIA key must live server-side. Choose ONE and stick with it:
- **Vercel** (FastAPI/Python or Node) — easiest, free tier
- **Render** (FastAPI/Python) — simple
- **Firebase Functions** — if you already use Firebase

Set up the deploy command in the guide's Day 1 step 5 (e.g. `vercel deploy`).

### 3.2 Build Config — Pin Versions Now
Lock these so CI and your machine never mismatch:
```
compileSdk = 35
minSdk     = 24
targetSdk  = 35
JDK        = 17 (Temurin)
AGP        = 8.x (match your Android Studio)
Kotlin     = 2.x (match your Android Studio)
Gradle     = 8.x (wrapper, see 3.3)
```

### 3.3 Gradle Wrapper — The #1 CI Failure
`./gradlew` only exists if the wrapper is committed:
- Commit `gradlew`, `gradlew.bat`, and `gradle/wrapper/gradle-wrapper.jar` + `.properties`
- Set the execute bit on `gradlew` so Linux CI can run it:
  `git update-index --chmod=+x gradlew`
- Without these, the GitHub Actions APK build fails on first push.

### 3.4 GitHub Secrets
If the backend deploys via GitHub Actions, add these to repo **Settings → Secrets and variables → Actions**:
- `NVIDIA_API_KEY` (your `nvapi-...`)
- `VERCEL_TOKEN` / `VERCEL_ORG_ID` / `VERCEL_PROJECT_ID` (if using Vercel)

Reference them only in CI, never in the app.

### 3.5 Judging Tie-Back
Every demo moment must map to a judging criterion:
| Demo moment | Criterion it proves |
|---|---|
| Assessment quiz diagnoses weak areas | Agent **diagnoses** |
| Study plan generated from results | Agent **creates plan** |
| Next question changes after a wrong answer | Agent **adapts** |
| Dashboard shows score history | Agent **tracks progress** |
| App decides what to review next (not the user) | Agent **takes action** |

---

## 4. Architecture

```
┌─────────────────────────────┐
│   Android App (Kotlin)      │
│   - Study plan screens      │
│   - Quiz / practice flow    │
│   - Progress dashboard      │
│   - Room DB (progress)      │
└──────────────┬──────────────┘
               │ HTTPS (no API key exposed)
┌──────────────▼──────────────┐
│   Backend Proxy (your key)  │
│   - Keeps NVIDIA_API_KEY    │
│   - Forwards requests       │
│   - Optional: logging       │
└──────────────┬──────────────┘
               │ OpenAI-compatible API
┌──────────────▼──────────────┐
│   NVIDIA NIM (Free)         │
│   nemotron-3-super-120b-a12b│
│   - Diagnosis               │
│   - Study plan generation   │
│   - Question generation     │
│   - Adaptive difficulty     │
└─────────────────────────────┘
```

---

## 5. AI Integration — NVIDIA NIM

### Endpoint
```
Base URL: https://integrate.api.nvidia.com/v1
Model:    nvidia/nemotron-3-super-120b-a12b
```

### Reference params (from the model playground)
```
temperature = 1
top_p       = 0.95
max_tokens  = 16384
extra_body  = {
  "chat_template_kwargs": { "enable_thinking": true },
  "reasoning_budget": 16384
}
```

### Python reference (backend)
```python
from openai import OpenAI

client = OpenAI(
    base_url="https://integrate.api.nvidia.com/v1",
    api_key="nvapi-YOUR_KEY"
)

completion = client.chat.completions.create(
    model="nvidia/nemotron-3-super-120b-a12b",
    messages=[{
        "role": "system",
        "content": "You are an adaptive SAT tutor. Diagnose weak areas, generate personalized practice, and track progress."
    }, {
        "role": "user",
        "content": "I'm struggling with quadratic equations. Create a 20-minute study plan."
    }],
    temperature=1,
    top_p=0.95,
    max_tokens=16384,
    extra_body={"chat_template_kwargs": {"enable_thinking": True}, "reasoning_budget": 16384},
    stream=True
)

for chunk in completion:
    if not chunk.choices:
        continue
    reasoning = getattr(chunk.choices[0].delta, "reasoning_content", None)
    if reasoning:
        print(reasoning, end="")
    if chunk.choices[0].delta.content is not None:
        print(chunk.choices[0].delta.content, end="")
```

### Model capabilities to exploit
- **Reasoning ON/OFF** → ON for hard math, OFF for fast flashcard drills
- **1M context** → hold full student history + study plan in one session
- **Function calling / structured output** → drive adaptive question selection from app logic
- **Text only** (no images in/out)

---

## 6. Step-by-Step Build Plan (2 Days)

### Day 1 — Foundation
0. **Load the design** from `assets\design_inspire.webp` and map out every screen to match it exactly (colors, layout, components, spacing). Build the UI to that spec — this is the visual source of truth.
1. Scaffold Android project (Kotlin + Jetpack Compose + Room)
2. Build core screens:
   - Onboarding / skill assessment quiz
   - Dashboard (weak areas, plan, progress)
   - Practice screen (question → answer → feedback)
3. Set up Room DB schema: `students`, `attempts`, `weak_areas`, `study_plan`
4. Create GitHub repo

### Day 1 — AI wiring
5. Write backend proxy (host chosen in section 3.1) holding the NVIDIA key
6. Implement app → proxy → NIM request flow
7. Build agent logic in Kotlin:
   - Diagnosis: score per SAT section → weak area list
   - Plan: ask model for a study plan from weak areas
   - Adapt: pick next question based on last answers
8. Test a full student interaction loop end-to-end

### Day 2 — Finish & Submit
9. Progress dashboard (graph of scores over time)
10. Persist everything to Room so the agent "remembers" the student
11. GitHub Actions: APK build → artifact + Release download
12. Record demo video (≤ 3 min) on emulator/device
13. Write one-page write-up (problem → how it works → impact)
14. Polish README.md
15. **Submit before Aug 15, 5:00 PM EDT**

---

## 7. GitHub Actions — APK Build & Download

Push to `main` (or tag `v1.0`) → workflow builds `debugAPK` → uploads as artifact → optional auto-Release.

Workflow summary (`.github/workflows/build-apk.yml`):
```yaml
name: Build APK

on:
  push:
    branches: [main]
  workflow_dispatch:

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: 17
      - name: Setup Gradle
        uses: gradle/actions/setup-gradle@v4
      - name: Build debug APK
        run: ./gradlew assembleDebug
      - name: Upload APK artifact
        uses: actions/upload-artifact@v4
        with:
          name: app-debug-apk
          path: app/build/outputs/apk/debug/app-debug.apk
      - name: Attach to Release
        if: startsWith(github.ref, 'refs/tags/')
        uses: softprops/action-gh-release@v2
        with:
          files: app/build/outputs/apk/debug/app-debug.apk
```

Download path for judges:
- **Artifact tab** on the Actions run page, or
- **Releases → latest → app-debug.apk**

---

## 8. Security — Critical

- **NEVER** hardcode `nvapi-...` in the Android app (decompiling exposes it).
- Route all AI calls through the backend proxy.
- Do not send PII to the model; keep student data in Room locally.

---

## 9. Submission Checklist

- [ ] Working demo of the agent with a real student interaction flow
- [ ] Video ≤ 3 minutes (problem → solution → demo)
- [ ] GitHub repo with source code + clear README
- [ ] One-page write-up: problem, how the agent works, impact on underserved schools
- [ ] APK downloadable from GitHub (Releases or Actions artifact)
- [ ] Submitted before **Aug 15, 2026 5:00 PM EDT**

---

## 10. Useful Links

- **Design source (MUST match):** `C:\Users\user\Downloads\new\assets\design_inspire.webp`
- NVIDIA NIM free API key: https://build.nvidia.com/models
- Model page: `nvidia/nemotron-3-super-120b-a12b`
- Hackathon platform: AceSAT challenge page
