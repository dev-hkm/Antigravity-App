# 🚀 Antigravity Multi-Account Desktop Client

Ứng dụng Desktop chuyên biệt cho Google Antigravity Web, tối ưu hoá hiệu năng vượt trội so với PWA, hỗ trợ quản lý đa tài khoản độc lập hoàn toàn.

---

## ✨ Điểm Nổi Bật

1. **Đa Tài Khoản Độc Lập (Isolated Partitions)**:
   - Mỗi tài khoản chạy trong một session riêng biệt (`persist:account_<id>`).
   - Cookie, Cache, OAuth Token của từng tài khoản được lưu riêng biệt trên ổ đĩa.
   - **Chỉ cần đăng nhập 1 lần duy nhất cho mỗi tài khoản** — không bao giờ bị đá phiên hay phải xác thực lại mỗi lần chuyển đổi.

2. **Chuyển Đổi Tức Thì (Zero-Delay Instant Switch)**:
   - Giữ các session view sẵn sàng trong bộ nhớ (`WebContentsView`).
   - Bấm chuyển tài khoản là hiển thị ngay lập tức (không phải reload lại trang từ đầu).

3. **Tự Động Ghi Nhớ Tài Khoản Cuối**:
   - Khi tắt app và mở lại, ứng dụng luôn tự động mở đúng tài khoản và vị trí bạn đang làm việc dở dang.

4. **Giao Diện Đậm Chất Antigravity**:
   - Sidebar quản lý tài khoản phong cách Discord/Slack với màu sắc nhận diện riêng.
   - Custom Titlebar mượt mà, tối giản.
   - Thanh điều hướng (Back, Forward, Reload, Home, Zen Mode để ẩn thanh công cụ khi cần 100% diện tích làm việc).

5. **Phím Tắt Siêu Tốc**:
   - `Ctrl + 1` đến `Ctrl + 9`: Chuyển tức thì sang tài khoản 1..9.
   - `Ctrl + Tab`: Chuyển sang tài khoản tiếp theo.
   - `Ctrl + Shift + Tab`: Chuyển về tài khoản trước đó.
   - `Ctrl + R` hoặc `F5`: Tải lại trang web.
   - `Ctrl + Shift + I`: Bật/tắt DevTools để debug khi cần.

---

## 💻 Cách Khởi Chạy

### Cách 1: Click đúp chạy nhanh
Nhấp đúp chuột vào file **`Run-Antigravity.bat`** tại thư mục này.

### Cách 2: Bằng Terminal
```bash
npm start
```

---

## 🛠️ Hướng Dẫn Sử Dụng

1. **Thêm tài khoản mới**:
   - Bấm nút **`+`** ở thanh sidebar bên trái.
   - Nhập tên gợi nhớ (VD: `Personal`, `Company`, `Project X`), chọn màu đại diện và URL (mặc định là `https://antigravity.google`).
   - Bấm **Lưu tài khoản**.
   - Tiến hành đăng nhập Google/Antigravity lần đầu tiên trên tài khoản mới này.

2. **Chỉnh sửa / Xóa tài khoản**:
   - Nhấp chuột phải (Right-click) vào biểu tượng tài khoản trên sidebar, hoặc bấm nút cây bút ✏️ trên thanh công cụ.
   - Bạn có thể đổi tên, đổi màu hoặc bấm **Xóa tài khoản này** (dữ liệu phiên của tài khoản đó sẽ được dọn dẹp an toàn).

3. **Chế độ Zen (Tối đa không gian làm việc)**:
   - Bấm nút mũi tên thu gọn trên thanh điều hướng để ẩn navbar.
   - Khi cần mở lại, bấm nút nhỏ ở góc trên bên phải màn hình.

## 🔐 Đăng nhập Google trên Android

Google không hỗ trợ đăng nhập trong WebView nhúng. Khi Antigravity chuyển sang
Google OAuth, Android app sẽ dừng navigation trong WebView và yêu cầu mở Chrome
Custom Tab hoặc trình duyệt hệ thống. Cookie của trình duyệt ngoài và WebView là
hai vùng lưu trữ độc lập, nên app không giả lập User-Agent hoặc sao chép cookie.
