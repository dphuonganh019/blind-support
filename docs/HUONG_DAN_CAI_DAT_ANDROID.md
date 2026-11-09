# Hướng dẫn cài đặt Trợ Lý Nghe Gọi cho máy Android

Áp dụng cho mọi điện thoại Android 10 trở lên. Bản app mới nhất luôn có ở [trang Releases](https://github.com/dphuonganh019/blind-support/releases).

**Trợ Lý Nghe Gọi** giúp người khiếm thị nghe máy, gọi đi bằng giọng nói và nghe tin nhắn (sim, Zalo, WhatsApp, SMS) bằng tiếng Việt. Tài liệu này dành cho **người hỗ trợ** – người sáng mắt cài app giúp **người dùng** – trên bất kỳ máy Android nào. Cách dùng hằng ngày, câu lệnh và xử lý sự cố có trong [hướng dẫn chi tiết có hình](HUONG_DAN_SU_DUNG.md).

## Yêu cầu máy

Kiểm tra máy trước khi cài. Phần lớn thông tin nằm ở **Cài đặt → Giới thiệu về điện thoại** (Samsung: **Thông tin điện thoại → Thông tin phần mềm**).

| Mục | Yêu cầu | Mức | Ghi chú |
| --- | --- | --- | --- |
| Phiên bản Android | **Android 10 trở lên** | Bắt buộc | Android 9 trở xuống không cài được |
| Loại máy | Điện thoại có **SIM nghe gọi** | Bắt buộc | Máy tính bảng không có SIM không khuyên dùng |
| Dịch vụ Google | Có **CH Play**, app **Google** và **Dịch vụ giọng nói của Google** | Bắt buộc | Máy Huawei / Honor đời mới không có dịch vụ Google: nghe lệnh kém, có thể thiếu giọng đọc tiếng Việt |
| Bản Android thường | Không phải **Android Go** | Bắt buộc | Máy Go thường không cho cấp quyền đọc thông báo, nên không đọc được tin Zalo. Dấu hiệu: máy ghi "Android (Go edition)", hoặc có sẵn app tên "Google Go", "Gmail Go" |
| RAM | **Từ 3 GB** | Khuyên dùng | Máy RAM thấp hay tự tắt app chạy ngầm, dễ lỡ cuộc gọi. App đã thử ổn trên máy 8 GB |
| Bộ nhớ trống | **Từ 1 GB** | Khuyên dùng | File cài khoảng 62 MB; app còn tải thêm bộ nhận dạng dự phòng, giọng đọc tiếng Việt và cập nhật app Google |
| Mạng | Wifi lúc cài; wifi hoặc 4G khi dùng | Bắt buộc lúc cài | Không có mạng vẫn báo và nhấc được cuộc gọi đến, nhưng gọi đi theo tên nghe kém hơn nhiều |
| Phần cứng | Micro, loa ngoài, **phím giảm âm lượng** còn tốt | Bắt buộc | Phím giảm âm lượng là cách người dùng mở lệnh giọng nói |
| Zalo / WhatsApp | Đã cài và đăng nhập | Tùy chọn | Chỉ cần nếu người dùng có dùng hai app này |

Máy đã thử: **OPPO A92** (Android 11, ColorOS 11.1) và máy **Samsung** đời mới. Máy hãng khác (Xiaomi, vivo, realme…) đủ điều kiện trên vẫn cài được, nhưng chưa được thử trực tiếp – hãy làm kỹ Bước 7.

Mức RAM và bộ nhớ "khuyên dùng" là mức an toàn ước tính, không phải giới hạn cứng của app.

## Bước 1: Tắt Play Protect

Play Protect của Google thường **chặn app cài ngoài CH Play** nếu app xin quyền Trợ năng và đọc thông báo. Bước này giống nhau trên mọi máy:

1. Mở **CH Play** → chạm **ảnh đại diện** (góc trên bên phải) → **Play Protect**.
2. Chạm **bánh răng ⚙** → tắt **"Quét ứng dụng bằng Play Protect"** → **Tắt**.

Nên để tắt luôn nếu máy chỉ để người khiếm thị dùng, vì Play Protect có thể tự thu hồi quyền của app sau này. Khi cài vẫn báo **"Đã chặn bởi Play Protect"** thì chạm **Thông tin chi tiết → Vẫn cài đặt**.

## Bước 2: Tải file cài đặt

Cách 1 – từ GitHub: trên máy người dùng, mở **Chrome** → vào [trang Releases của app](https://github.com/dphuonganh019/blind-support/releases) → ở bản trên cùng (số lớn nhất), chạm file **TroLyNgheGoi-v1.0.xx.apk**. File vào thư mục **Download**.

Cách 2 – qua Zalo: nhận file .apk trong cuộc trò chuyện, chạm để tải. File vào thư mục **Download/Zalo** (Zalo bản cũ có thể để ở **Zalo** hoặc **Android/data**).

## Bước 3: Cài đặt

1. Mở app quản lý file của máy (tên thường là **Quản lý tệp**, **Tệp của bạn**, **Files**) → vào thư mục ở Bước 2 → chạm file .apk. Cách nhanh: gõ **TroLyNgheGoi** vào ô tìm kiếm của app đó.
2. Máy báo không cho cài ứng dụng không rõ nguồn → chạm **Cài đặt** → bật **"Cho phép từ nguồn này"** → quay lại → chạm **Cài đặt**.
3. Máy có thể quét bảo mật, hỏi mật khẩu tài khoản hãng (OPPO, Xiaomi, vivo…) hoặc cảnh báo rủi ro → nhập mật khẩu / chạm **Vẫn cài đặt**.
4. Riêng **Samsung**: nếu máy cấm cài, vào Cài đặt → Bảo mật và quyền riêng tư → **Chặn tự động** → Tắt.

Cài xong, màn hình chính có **3 biểu tượng**: **Trợ Lý Nghe Gọi** (người dùng mở lệnh giọng nói), **Cài đặt Trợ Lý** (người hỗ trợ cài đặt), **Điện thoại** nền xanh dương (bàn phím gọi cho người sáng mắt).

## Bước 4: Cấp quyền trong Cài đặt Trợ Lý

Mở **Cài đặt Trợ Lý**. Mục **"1. Quyền và trạng thái"** là danh sách các việc cần làm: dòng nào **❌** thì chạm **Sửa** bên cạnh, máy tự mở đúng chỗ, bật xong bấm quay lại. Làm tới khi **mọi dòng đều ✅**. Cách này giống nhau trên mọi hãng.

1. Chạm **"Cấp tất cả quyền cơ bản"** → luôn chọn **Cho phép** (micro: **"Trong khi dùng ứng dụng"**).
2. **Ứng dụng điện thoại mặc định** → chọn **Trợ Lý Nghe Gọi**. Bắt buộc – thiếu thì app không báo được cuộc gọi sim. Sau này **không đổi lại** về app Điện thoại gốc; người sáng mắt cần gọi thì dùng biểu tượng **Điện thoại** của Trợ Lý.
3. **Quyền đọc thông báo** → bật.
4. **Trợ năng** (có máy ghi **Hỗ trợ tiếp cận**) → chạm **Trợ Lý Nghe Gọi** (thường trong mục *Ứng dụng đã tải xuống* / *Ứng dụng đã cài đặt*) → bật → **Cho phép**.
5. **Hiển thị trên ứng dụng khác** → bật. **Không giới hạn pin** → Cho phép. **Không tự thu hồi quyền** → tắt "Xóa quyền nếu không dùng ứng dụng".
6. **Giọng đọc tiếng Việt** → tải giọng Tiếng Việt (cần wifi). **Nhận dạng giọng nói Google** → cài hoặc cập nhật app **Google**. **Bộ nhận dạng offline** → chờ vài chục giây.

### Máy Android 13 trở lên: "Cài đặt bị hạn chế"

Với app cài từ file .apk, Android 13+ khóa sẵn hai quyền **Trợ năng** và **đọc thông báo**. Khi bật, máy hiện **"Cài đặt bị hạn chế"** và công tắc bị xám. Cách mở:

1. Thử bật quyền đó một lần (để máy hiện thông báo hạn chế) → đóng lại.
2. Vào **Cài đặt → Ứng dụng → Trợ Lý Nghe Gọi** → chạm **⋮** (góc trên bên phải) → **Cho phép cài đặt hạn chế** → nhập mã mở khóa máy.
3. Quay lại Cài đặt Trợ Lý, chạm **Sửa** và bật lại hai quyền.

Kiểm tra phiên bản Android ở **Cài đặt → Giới thiệu về điện thoại**. Android 10–12 không có bước này.

## Bước 5: Cho app chạy ngầm

Nhiều hãng tự tắt app chạy ngầm để tiết kiệm pin, làm lỡ cuộc gọi. Làm bước này **cho cả 3 app: Trợ Lý Nghe Gọi, Zalo, WhatsApp**. Tên mục khác nhau tùy hãng và đời máy; không thấy thì gõ "pin", "tự khởi chạy", "chạy nền" vào ô tìm kiếm của Cài đặt.

**Mọi máy:**

- **Khóa thẻ app**: mở app một lần → mở danh sách app gần đây → giữ hoặc chạm **⋮** trên thẻ app → **Khóa** / **Giữ để mở** (nếu máy có).
- Tắt **chế độ tiết kiệm pin** của máy.
- Không cài app dọn dẹp / tăng tốc; không bấm **"Buộc dừng"** app.
- Zalo / WhatsApp phải **bật thông báo** (Cài đặt → Thông báo → Zalo / WhatsApp). Trong Zalo: Cá nhân → ⚙ → Thông báo → bật tin nhắn mới và cuộc gọi đến.

**Theo hãng:**

| Hãng | Cần làm (cho từng app) |
| --- | --- |
| **OPPO, realme, OnePlus** (ColorOS) | Cài đặt → Quản lý ứng dụng → chọn app → **Mức sử dụng pin** → bật *Cho phép hoạt động nền*, *Cho phép tự khởi chạy*, *Cho phép ứng dụng khác khởi chạy*. Cài đặt → Pin → Cài đặt khác → tắt *Tối ưu hóa chờ khi ngủ*. Xem thêm [hướng dẫn chi tiết cho OPPO](HUONG_DAN_SU_DUNG.md). |
| **Samsung** (One UI) | Cài đặt → Pin → **Giới hạn sử dụng nền** → **Ứng dụng không bao giờ ngủ** → thêm 3 app. Cài đặt → Ứng dụng → chọn app → Pin → **Không hạn chế**. Tuỳ chọn: Cài đặt → Tính năng nâng cao → Phím sườn → Nhấn và giữ → Trợ lý kỹ thuật số → Trợ Lý Nghe Gọi (giữ nút nguồn để mở lệnh). |
| **Xiaomi, Redmi, POCO** (MIUI / HyperOS) | Cài đặt → Ứng dụng → Quản lý ứng dụng → chọn app → bật **Tự khởi chạy** → **Tiết kiệm pin** → **Không hạn chế**. Trong **Quyền khác**: bật *Hiển thị trên màn hình khóa*, *Mở cửa sổ mới khi chạy nền*. |
| **vivo, iQOO** | Cài đặt → Pin → **Quản lý mức tiêu thụ pin nền** (hoặc *Mức sử dụng pin nền cao*) → cho phép 3 app. Cài đặt → Ứng dụng → **Tự khởi động** → bật 3 app. |
| **Hãng khác** (Nokia, Motorola, Pixel, TCL…) | Cài đặt → Ứng dụng → chọn app → Pin → **Không hạn chế** (hoặc *Không tối ưu hóa*). Các máy này thường ít tự tắt app hơn. |

Đường dẫn cho Xiaomi, vivo và hãng khác viết theo cài đặt phổ biến, chưa thử trên máy thật; tên mục có thể lệch.

## Bước 6: Danh bạ và Zalo

- Lưu tên **tiếng Việt có dấu, ngắn**, đúng cách người dùng quen gọi ("Chị Trang", "Anh Minh"). Tên tiếng Anh, viết tắt thì app không gọi được – Cài đặt Trợ Lý sẽ liệt kê các tên cần sửa.
- Muốn gọi Zalo / WhatsApp bằng giọng nói: người đó phải có trong danh bạ máy với **đúng số đăng ký Zalo / WhatsApp**.
- Nên **tắt tự cập nhật Zalo và WhatsApp** trong CH Play (⋮ → bỏ "Bật tự động cập nhật") để giao diện không đổi đột ngột.
- Để máy ở chế độ **đổ chuông**, không để im lặng.

## Bước 7: Thử trước khi trao máy

Nhờ người khác gọi vào và để **người dùng tự nói**. Với máy hãng chưa được thử, bước này đặc biệt quan trọng.

- [ ] Cài đặt Trợ Lý: **mọi dòng ✅**; "Thử giọng đọc" nghe tiếng Việt rõ.
- [ ] Gọi **sim** vào lúc **khóa màn hình** → nghe đọc tên → sau tiếng bíp nói "nghe" → nhấc máy, loa ngoài bật.
- [ ] Gọi **Zalo** vào lúc khóa màn hình → nói "nghe" → nhấc máy, loa ngoài bật. Làm lại với **WhatsApp**.
- [ ] Giữ phím giảm âm lượng → nói "gọi \[tên\]" → "đúng" → gọi được.
- [ ] Nhắn Zalo cho người dùng → app đọc to.
- [ ] **Khởi động lại máy**, không mở app → gọi sim và Zalo vào vẫn được báo.
- [ ] Để máy nằm yên, tắt màn hình **khoảng 1 tiếng** rồi gọi Zalo vào → vẫn được báo (kiểm tra máy không tự tắt app khi ngủ).

Mục nào không được: xem bảng sự cố trong [hướng dẫn chi tiết](HUONG_DAN_SU_DUNG.md), hoặc gửi mô tả (máy gì, Android mấy, chuyện gì xảy ra) vào mục [Issues trên GitHub](https://github.com/dphuonganh019/blind-support/issues).

Tiếp theo: đọc cho người dùng nghe phần **Cách sử dụng hằng ngày** trong hướng dẫn chi tiết (phím, vùng chạm, câu lệnh) và cùng tập vài lần.
