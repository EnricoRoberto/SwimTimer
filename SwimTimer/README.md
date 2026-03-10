# 🏊 SwimTimer — Cronometro Nuoto Android

App cronometro professionale per il nuoto con animazioni acquatiche, tema piscina e cronologia gare.

## ✨ Novità rispetto alla versione base

### 🌊 Animazioni tematiche nuoto
- **Onde animate** in tempo reale (3 livelli sovrapposti con frequenze diverse)
- **Nuotatore animato** nella barra in alto che "nuota" mentre il cronometro è attivo
- **Corde di corsia** (lane rope) arancioni come decorazione nei titoli
- **Sfondo pool-blue** con gradiente profondità acqua su tutte le schermate
- Tema **"Piscina Giorno"** (blu oceano), **"Piscina Notte"** (deep blue scuro), **"Notturna Viola"** (lilla acqua)
- Tempo totale mostrato in **oro** come un podio

## 📲 Come compilare

### Opzione 1 — Android Studio (consigliata)
1. Apri Android Studio → **File → Open** → cartella `SwimTimer`
2. Attendi sync Gradle
3. ▶️ Run su dispositivo/emulatore

### Opzione 2 — VS Code
1. Installa estensione **"Gradle for Java"** e **"Android iOS Emulator"**
2. Apri la cartella del progetto
3. Da terminale: `./gradlew assembleDebug`

### Opzione 3 — Solo riga di comando (JDK + Android SDK)
```bash
export ANDROID_HOME=/path/to/sdk
./gradlew assembleDebug
# APK: app/build/outputs/apk/debug/app-debug.apk
```

### Opzione 4 — GitHub Codespaces (online, senza nulla da installare)
1. Carica il progetto su un repo GitHub
2. Apri **Codespaces** dal repo
3. Installa SDK: `sdkmanager "platforms;android-33" "build-tools;33.0.2"`
4. `./gradlew assembleDebug`

## 🎯 Funzionalità

| Feature | Descrizione |
|---|---|
| ⏱️ Cronometro | Precisione al centisecondo |
| 🌊 Onde animate | 3 wave layers con fase e frequenza diversa |
| 🏊 Swimmer live | Nuotatore animato durante il timing |
| 📍 Parziali vasche | Verde=più veloce, Rosso=più lento |
| 💾 Salva gara | Nome personalizzato + parziali |
| 📋 Cronologia | Lista gare con tempi e conteggio vasche |
| 🔍 Dettaglio | Parziali + tempi cumulativi, rinomina |
| 📳 Vibrazione | Su ogni azione (start/stop/vasca/reset) |
| 🎨 3 Temi | Giorno, Notte, Viola — onde cambiano colore |

## Requisiti
- minSdk 29 (Android 10)
- targetSdk 33
- JDK 8+
