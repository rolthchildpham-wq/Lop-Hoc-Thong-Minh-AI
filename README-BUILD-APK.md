# Lớp Học Thông Minh AI Android Hybrid v4.4

## Cách hoạt động
- Có mạng: WebView mở https://aihuedanlearn.edgeone.dev/ để nhận bản web mới nhất và dùng Firebase 1v1.
- Mất mạng / live URL lỗi: tự chuyển sang `assets/www/index.html` đóng gói trong APK.
- Offline core: CEFR A1-C1, HSK core, Toán/Tiếng Việt/Tiếng Anh lớp 5, Learning Hub, điểm/streak/localStorage.
- Cần mạng: Firebase 1v1, tải KorvaTTS lần đầu, Hanzi Writer/dữ liệu nét Hán mới.

## Build trong Android Studio
1. Mở thư mục này bằng Android Studio.
2. Chờ SDK/Gradle sync.
3. Build > Build APK(s).
4. APK debug: `app/build/outputs/apk/debug/app-debug.apk`.

## Build không cần Android Studio (GitHub Actions)
Đưa project lên GitHub, mở tab Actions > Build Android APK > Run workflow. Artifact APK sẽ xuất hiện sau build.

## Package
`vn.aihuedan.learn` — version 4.4 (versionCode 44), min Android 8 (API 26), target 35.
