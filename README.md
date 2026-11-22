# Gaz AI – Truth Detective

**An experimental Android AI assistant and investigation workspace.** Ask questions, follow leads, collect evidence and explore possible connections.

[Source code](https://github.com/garethedmond73-art/gaz-ai) · [User guide](USER_GUIDE.md) · [Beta testing](BETA_TESTING.md) · [Contributing](CONTRIBUTING.md) · [GitHub Actions builds](https://github.com/garethedmond73-art/gaz-ai/actions)

> **Project status:** Experimental community project. The Android debug APK can be built by GitHub Actions, but not every recent build has completed its emulator tests. Read the beta-testing notes before installing. This is not an app-store release.

## What is Gaz AI?

Gaz AI brings together an on-device AI assistant and the Truth Detective idea: a place to explore a subject, keep a record of information, and examine how different pieces might relate.

The aim is to make investigation feel like an ongoing process rather than a one-off chat.

## Main features

- **Endless Investigation:** follow a chain of questions and suggested next steps.
- **Evidence Ledger:** save useful answers and information for later review.
- **Connections:** link two evidence records and explain the possible relationship.
- **Connection Map:** view the links between saved records.
- **Local investigation trail:** return to saved investigation work on the device.
- **On-device AI:** local Qwen model download and inference, without requiring an OpenAI API key or paid AI credits.
- **Optional online research:** Wikipedia lookup can provide additional reference material when an internet connection is available.
- **Optional free AI second opinion:** an external free-model route may be available; limits, availability and provider privacy practices can change.

See the [User Guide](USER_GUIDE.md) for more detail.

## How to try the Android app

1. Open the [GitHub Actions page](https://github.com/garethedmond73-art/gaz-ai/actions).
2. Choose a recent **Truth Detective Android CI** run.
3. Check its result before using its APK:
   - **Success:** the workflow completed its configured build and emulator smoke-test steps.
   - **Cancelled or failed:** do not treat it as a fully tested build, even if an APK artifact is present.
4. If an artifact is available, open **Artifacts** at the bottom of the run and download `truth-detective-build-and-test`.
5. Unzip it and locate `app-debug.apk`. Transfer it to an Android device and install it only if you are comfortable testing experimental software.

The artifact is a development/debug build, not a signed Play Store release. GitHub Actions artifacts are temporary and expire according to the workflow's retention setting.

## Build from source

1. Install Android Studio and a compatible Android SDK.
2. Clone this repository:
   ```bash
   git clone https://github.com/garethedmond73-art/gaz-ai.git
   cd gaz-ai
   ```
3. Open the project in Android Studio and allow Gradle to sync.
4. Build the debug APK:
   ```bash
   gradle assembleDebug
   ```

The GitHub Actions workflow also builds the debug APK and runs its configured Android emulator checks.

## Privacy and external services

- The local model is designed to run on the device.
- Chat history and investigation notes are stored locally by default.
- Optional Wikipedia research sends the current question to Wikipedia's public API.
- The optional free AI second-opinion feature sends the question entered for that feature to an external AI gateway. External providers may process or retain prompts under their own terms. Do not submit personal, sensitive or confidential information.
- Check the app's current settings and the provider's own privacy terms before using optional online features.

## Important: investigate, don't assume

Gaz AI is an aid for exploration, not an authority or a substitute for checking facts. An AI-generated answer is a lead, not proof. Check important claims against independent sources, distinguish facts from theories, and consider alternative explanations. A connection between two records is a possible relationship to examine, not proof that they are causally related.

## Help improve the project

This repository is public so developers and curious testers can inspect the idea, try the code and help improve it. Bug reports, usability feedback, documentation improvements and pull requests are welcome.

- Read [CONTRIBUTING.md](CONTRIBUTING.md) before making changes.
- Use [BETA_TESTING.md](BETA_TESTING.md) to report useful test results.
- Open an [issue](https://github.com/garethedmond73-art/gaz-ai/issues) for bugs or suggestions.

There is no promise that every suggestion or pull request will be accepted or merged.

## Licence

This project is licensed under the [MIT Licence](LICENSE). Please retain the copyright and licence notice when redistributing substantial portions of the project.
