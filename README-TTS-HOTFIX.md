# V5.3 — Hotfix kết nối phát âm Android CN/EN

## Lỗi được sửa trong bản này

APK `app-debug.apk` người dùng cung cấp có chứa Sherpa-ONNX, `AndroidVoice`, `NeuralTtsManager` và các mô hình offline tại `assets/tts/en/` và `assets/tts/zh/`. Tuy nhiên, bản web V5.3 trước đó chỉ gọi `speechSynthesis` của WebView. Hook native trong APK được cài khi `window.AndroidVoice` đã tồn tại; nếu bridge được đăng ký muộn hơn thời điểm script chạy, hook không được cài và lệnh đọc rơi về Web Speech.

Hotfix thay đổi `MultiVoiceManager.systemSpeakAsync()` để kiểm tra và gọi trực tiếp `window.AndroidVoice.speak(text, locale)` tại đúng thời điểm người dùng yêu cầu đọc. Nếu bridge không tồn tại hoặc native call lỗi, ứng dụng vẫn fallback sang Web Speech. `cancelAll()` cũng gọi `AndroidVoice.cancel()`.

## Quan trọng — giới hạn của ZIP này

ZIP gốc do người dùng gửi **không có thư mục Android native** và workflow cũ chạy `npx cap add android` để tạo một Android project mới. Do đó, build APK chỉ từ ZIP này sẽ không tự có `AndroidVoice`/Sherpa-ONNX hoặc các model 500 MB. Hotfix này sửa đường gọi JavaScript cho APK có sẵn native bridge; nó **không tự tái tạo** mã native và model đã có trong APK cũ.

Để tạo APK offline CN/EN độc lập từ GitHub, cần đưa project Android native gốc (MainActivity, NeuralTtsManager, NativeVoiceBridge, Gradle dependencies và assets/tts) vào repository. APK debug hiện tại có thể được dùng để trích xuất assets, nhưng không thể khôi phục an toàn toàn bộ mã nguồn Java/Kotlin chỉ từ ZIP web builder.

## Thay đổi

- `www/index.html`: ưu tiên gọi native `AndroidVoice` trực tiếp; Web Speech là phương án dự phòng.
- Không thay đổi Firebase, nội dung học, giao diện hoặc dữ liệu bài học.
