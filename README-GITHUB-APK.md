# Lớp Học Thông Minh AI V5.3 — GitHub APK Builder

Dự án này đóng gói bản web trong thư mục `www/` thành ứng dụng Android bằng Capacitor.
GitHub Actions sẽ tự tạo dự án Android và build APK debug.

## Cách tạo APK bằng GitHub

1. Đăng nhập GitHub và tạo repository mới (ví dụ `lop-hoc-thong-minh-ai`).
2. Giải nén ZIP này trên máy tính.
3. Upload toàn bộ nội dung bên trong thư mục vào repository (phải thấy `package.json`, `capacitor.config.json`, `www/` và `.github/` ở thư mục gốc).
4. Mở tab **Actions**, chọn workflow **Build Android APK**.
5. Nhấn **Run workflow**. Chờ job hoàn tất màu xanh.
6. Mở lần chạy vừa xong, kéo xuống **Artifacts**, tải `Lop-Hoc-Thong-Minh-AI-V5.3-debug-apk.zip`.
7. Giải nén artifact, lấy `app-debug.apk`, chuyển sang Android và cài đặt.

## Build tự động
Mỗi lần push/commit lên nhánh `main` hoặc `master`, GitHub Actions sẽ tự build lại APK.

## Lưu ý
- Đây là **debug APK** để cài thử nội bộ, chưa phải APK phát hành ký số để đưa lên Google Play.
- Ứng dụng cần Internet vì Firebase, Hanzi Writer và một số thư viện/nguồn dữ liệu được tải từ CDN.
- Firebase config và dữ liệu trong HTML được giữ nguyên từ gói ZIP nguồn.
- Mã package: `vn.topkey.lophoc.thongminhai`.
- Nếu muốn phát hành chính thức, cần tạo keystore và cấu hình GitHub Secrets để ký release APK/AAB.
