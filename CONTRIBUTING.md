# Contributing to Gaz AI – Truth Detective

Thanks for taking an interest in Gaz AI. This is an experimental project, and practical feedback from Android developers, testers and curious users is welcome.

## Ways to help

- Reproduce bugs and report clear steps to trigger them.
- Test on different Android phones, screen sizes and Android versions.
- Improve accessibility, text contrast, layout and navigation.
- Improve Endless Investigation, evidence saving, connections and the connection map.
- Add or improve automated tests.
- Improve build reliability, performance, battery use and local-model handling.
- Improve documentation and explain confusing parts of the app.

## Before starting

1. Check the existing [issues](https://github.com/garethedmond73-art/gaz-ai/issues) and pull requests to avoid duplicating work.
2. For a substantial change, open an issue first and describe the problem or proposed improvement.
3. Keep changes focused. Smaller pull requests are easier to review.
4. Do not include API keys, passwords, signing keys, personal data or private investigation content in commits, screenshots or issue reports.

## Development setup

1. Fork the repository on GitHub.
2. Clone your fork:
   ```bash
   git clone https://github.com/YOUR-USERNAME/gaz-ai.git
   cd gaz-ai
   ```
3. Open the project in Android Studio and allow Gradle to sync.
4. Build:
   ```bash
   gradle assembleDebug
   ```
5. Run the available tests from Android Studio or with the project's Gradle test tasks.

## Pull request checklist

Please include:

- A short explanation of what changed and why.
- Steps to reproduce the original issue, where relevant.
- The Android version/device tested.
- Build and test results. Be precise: distinguish tests that passed from tests that were not run or were cancelled.
- Screenshots or a short recording for visible UI changes, with private information removed.

Keep the app's existing privacy expectations in mind. Any new network feature should explain what data leaves the device, which service receives it, and whether an API key or payment is required.

## Project principles

- Treat AI answers as suggestions or research leads, not verified facts.
- Keep evidence, claims, theories and possible connections distinguishable.
- Avoid presenting a possible connection as proof.
- Prefer clear, readable interfaces, especially on smaller phone screens.
- Be honest about limitations, incomplete tests and external service dependencies.

## Licence

By submitting a contribution, you agree that it will be made available under the repository's MIT Licence. Please make sure you have the right to contribute the code and that third-party code, assets and datasets are appropriately licensed and attributed.

Thanks for helping make the project better.
