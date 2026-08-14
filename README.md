# AceSAT Education AI-Agent

AceSAT is an intelligent, adaptive SAT tutor designed to help students in underserved schools improve their scores through active AI steering. It features a custom Neobrutalist UI and is powered by the `nvidia/nemotron-3-super-120b-a12b` model.

This repository contains:
1. **Android App (Kotlin)**: Jetpack Compose mobile client with local Room database and Retrofit integration.
2. **Backend Security Proxy**: Node.js Express server to securely forward queries to NVIDIA NIM and keep API keys safe.
3. **Web Preview Simulator**: An interactive single-page web app mimicking the Jetpack Compose client interface for local browser testing and presentation.

---

## 🚀 Key Features & Judging Tie-Backs

| Demo Moment | Implementation | Hackathon Judging Criterion |
| --- | --- | --- |
| **Diagnostic Quiz** | 3-question onboarding test that grades answers and identifies weaknesses. | **Agent Diagnoses** |
| **Personalized Study Plan** | Calls NVIDIA NIM to generate a 4-step study plan tailored to weak categories. | **Agent Creates Plan** |
| **Difficulty Scaling** | Adapts question difficulty in real-time based on right/wrong responses. | **Agent Adapts** |
| **Local Room Persistence** | Stores score progression, weak areas, and attempt logs locally. | **Agent Tracks Progress** |
| **Curriculum Navigation** | AI automatically selects the next focus area based on user performance. | **Agent Takes Action** |

---

## 🛠️ Tech Stack & Pin Versions

* **App Platform**: Android + Kotlin (Jetpack Compose)
* **Compile / Target SDK**: `35`
* **Minimum SDK**: `24`
* **JDK**: `17` (Temurin)
* **Gradle Wrapper**: `8.13`
* **AI Model**: `nvidia/nemotron-3-super-120b-a12b` (NVIDIA NIM Free Endpoint)
* **Database**: Room Database (Local Persistence)
* **Networking**: Retrofit + OkHttp
* **Security Proxy**: Node.js + Express + OpenAI SDK

---

## 📂 Repository Structure

```
├── .github/workflows/
│   └── build-apk.yml       # CI/CD pipeline to compile APK and release
├── app/
│   ├── build.gradle.kts    # App-level build configurations
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/acesat/education/
│       │   ├── MainActivity.kt        # Compose routing & UI screens
│       │   ├── agent/
│       │   │   └── AdaptiveAgent.kt   # Diagnosis, planning & adaptive logic
│       │   ├── data/
│       │   │   ├── api/
│       │   │   │   └── NvidiaService.kt  # Retrofit API interface
│       │   │   └── room/
│       │   │       ├── Student.kt     # Room entity definitions
│       │   │       ├── Attempt.kt
│       │   │       ├── WeakArea.kt
│       │   │       ├── StudyPlan.kt
│       │   │       └── AppDatabase.kt  # DB configurations and DAOs
│       │   └── ui/theme/
│       │       ├── Color.kt           # Neobrutalist colors
│       │       └── Theme.kt           # Custom Material Theme wrapper
│       └── res/
│           └── values/
│               ├── strings.xml
│               └── themes.xml
├── backend/
│   ├── .env.template       # Environment template
│   ├── package.json        # Node.js dependencies
│   └── server.js           # Secure Express proxy + offline fallback engine
├── web/
│   ├── index.html          # Web simulator DOM
│   ├── index.css           # Neobrutalist design implementation
│   └── app.js              # State engine and API service
├── build.gradle.kts        # Root build configuration
├── settings.gradle.kts     # Root settings configuration
├── gradle.properties       # Project properties and daemon configuration
├── gradlew                 # Gradle script for Linux/CI
├── gradlew.bat             # Gradle script for Windows
├── Build guide.md          # Reference build instructions
└── Write-up.md             # One-page hackathon summary
```

---

## ⚡ Setup & Installation

### 1. Run the Security Proxy
The proxy handles API keys securely and serves fallback mock questions when the key is omitted.

1. Navigate to the `backend` folder:
   ```bash
   cd backend
   ```
2. Install dependencies:
   ```bash
   npm install
   ```
3. (Optional) Set up your NVIDIA key:
   - Create a `.env` file from `.env.template`.
   - Add your key: `NVIDIA_API_KEY=nvapi-...`
   - *If no key is added, the server runs in Offline Demo Mode serving local questions.*
4. Start the server:
   ```bash
   npm start
   ```
   *The server runs at `http://localhost:3000`.*

### 2. Run the Web Preview Simulator
This is the easiest way to interact with the project and test the agent's logic.

1. Launch a local web server in the `web` folder. For example, using Python:
   ```bash
   cd web
   python -m http.server 8082
   ```
2. Open `http://localhost:8082` in your browser.
3. Complete the onboarding, take the quiz, and practice. The web app connects directly to the proxy server.

### 3. Build the Android App (APK)
The CI/CD pipeline builds the APK on every push to `main`.

To build locally:
1. Make sure you have **JDK 17** installed and added to your `JAVA_HOME`.
2. Run the gradle build command in the root folder:
   ```bash
   ./gradlew assembleDebug
   ```
3. The compiled APK will be located at:
   `app/build/outputs/apk/debug/app-debug.apk`

---

## 🎨 Neobrutalist Styling Guide
Following `assets/design_inspire.webp`, the design characteristics include:
- **Cream Background**: `#FDFBF7`
- **Pastel Accents**: Purple (`#A384FF`), Teal (`#00B6B9`), Pink (`#FFB4B4`).
- **Thick Borders**: Solid black `3px` or `4px` borders around cards, inputs, and buttons.
- **Flat Drop Shadows**: Black offset shadows without blur (`box-shadow: 4px 4px 0px #000000;`).
- **Press Effect**: Active state changes scale and shadow offsets.
- **Typography**: Geometric, bold headers (`Space Grotesk`).
