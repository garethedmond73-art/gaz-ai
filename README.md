# Gaz AI

Gaz AI is an Android assistant with two planned modes:

- **Chat** — a general-purpose assistant.
- **Truth Detective** — organise claims, evidence for and against, sources, and connections.

## Design goals

- Run the language model locally on the phone after its first download.
- No OpenAI API key, paid API credits, or subscription.
- Keep conversations and investigation notes on the device.
- Make internet search optional; ordinary local use must not depend on it.
- Keep the AI engine separate from the Truth Detective interface so it can potentially be reused in the existing Truth Detective project later.

## Current status

This is the initial Android app scaffold. The interface and local storage are being built first. **The on-device language model is not integrated yet**, so this is not yet a working AI assistant.

The next milestone is integrating an Android native inference runtime (such as llama.cpp), then adding a first-run model download with a clear model-size and storage warning.

## Device and model

The exact phone RAM and available storage should be checked before choosing a model. A compact quantised model is likely to be the sensible starting point; response quality and speed will be lower than a large cloud model.

## Build

Open this project in Android Studio and allow Gradle to sync. The GitHub Actions workflow builds a debug APK on pushes to `main`.

## Privacy

Conversation and investigation data should remain local by default. No cloud AI service or API key is configured in this project.
