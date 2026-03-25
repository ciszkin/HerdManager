# HerdManager

A cross-platform application for managing local AI/LLM models. Built with Kotlin Multiplatform and Compose, targeting Android and Desktop (JVM) with a shared codebase.

HerdManager connects to your local Ollama server, allowing you to browse and pull models from the ollama.com registry, view and manage downloaded models, and monitor running models. Features include real-time model status, configurable polling intervals, and persistent settings. The app follows clean MVI architecture with platform-specific optimizations for both Android and Desktop experiences.

## Features

- **Local Models Management:** View, delete, and inspect downloaded models with detailed information including size, quantization level, family, and modification date
- **Model Registry:** Browse and pull models from ollama.com/library with search functionality, capabilities display, and pull counts for popularity reference
- **Running Models:** Monitor currently loaded models in real-time with status, memory usage, VRAM consumption, and auto-refresh polling
- **Configurable Settings:** Customize server URL connection, polling intervals (1-60 seconds), and enable/disable auto-refresh functionality
- **Cross-Platform:** Native Android and Desktop (JVM) applications with shared codebase, adaptive navigation (bottom bar for mobile, side rail for desktop), and platform-specific optimizations

## Quick Start

### Android

Build the debug APK:
```bash
./gradlew :composeApp:assembleDebug
```

Use the run configuration from your IDE's toolbar to run the app directly.

### Desktop (JVM)

Run the desktop application:
```bash
./gradlew :composeApp:run
```

Or use the run configuration from your IDE's toolbar.

## Project Structure

The codebase follows a multiplatform structure:

- `commonMain/kotlin` - Shared code used across all platforms (Android, Desktop)
- `androidMain/kotlin` - Android-specific implementations and configurations
- `jvmMain/kotlin` - Desktop/JVM-specific implementations and configurations

## Learn More

- [Kotlin Multiplatform Documentation](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html)
- [Ollama Documentation](https://github.com/ollama/ollama)
