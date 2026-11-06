# V5.0 Offline Neural Voice Packs

This Android build bundles offline neural text-to-speech models at build time.

## English
- Model: vits-piper-en_US-lessac-medium
- Source: sherpa-onnx TTS model releases
- Upstream voice: Piper en_US lessac medium
- Runtime: sherpa-onnx 2.0.1
- Packaged under: app/src/main/assets/tts/en/

## Chinese
- Model: kokoro-multi-lang-v1_0
- Used for Mandarin Chinese voice
- Source: sherpa-onnx TTS model releases
- Runtime: sherpa-onnx 2.0.1
- Packaged under: app/src/main/assets/tts/zh/

The build workflow downloads the official model archives and keeps the included
LICENSE/README files in the packaged assets.

These voices run locally on-device. English and Chinese speech do not require
Android system TTS data or an Internet connection after installation.
