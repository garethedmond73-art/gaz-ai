# Gaz AI

Gaz AI is an Android assistant with a local language model and an expandable offline knowledge library.

## Current features
- On-device Qwen model download and local inference; no OpenAI API key or paid AI credits.
- Built-in starter knowledge pack with original reference notes across Scripture, science, philosophy, symbolism, history and research skills.
- Keyword retrieval adds relevant starter notes and source links to the local model's prompt.
- Browse the starter collection in the Library tab.
- Chat history and investigation notes are stored locally on the device.

## Knowledge library and licensing
The initial pack contains original short explanations written for this project, not complete copies of copyrighted books. Each note includes a suggested source link. Linked websites and texts have their own terms; a link in the app does not mean their full contents are bundled or licensed for redistribution.

The library is designed so additional packs and user-imported material can be added later. Before bundling any full text, verify the exact edition, translation, dataset licence, attribution requirements and whether redistribution/offline use is allowed. Where rights are unclear, keep a source link rather than copying the text.

## Current limitations
- The starter retrieval is lightweight keyword matching, not yet semantic/vector search.
- The initial library is a starter reference collection, not a complete encyclopaedia or a bundle of full scripture editions.
- The Library screen currently browses built-in notes. Importing PDF/EPUB/TXT files and downloading additional packs is a later milestone.
- Internet research is not yet implemented; the ordinary chat and starter library work offline once the model is downloaded.

## Build
Open in Android Studio and allow Gradle to sync. GitHub Actions builds a debug APK on pushes to `main`.

## Privacy
Conversation and investigation data stay local by default. The local model does not require a cloud AI service or API key.
