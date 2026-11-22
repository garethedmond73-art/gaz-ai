# Gaz AI – Truth Detective: Beta testing notes

Thanks for trying the app. This is an experimental Android project shared for GitHub developers and testers. It is not an app-store release and should not be relied on for important decisions or as a source of verified facts.

## Where to get a test build

1. Go to [GitHub Actions](https://github.com/garethedmond73-art/gaz-ai/actions).
2. Open a recent **Truth Detective Android CI** workflow run.
3. Read the run's conclusion before downloading:
   - **Success** means the configured workflow completed successfully.
   - **Failure** or **cancelled** means the build or test process did not complete cleanly.
4. If the run has an artifact, download `truth-detective-build-and-test` from its **Artifacts** section.
5. Unzip the download and find `app-debug.apk`.

An APK artifact existing does not mean its emulator tests passed. The workflow's artifacts are temporary and expire after the configured retention period. These are debug builds, not production-signed releases.

## What to test

- Does the app open and remain stable?
- Can you enter text and read it clearly on your device?
- Does Endless Investigation produce follow-up questions?
- Does the investigation trail remain available when you return?
- Can you save an answer to the Evidence Ledger?
- Does it avoid saving the same answer repeatedly?
- Can you create a connection between two different evidence records with a reason?
- Does the Connection Map display the saved links clearly?
- Are there crashes, freezes, slow screens or layout problems?

Please remember that optional online AI features depend on external services and may have changing limits or availability.

## How to report a bug

Open a [GitHub issue](https://github.com/garethedmond73-art/gaz-ai/issues) and include:

- A short, descriptive title.
- Device model and Android version.
- The build/workflow run you tested, if known.
- Exact steps to reproduce the problem.
- What you expected and what happened instead.
- A screenshot or recording if it helps (remove names, messages and other private information).

For a crash, include relevant logcat output if you know how to collect it. Remove tokens, keys, email addresses and personal data before posting logs publicly.

## Feedback and ideas

Usability suggestions and constructive criticism are just as useful as bug reports. Explain what you were trying to do and what would have made it easier.

## Safety and privacy

- Do not use the app as a substitute for professional advice or independent fact-checking.
- Do not enter confidential or sensitive information into optional online AI features.
- Local investigation data is intended to remain on the device, but back up anything important before uninstalling or changing devices.
- Only install APKs you trust and understand that experimental software can contain bugs.
