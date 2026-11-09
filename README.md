# Trợ Lý Nghe Gọi

**Ứng dụng Android miễn phí giúp người khiếm thị tự nghe gọi Zalo, WhatsApp, điện thoại sim và nghe tin nhắn bằng giọng nói tiếng Việt, không cần nhìn màn hình.**

Ngày nay phần lớn người thân gọi nhau qua Zalo hay WhatsApp thay vì gọi sim. Với người khiếm thị, màn hình cảm ứng gần như không dùng được: không biết ai đang gọi, không tìm thấy nút "Trả lời", không bật được loa ngoài. Trợ Lý Nghe Gọi làm thay những việc đó. App **đọc to tên người gọi**, người dùng chỉ cần **nói "nghe"**, app tự nhấc máy và **bật loa ngoài**. Muốn gọi ai thì **giữ phím giảm âm lượng** và nói *"gọi Zalo chị Lan"*.

> **Tài liệu này dành cho ai?**
> Dành cho **người hỗ trợ**: người nhà, bạn bè, tình nguyện viên, hoặc người muốn giới thiệu và cài app cho một người khiếm thị. Người dùng không cần đọc tài liệu này. Bạn sẽ cài đặt máy giúp họ (khoảng 20–30 phút), rồi hướng dẫn họ vài thao tác chính ở [mục 5](#5-hướng-dẫn-người-dùng).

---

## Mục lục

1. [App làm được gì](#1-app-làm-được-gì)
2. [App có phù hợp không?](#2-app-có-phù-hợp-không)
3. [Quyền riêng tư và an toàn](#3-quyền-riêng-tư-và-an-toàn)
4. [Tải và cài đặt](#4-tải-và-cài-đặt)
5. [Hướng dẫn người dùng](#5-hướng-dẫn-người-dùng)
6. [Chi tiết: cuộc gọi đến](#6-chi-tiết-cuộc-gọi-đến)
7. [Chi tiết: lệnh giọng nói](#7-chi-tiết-lệnh-giọng-nói)
8. [Chi tiết: tin nhắn và thông báo](#8-chi-tiết-tin-nhắn-và-thông-báo)
9. [Kiểm tra trước khi trao máy](#9-kiểm-tra-trước-khi-trao-máy)
10. [Độ to](#10-độ-to)
11. [Người khác dùng máy để gọi](#11-người-khác-dùng-máy-để-gọi)
12. [Khi có sự cố](#12-khi-có-sự-cố)
13. [Tạm tắt, bật lại, gỡ ứng dụng](#13-tạm-tắt-bật-lại-gỡ-ứng-dụng)
14. [Giới hạn đã biết](#14-giới-hạn-đã-biết)
15. [Dành cho người phát triển](#15-dành-cho-người-phát-triển)

---

## 1. App làm được gì

**Khi có người gọi đến** (sim, Zalo, WhatsApp)
- Chuông reo to, máy rung, rồi app đọc: *"Có điện thoại Zalo! Chị Lan gọi. Sau tiếng bíp, nói: nghe."*
- Người dùng nói **"nghe"**, app nhấc máy và bật loa ngoài. Với cuộc gọi sim, bấm **phím bất kỳ ở cạnh máy** cũng nhấc máy được.
- Không trả lời thì app hỏi lại cho đến khi người gọi cúp máy. Tiếng ồn hay im lặng **không bao giờ** làm app từ chối cuộc gọi.
- Lỡ cuộc gọi thì app báo *"Bạn vừa nhỡ cuộc gọi từ …"*, nói "gọi lại" là gọi lại ngay.

**Khi muốn gọi đi**
- Giữ phím **giảm âm lượng** khoảng 1,5 giây rồi nói: *"gọi chị Lan"*, *"gọi Zalo anh Minh"*, *"gọi số 0912…"*.
- App hỏi lại cho chắc trước khi gọi.

**Tin nhắn và thông tin**
- Đọc to tin nhắn Zalo, WhatsApp, SMS (người gửi và nội dung).
- Nghe giờ, ngày, mức pin; báo pin yếu, cắm sạc, sạc đầy.
- Tự cảnh báo bằng giọng nói nếu điện thoại tắt mất một quyền quan trọng của app.

**Thiết kế cho người không nhìn được màn hình**
- Mọi thứ đều đọc to bằng tiếng Việt, không cần chạm đúng chỗ nào.
- Máy để im lặng vẫn đọc (người khiếm thị không biết máy đang im lặng).
- Không cần mở app. App tự chạy nền, kể cả sau khi khởi động lại máy.

---

## 2. App có phù hợp không?

**Cần có:**
- Điện thoại **Android 10 trở lên**. iPhone không dùng được.
- Người dùng **nghe và nói được tiếng Việt**. Người nghe kém vẫn dùng được: app có 4 mức độ to và chạy được với loa hoặc tai nghe Bluetooth.
- **Một người sáng mắt cài đặt máy** lần đầu (khoảng 20–30 phút) và có thể giúp khi có sự cố.
- **Wifi hoặc 4G** để gọi theo tên và theo số bằng giọng nói (xem [mục 7](#7-chi-tiết-lệnh-giọng-nói)). Nghe cuộc gọi đến thì không cần mạng riêng cho app.
- Zalo/WhatsApp đã đăng nhập sẵn trên máy, nếu muốn dùng Zalo/WhatsApp.

**Máy đã thử:**

| Máy | Tình trạng |
|---|---|
| OPPO A92 (ColorOS 11.1, Android 11) | Đã dùng ổn định hằng ngày (bản 1.0.31) |
| Samsung (One UI) | Đã thử |
| Xiaomi, Vivo, Realme, các hãng khác | **Chưa thử.** Vẫn cài được, nhưng các hãng này hay tắt app chạy nền, nên hãy làm kỹ [mục 9](#9-kiểm-tra-trước-khi-trao-máy) và tìm mục cài đặt pin/tự khởi chạy tương tự như hướng dẫn cho OPPO |

**Lưu ý quan trọng:** App giúp việc nghe gọi hằng ngày dễ hơn, nhưng **không phải thiết bị khẩn cấp**. Zalo/WhatsApp đổi giao diện sau khi cập nhật có thể làm một vài tính năng tạm thời không chạy (xem [mục 14](#14-giới-hạn-đã-biết)). Người nhà nên thỉnh thoảng gọi thử, và vẫn giữ cách liên lạc dự phòng.

---

## 3. Quyền riêng tư và an toàn

- **App không xin quyền truy cập Internet.** Danh bạ, tin nhắn, nhật ký cuộc gọi chỉ được đọc trên máy để đọc to cho người dùng, không gửi đi đâu.
- Lệnh giọng nói dùng **bộ nhận dạng giọng nói của Google** có sẵn trên máy. Giống như khi dùng Trợ lý Google, câu lệnh có thể được Google xử lý qua mạng. Khi mất mạng, app dùng bộ nhận dạng offline chạy hoàn toàn trên máy.
- Các nút "Gửi nhật ký…" trong Cài đặt Trợ Lý **chỉ chạy khi người hỗ trợ tự bấm**, và người hỗ trợ tự chọn gửi qua Zalo, email… để báo lỗi.
- App **miễn phí, không quảng cáo, mã nguồn mở**: ai cũng xem được mã trong repo này.

**Vì sao app cần nhiều quyền như vậy?**

| Quyền | Để làm gì |
|---|---|
| Ứng dụng điện thoại mặc định | Báo cuộc gọi sim, nhấc máy bằng giọng nói hoặc phím cạnh máy |
| Đọc thông báo | Đọc tin nhắn, nhận biết cuộc gọi Zalo/WhatsApp đến |
| Trợ năng (Hỗ trợ tiếp cận) | Giữ phím giảm âm lượng để ra lệnh; chạm nút "Trả lời" và "Loa" của Zalo/WhatsApp thay người dùng |
| Micro | Nghe câu trả lời "nghe / không" và lệnh giọng nói |
| Danh bạ, nhật ký cuộc gọi | Đọc tên người gọi, gọi theo tên, "gọi lại", "cuộc gọi nhỡ" |
| Hiển thị trên ứng dụng khác, không giới hạn pin | Để app không bị điện thoại tắt khi chạy nền |

**Chỉ tải app từ mục [Releases](https://github.com/dphuonganh019/blind-support/releases) của repo này.** Không cài file APK do người lạ gửi qua tin nhắn: app có các quyền rất mạnh, nên bản giả mạo có thể gây hại.

---

## 4. Tải và cài đặt

### 4.1. Tải file cài đặt

1. Trên điện thoại sẽ dùng, mở trang **[Releases](https://github.com/dphuonganh019/blind-support/releases)** của repo này.
2. Tải file `TroLyNgheGoi-vX.Y.Z.apk` của bản mới nhất (có nhãn **Latest**), rồi mở file để cài.
3. Nếu máy hỏi, cho phép trình duyệt/trình quản lý tệp **cài ứng dụng không rõ nguồn gốc**.
4. Nếu Google Play Protect cảnh báo, chọn **"Vẫn cài đặt"**. App không có trên CH Play nên cảnh báo này là bình thường.
   - Samsung báo không cho cài: Cài đặt → Bảo mật và quyền riêng tư → **Chặn tự động** → Tắt (cài xong có thể bật lại).

Cài xong, máy có **3 biểu tượng**:

| Biểu tượng | Dành cho |
|---|---|
| **Trợ Lý Nghe Gọi** | Mở lệnh giọng nói (người dùng thường mở bằng phím giảm âm lượng thay vì chạm vào đây) |
| **Cài đặt Trợ Lý** | Người hỗ trợ: cấp quyền, thử nghiệm, chỉnh tùy chọn |
| **Điện thoại** (nền xanh dương) | Bàn phím gọi cho người sáng mắt dùng chung máy (xem [mục 11](#11-người-khác-dùng-máy-để-gọi)) |

**Cập nhật:** tải bản mới và cài đè lên bản cũ. Cài đặt và quyền được giữ nguyên.

### 4.2. Màn hình "Cài đặt Trợ Lý"

Mở biểu tượng **Cài đặt Trợ Lý**. Làm cho tới khi **mọi mục ở phần 1 có dấu ✅** (bấm **"Sửa"** ở mục ❌, app sẽ mở đúng màn hình cài đặt của máy):

- **Là ứng dụng điện thoại mặc định**: bắt buộc để báo cuộc gọi sim.
- Quyền **micro, danh bạ, gọi điện, nhật ký cuộc gọi, trạng thái điện thoại, hiện thông báo**: bấm "Cấp tất cả quyền cơ bản".
- **Quyền đọc thông báo**: để đọc tin nhắn và báo cuộc gọi Zalo/WhatsApp.
- **Hỗ trợ tiếp cận / Trợ năng** → "Trợ Lý Nghe Gọi": để giữ phím giảm âm lượng ra lệnh và tự bật loa Zalo/WhatsApp.
  - Nếu máy báo **"Cài đặt bị hạn chế"**: vào Thông tin ứng dụng → ⋮ → **"Cho phép cài đặt hạn chế"**, rồi bật lại.
  - Samsung: Cài đặt → Hỗ trợ → Ứng dụng đã cài đặt → Trợ Lý Nghe Gọi.
- **Hiển thị trên ứng dụng khác**, **không giới hạn pin**, **không tự thu hồi quyền**, **hiện màn hình cuộc gọi khi khóa máy**.
- **Giọng đọc tiếng Việt** và **"đã tải về máy"**: nếu ❌, tải giọng tiếng Việt cho "Dịch vụ giọng nói của Google" (cần wifi một lần). Nếu giọng đọc lỗi, app phát 3 tiếng tít ngắn thay vì im lặng.
- **Nhận dạng giọng nói Google**: nếu ❌, cài hoặc cập nhật ứng dụng **Google** từ CH Play.
- **Bộ nhận dạng giọng nói offline** (dự phòng khi mất mạng).

Các phần khác trên màn hình này:

- **2. Thử nghiệm:** "Thử giọng đọc"; chọn **độ to** Mức 1–4 (nút đang chọn màu xanh, có dấu ✓; mặc định Mức 3); đọc chậm hơn / nhanh hơn; "Thử nhận lệnh giọng nói"; **"Giả lập cuộc gọi đến"**; các nút gửi nhật ký để báo lỗi.
- **3. Tùy chọn:** hỏi "nghe không" bằng giọng nói; **đổ chuông trước khi đọc tên** (0/3/6/10 giây); rung khi có cuộc gọi; tự bật loa ngoài; âm lượng cuộc gọi tối đa; báo cuộc gọi Zalo/WhatsApp; đọc tin Zalo / WhatsApp / SMS / tin nhóm / nội dung tin; báo pin; giữ phím giảm âm lượng để mở lệnh; giờ yên tĩnh.
- **4. Cách mở lệnh giọng nói** và **5. Cài đặt riêng theo hãng máy** (hướng dẫn hiện theo máy đang dùng).
- **6. Tạm tắt hoặc gỡ Trợ Lý** (xem [mục 13](#13-tạm-tắt-bật-lại-gỡ-ứng-dụng)).

### 4.3. Máy OPPO (ColorOS) – và các hãng hay tắt app chạy nền

ColorOS tắt app chạy nền rất mạnh. Làm cho **cả 3 app: Trợ Lý Nghe Gọi, Zalo và WhatsApp**. Xiaomi, Vivo, Realme có các mục tương tự (tên có thể khác: "Tự khởi động", "Không hạn chế pin"…).

1. Cài đặt → Quản lý ứng dụng → chọn app → bật **Cho phép tự khởi chạy** và **Cho phép ứng dụng khác khởi chạy**.
2. Cùng màn hình → **Mức sử dụng pin** → bật **Cho phép hoạt động nền**, tắt tối ưu hóa pin.
3. Cùng màn hình → Quyền → bật **Hiển thị trên màn hình khóa**, **Cửa sổ bật lên / mở giao diện khi chạy nền**, **Hiển thị trên ứng dụng khác**.
4. Mở danh sách app gần đây → **khóa** thẻ Trợ Lý Nghe Gọi và Zalo.
5. Cài đặt → Pin → Cài đặt khác: tắt **Tối ưu hóa chờ khi ngủ** (nếu có). Mục này cắt mạng khi máy nằm yên, làm Zalo không nhận tin cho tới khi bật màn hình.
6. Nút nguồn của OPPO chỉ mở được Camera / Trợ lý Google, nên dùng **giữ phím giảm âm lượng** để mở lệnh giọng nói.
7. Nên thử **tắt mạng** rồi ra một lệnh: nếu nhật ký ghi **[Google]** là máy có gói nhận dạng tiếng Việt ngoại tuyến của Google; ghi **[offline]** là đang dùng bộ dự phòng.

### 4.4. Máy Samsung

1. Nút nguồn mở lệnh (tùy chọn): Cài đặt → Tính năng nâng cao → Phím sườn → **Nhấn và giữ** → Trợ lý kỹ thuật số → **Trợ Lý Nghe Gọi**; hoặc **Nhấn hai lần** → Mở ứng dụng → Trợ Lý Nghe Gọi.
2. Pin: Cài đặt → Pin → Giới hạn sử dụng nền → **Ứng dụng không bao giờ ngủ** → thêm Trợ Lý Nghe Gọi, Zalo, WhatsApp; đặt pin của 3 app là **Không hạn chế**.

### 4.5. Zalo, WhatsApp và danh bạ

- Trong Zalo → Cá nhân → Cài đặt → Thông báo: bật báo **tin nhắn mới** và **cuộc gọi đến**. Cuộc trò chuyện nào bị "tắt thông báo" thì app cũng không đọc được.
- Lưu danh bạ **có dấu, dễ nghe** (ví dụ "Con Hoa", "Anh Minh", "Chị Lan"). App tìm tên không phân biệt dấu, nhưng tên có dấu thì giọng đọc đọc rõ hơn.
- Để **gọi Zalo/WhatsApp bằng giọng nói**, người đó cần có trong **danh bạ máy** với đúng số đăng ký Zalo/WhatsApp (Zalo/WhatsApp tự đồng bộ dòng "Gọi Zalo" / "Gọi thoại WhatsApp" vào danh bạ).
- Nên **tắt tự cập nhật Zalo/WhatsApp** trong CH Play, để Zalo không bất ngờ đổi giao diện (xem [mục 14](#14-giới-hạn-đã-biết)).

---

## 5. Hướng dẫn người dùng

Người dùng **không cần nhớ hết**. Hãy dạy **3 việc chính**, cho họ tự làm thử vài lần, và có bạn ngồi cạnh:

| # | Việc | Cách làm |
|---|---|---|
| 1 | **Nghe điện thoại** | Khi nghe app đọc tên người gọi và tiếng **bíp**, nói **"nghe"**. Gọi sim thì bấm phím bất kỳ ở cạnh máy cũng được. Không muốn nghe thì nói **"không"**, rồi **"đúng"**. |
| 2 | **Gọi cho ai đó** | **Giữ phím giảm âm lượng** đến khi máy rung và nói *"Mời nói lệnh"*. Sau tiếng bíp, nói **"gọi chị Lan"** hoặc **"gọi Zalo chị Lan"**. App hỏi lại, nói **"đúng"**. |
| 3 | **Nghe tin nhắn** | Giữ phím giảm âm lượng, nói **"đọc tin nhắn"**. Bấm nhanh phím giảm âm lượng để dừng đọc. |

Gợi ý khi hướng dẫn:
- Cho người dùng **sờ và nhớ vị trí phím giảm âm lượng**.
- Dùng nút **"Giả lập cuộc gọi đến"** trong Cài đặt Trợ Lý để tập nghe máy, rồi nhờ người nhà gọi thật bằng sim và bằng Zalo.
- Dặn: sau tiếng bíp mới nói, nói to và rõ. Nói chậm, ngập ngừng cũng không sao.
- Người dùng quên lệnh thì nói **"hướng dẫn"**, app đọc lại toàn bộ danh sách lệnh.
- Nếu người dùng nghe kém, chọn **Mức 4** hoặc dùng thêm loa / tai nghe Bluetooth (xem [mục 10](#10-độ-to)).

---

## 6. Chi tiết: cuộc gọi đến

### Cuộc gọi sim

1. App kéo **âm lượng chuông lên 100%** (gọi xong trả lại mức cũ), chuông máy reo **6 giây** trước (chỉnh được 0/3/6/10 giây), máy **rung liên tục**. Nếu máy đang để im lặng/rung thì 6 giây đầu sẽ im (Android không cho app bật chuông), nhưng sau đó app vẫn đọc tên và phát chuông riêng như bình thường.
2. App tắt chuông thật (để tiếng chuông không lọt vào micro) và nói: *"Có điện thoại! Chị Lan gọi. Sau tiếng bíp, nói: nghe. Hoặc bấm phím cạnh máy để nghe."* Số chưa lưu danh bạ thì đọc từng chữ số; số ẩn thì nói "số ẩn".
3. Tiếng bíp → app nghe khoảng **9 giây**.
4. Nếu chưa trả lời: app phát **tiếng chuông riêng** khoảng 1,5 giây (vẫn kêu khi máy để im lặng), rồi hỏi *"Bạn có muốn nghe máy không? Sau tiếng bíp, nói: nghe."* và lặp lại cho tới khi người gọi cúp máy.

> Nhà mạng thường chỉ cho đổ chuông khoảng 30–40 giây. Với cài đặt mặc định, người dùng có 2 lượt trả lời (khoảng giây 22 và giây 33). Nếu người dùng hay trả lời chậm, giảm "Đổ chuông trước khi đọc tên" xuống 3 giây.

| Muốn | Cách làm |
|---|---|
| Nghe máy | Nói **"nghe"** – chỉ cần có chữ "nghe" trong câu ("nghe em", "ừ nghe đi"…); hoặc "bắt máy", "trả lời", "có", "vâng", "ừ"; hoặc **bấm phím bất kỳ ở cạnh máy**; hoặc chạm **nửa trên màn hình** (nút NGHE màu xanh) |
| Từ chối | Nói **"không"** (hoặc "không nghe", "từ chối", "cúp máy"). App hỏi lại *"Từ chối cuộc gọi, đúng không?"*, nói **"đúng"** mới từ chối. Hoặc **giữ lâu** nửa dưới màn hình (nút TỪ CHỐI màu đỏ) |

Nghe máy xong app tự **bật loa ngoài** và đưa **âm lượng cuộc gọi lên tối đa**.

> Im lặng hoặc tiếng ồn (TV, quạt, vật nuôi…) **không bao giờ** làm app từ chối cuộc gọi: chữ "không" chỉ được tính khi bộ nhận dạng rất chắc chắn, và vẫn phải xác nhận lại.

### Cuộc gọi Zalo / WhatsApp

1. Chuông Zalo/WhatsApp reo khoảng **3 giây**, rồi app nói *"Có điện thoại Zalo! … gọi. Sau tiếng bíp, nói: nghe, để nghe."* Các lượt sau giống cuộc gọi sim.
2. Vì chuông Zalo/WhatsApp vẫn kêu (app không tắt được), app chỉ nhận câu có chữ **"nghe"**, "bắt máy", "trả lời" để nghe máy (không nhận "có", "ừ", "vâng" vì tiếng chuông hay bị nghe nhầm thành các từ này). Nói **"không"** để từ chối (có hỏi lại).
3. **Phím cạnh máy không dùng được** để nghe cuộc gọi Zalo/WhatsApp (Android không chuyển phím cho app).
4. Nghe máy xong, app tự chạm nút **Loa** của Zalo/WhatsApp. Nếu máy đang nối Bluetooth và Zalo hiện bảng chọn âm thanh, app chạm tiếp "Loa ngoài". Không bật được thì app nói *"Chưa bật được loa ngoài, hãy áp máy lên tai."*

### Cuộc gọi nhỡ

App đọc *"Bạn vừa nhỡ cuộc gọi từ …"* kèm **tên** hoặc **số điện thoại** (Zalo/WhatsApp thì nói *"…cuộc gọi Zalo từ …"*). Zalo đôi khi báo lại cả cuộc gọi sim nhỡ, nên app có thể báo vài lần.

---

## 7. Chi tiết: lệnh giọng nói

### Cách mở

- **Giữ phím GIẢM âm lượng khoảng 1,5 giây** (mọi hãng máy): máy rung nhẹ, app nói *"Mời nói lệnh sau tiếng bíp"*. Màn hình đang tắt app cũng tự bật sáng; nếu máy không rung thì bấm nút nguồn cho sáng rồi giữ lại.
- Samsung có thêm: nhấn giữ hoặc nhấn đúp **nút nguồn** (xem [4.4](#44-máy-samsung)).
- Bấm nhanh phím giảm âm lượng vẫn giảm âm lượng bình thường; **khi app đang đọc** thì bấm nhanh để **dừng đọc**.

### Danh sách lệnh

**Gọi điện**

| Nói | App làm |
|---|---|
| **"Gọi [tên]"** – ví dụ "gọi con Hoa", "gọi chị Lan" | Gọi sim người trong danh bạ |
| **"Gọi Zalo [tên]"**, **"Gọi WhatsApp [tên]"** | Gọi qua Zalo / WhatsApp, rồi tự bật loa ngoài |
| **"Gọi số"** kèm số, ví dụ "gọi số không chín một hai…" | Gọi theo số điện thoại |
| **"Gọi"** rồi dừng | App hỏi *"Bạn muốn gọi ai?"* |
| **"Gọi lại"** | Gọi lại người vừa gọi đến / vừa gọi nhỡ |
| **"Cuộc gọi nhỡ"**, "ai vừa gọi" | Đọc các cuộc gọi nhỡ trong 24 giờ |

**Tin nhắn**

| Nói | App làm |
|---|---|
| **"Đọc tin nhắn"** | Đọc các tin cá nhân chưa nghe (Zalo, WhatsApp, SMS) |
| **"Tin nhóm"** | Đọc tối đa 10 tin nhóm mới nhất |

**Thông tin và chỉnh giọng**

| Nói | App làm |
|---|---|
| **"Mấy giờ"**, "hôm nay thứ mấy", "ngày bao nhiêu" | Đọc giờ, thứ, ngày |
| **"Pin"**, "còn bao nhiêu pin" | Đọc mức pin, có đang sạc không |
| **"Danh bạ"** | Đọc tên trong danh bạ (tối đa 40 tên) |
| **"To lên"** / **"Nhỏ xuống"** | Chỉnh độ to giọng đọc |
| **"Nói chậm"** / **"Nói nhanh"** | Chỉnh tốc độ đọc |
| **"Hướng dẫn"** | Đọc lại toàn bộ danh sách lệnh |
| **"Thôi"**, "hủy", "không" | Đóng trợ lý |

### Gọi theo tên

- Nói **cả tên hoặc một phần tên**, không cần đúng thứ tự, **không phân biệt dấu**.
- **Một người khớp:** app hỏi *"Bạn muốn gọi cho …, đúng không? Nói: đúng, để gọi. Nói: không, để hủy."*
- **Nhiều người khớp (tối đa 6):** app đọc *"Có 3 người. Số 1: …. Số 2: …. Số 3: …. Bạn muốn gọi ai?"* – nói **số thứ tự** ("hai", "số hai", "người thứ hai") là **gọi ngay**. Hai người trùng hẳn tên thì đọc kèm 3 số cuối.
- **Hơn 6 người khớp / không tìm thấy:** app xin nói thêm tên, hoặc nói lại.

### Gọi theo số

- Nên nói **"gọi số"** rồi đọc số – có chữ "số" thì bộ nhận dạng nghe số chuẩn hơn.
- Số bắt đầu bằng 0 mà nghe **chưa đủ 10 số** (11 số với máy bàn), app nói *"Tôi mới nghe được 7 số: …. Số điện thoại cần 10 số. Hãy đọc lại cả số điện thoại."* Số ngắn như 113, 1900… gọi bình thường.
- App đọc lại số theo từng cụm, ví dụ *"Bạn muốn gọi số 0 9 1 2, 3 4 5, 6 7 8, đúng không? Nói: đúng, để thực hiện cuộc gọi. Nói: đọc lại, để đọc lại số. Nói: không, để hủy."*

### Trả lời "đúng / không"

- Đồng ý: **"đúng"**, "đúng rồi", "phải", "vâng", "có", "gọi đi".
- Từ chối: **"không"**, "sai", "thôi", "hủy".
- Im lặng: app hỏi lại một lần rồi hủy.

### Bộ nhận dạng giọng nói

- Lệnh giọng nói dùng **bộ nhận dạng của Google** có sẵn trên máy, nghe tên và số chính xác. Cần **mạng** (wifi/4G), hoặc máy đã có **gói tiếng Việt ngoại tuyến** của Google (ứng dụng Google → Cài đặt → Giọng nói). Mất mạng mà không có gói này thì app tự dùng bộ offline dự phòng (kém chính xác hơn nhiều).
- Câu hỏi "nghe hay không" khi có cuộc gọi đến luôn dùng bộ offline, không cần mạng.
- Sau tiếng bíp, nếu chưa nói gì trong khoảng 5 giây, app bíp ngắn và **nghe thêm một lượt**. Đã bắt đầu nói thì app chờ **im lặng khoảng 1,5 giây** mới coi là nói xong.
- Chữ app nghe được hiện trên màn hình (cho người hỗ trợ xem) và được ghi vào **nhật ký nhận dạng**.

---

## 8. Chi tiết: tin nhắn và thông báo

- **Tin Zalo, WhatsApp, SMS:** đọc to người gửi và nội dung (lọc emoji, đổi đường link thành "một đường link", cắt tin quá dài). Tin thoại Zalo chỉ được báo là "một tin nhắn thoại".
- **Tin nhóm:** mặc định chỉ lưu lại, không đọc to; nói "tin nhóm" để nghe (ví dụ *"nhóm Gia đình, chị Lan: chiều nay về nhé"*). Bật "Đọc to cả tin nhắn NHÓM" trong Cài đặt Trợ Lý nếu muốn đọc to luôn.
- Đang gọi điện thì không đọc mà lưu lại; gọi xong app nhắc số tin mới.
- Máy để im lặng / rung **vẫn đọc**.
- **Giờ yên tĩnh 22h–6h (mặc định bật):** ban đêm không đọc tin nhắn, **vẫn báo cuộc gọi**. Hết giờ yên tĩnh app nhắc số tin mới một lần.
- **Pin:** báo pin yếu (20%, 10%, 5%), cắm sạc, rút sạc, sạc đầy.
- **Cảnh báo:** nếu app mất quyền quan trọng, hoặc Trợ năng / dịch vụ đọc thông báo bị máy ngắt, app **đọc to cảnh báo** (vấn đề mới báo ngay, vấn đề cũ nhắc lại mỗi 6 tiếng). Người dùng nghe thấy cảnh báo này thì nên nhờ người hỗ trợ mở Cài đặt Trợ Lý và sửa các mục ❌.

---

## 9. Kiểm tra trước khi trao máy

Làm trên **chính chiếc máy sẽ trao**, với **giọng của người dùng thật**:

**Cuộc gọi đến**
- [ ] Gọi sim đến khi **màn hình khóa**: chuông reo, máy rung, nghe đọc tên; nói "nghe" → nhấc máy, loa ngoài bật
- [ ] Gọi sim đến, nói "không" → "đúng" → từ chối
- [ ] Gọi sim đến, **bấm phím bất kỳ ở cạnh máy** → nhấc máy
- [ ] Gọi sim đến, im lặng (để TV/quạt chạy) → app hỏi lại liên tục; cuộc gọi **không** bị từ chối
- [ ] Gọi từ số **không có trong danh bạ** → đọc từng chữ số
- [ ] Để nhỡ một cuộc gọi → nghe báo cuộc gọi nhỡ; giữ phím giảm âm lượng, nói "gọi lại"
- [ ] Gọi **Zalo** đến (cả khi màn hình khóa và khi mở khóa) → nói "nghe" → nhấc máy, **loa ngoài bật**
- [ ] Gọi **WhatsApp** đến → như trên

**Gọi đi**
- [ ] "Gọi [tên]" → "đúng" → gọi đi, loa ngoài bật
- [ ] "Gọi [một phần tên trùng nhiều người]" → nghe danh sách → nói số thứ tự → gọi đi
- [ ] "Gọi số …" đọc đủ 10 số → "đúng" → gọi đi; thử nói "đọc lại"
- [ ] "Gọi Zalo [tên]", "Gọi WhatsApp [tên]" → gọi đi, loa ngoài bật

**Độ bền**
- [ ] Nhắn tin Zalo, WhatsApp, SMS → nghe đọc nội dung; "đọc tin nhắn"; bấm nhanh phím giảm âm lượng để dừng
- [ ] **Khởi động lại máy** rồi thử lại 1–2 mục trên (không mở app)
- [ ] Vuốt xóa hết app ở đa nhiệm, bấm dọn dẹp → giữ phím giảm âm lượng vẫn mở lệnh; Trợ năng vẫn ✅
- [ ] Để máy **qua đêm**, màn tắt → sáng ra gọi sim và Zalo đến vẫn được báo
- [ ] **Người dùng tự làm một mình:** mở lệnh, gọi một người, nghe tin nhắn, nghe một cuộc gọi đến

---

## 10. Độ to

- Giọng đọc phát qua thanh âm lượng **"Chuông báo"**. App đặt thanh này về mức đã chọn (mặc định 100%) trong lúc đọc, đọc xong **trả lại mức cũ**.
- App **khuếch đại giọng đọc bằng phần mềm**: Mức 2 / 3 / 4 to hơn khoảng +4 / +6 / +8 dB. Mức 4 có thể hơi rè.
- Lệnh **"to lên"** tăng âm lượng tới 100% rồi tăng mức khuếch đại; **"nhỏ xuống"** thì ngược lại.
- Khi nghe máy, app đưa **âm lượng cuộc gọi lên tối đa**.
- **Người nghe rất kém** nên dùng thêm loa hoặc tai nghe Bluetooth (khi có tai nghe, app không bật loa ngoài).

---

## 11. Người khác dùng máy để gọi

Khi Trợ Lý là ứng dụng điện thoại mặc định, app Điện thoại gốc của máy có thể không gọi được. Người sáng mắt (khách, người hỗ trợ) dùng biểu tượng **"Điện thoại"** (nền xanh dương) của Trợ Lý:

- **Bàn phím:** bấm số → **GỌI**; dưới số hiện tên nếu có trong danh bạ; nút **Lưu số**.
- **Gần đây:** nhật ký cuộc gọi, nút **GỌI** để gọi lại.
- **Danh bạ:** ô tìm kiếm (gõ không dấu cũng được), nút **+ Thêm**.
- Trong lúc gọi có nút **BÀN PHÍM** (bấm phím khi gọi tổng đài); nút **kết thúc phải giữ lâu** để tránh chạm nhầm.

Nên đặt biểu tượng "Điện thoại" của Trợ Lý vào chỗ biểu tượng Điện thoại gốc trên màn hình chính.

---

## 12. Khi có sự cố

Việc đầu tiên luôn là: **mở Cài đặt Trợ Lý, sửa mọi mục ❌ ở phần 1.**

| Hiện tượng | Thường do | Cách xử lý |
|---|---|---|
| Có cuộc gọi sim mà app không đọc tên | Trợ Lý không còn là ứng dụng điện thoại mặc định | Cài đặt Trợ Lý → bấm "Sửa" ở mục đó |
| Cuộc gọi/tin Zalo không được báo, hoặc báo trễ | Máy tắt Zalo hoặc app chạy nền; thông báo Zalo bị tắt | Làm lại [4.3](#43-máy-oppo-coloros--và-các-hãng-hay-tắt-app-chạy-nền) / [4.4](#44-máy-samsung) và [4.5](#45-zalo-whatsapp-và-danh-bạ) cho cả Zalo |
| Giữ phím giảm âm lượng không mở lệnh | Trợ năng bị máy tắt | Bật lại Trợ năng → Trợ Lý Nghe Gọi |
| Nghe máy Zalo được nhưng không bật loa | Zalo vừa cập nhật, đổi giao diện | Báo lỗi kèm "thông tin màn hình Zalo" (bên dưới); tạm thời áp máy lên tai |
| Gọi theo tên hay nghe sai | Mất mạng (đang dùng bộ offline), hoặc tên trong danh bạ khó đọc | Kiểm tra wifi/4G; đặt tên danh bạ ngắn, rõ, có dấu |
| Giọng đọc quá nhỏ | Mức độ to thấp | Chọn Mức 4, hoặc dùng loa Bluetooth |

**Báo lỗi:** trong Cài đặt Trợ Lý → phần 2, bấm nút gửi nhật ký phù hợp ("Gửi nhật ký nhận dạng giọng nói", "Gửi thông tin màn hình Zalo", "Gửi nhật ký thông báo Zalo / WhatsApp"), lưu lại nội dung, rồi tạo một **[Issue](https://github.com/dphuonganh019/blind-support/issues)** trên repo này. Ghi kèm: **hãng và đời máy, phiên bản Android, phiên bản app** (hiện ở đầu Cài đặt Trợ Lý), và mô tả chuyện gì đã xảy ra. Nhật ký có thể chứa tên trong danh bạ và nội dung thông báo, hãy xóa phần riêng tư trước khi đăng công khai.

---

## 13. Tạm tắt, bật lại, gỡ ứng dụng

Ở **cuối trang** Cài đặt Trợ Lý:

- **"Tạm tắt hết"** (nút đỏ, có hỏi xác nhận): app ngừng mọi việc và **tự tắt Trợ năng** (phím giảm âm lượng trở lại bình thường). Sau đó máy mở màn hình **chọn ứng dụng mặc định**: chạm **"Ứng dụng điện thoại"** rồi chọn **Điện thoại gốc** của máy. Trong lúc chưa chọn lại, app vẫn hiện màn hình cuộc gọi bình thường để không bao giờ mất cuộc gọi.
- **"Bật lại Trợ Lý"** (nút xanh, cùng chỗ): bật lại mọi tính năng. Cần bật lại **Trợ năng** và chọn lại Trợ Lý làm **ứng dụng điện thoại** (bấm "Sửa" ở các mục ❌).
- **"Gỡ hẳn ứng dụng – trả máy như cũ"** (nút đỏ, có hỏi xác nhận): máy trở về như chưa cài. Android tự trả lại ứng dụng Điện thoại gốc và tự xóa mọi quyền của app. Sau khi gỡ, nên gọi thử một cuộc vào máy để kiểm tra.

**Không bấm "Buộc dừng"** app trong Cài đặt → Ứng dụng. App không cần luôn mở: Android tự đánh thức app khi có cuộc gọi, và một "người gác" chạy khoảng 15 phút một lần để kết nối lại dịch vụ nếu bị ngắt. Vuốt tắt app hay khởi động lại máy đều không ảnh hưởng.

---

## 14. Giới hạn đã biết

- **Phím cạnh máy** chỉ nghe được **cuộc gọi sim**, không nghe được cuộc gọi Zalo/WhatsApp.
- **Nghe máy và bật loa Zalo/WhatsApp** dựa vào việc app chạm nút trên màn hình cuộc gọi. Zalo/WhatsApp đổi giao diện sau khi cập nhật thì có thể tạm thời không bật được loa (app vẫn nói "hãy áp máy lên tai"). Nên tắt tự cập nhật Zalo/WhatsApp. Trên Android 12 trở lên, app không kiểm tra lại được loa đã bật thật hay chưa.
- **Lệnh giọng nói cần mạng** (hoặc gói tiếng Việt ngoại tuyến của Google). Bộ dự phòng khi mất mạng nghe tên và số kém.
- Đang trong một cuộc gọi mà có cuộc gọi chờ thì app chỉ đọc tên người gọi.
- Tin nhắn thoại Zalo chỉ được báo, chưa nghe được nội dung.
- Danh bạ giả do một số ứng dụng đồng hồ thông minh (ví dụ "Call Bridge") tạo ra không gọi được.
- Mới thử trên OPPO và Samsung. Rất mong nhận phản hồi từ các hãng máy khác qua [Issues](https://github.com/dphuonganh019/blind-support/issues).

---

## 15. Dành cho người phát triển

```
app/src/main/java/vn/blindsupport/app/
├── App.kt, Watchdog.kt     Khởi động, "người gác", nhận sự kiện bật máy
├── core/                   Giọng đọc (Speaker), nhận dạng giọng nói (SpeechInput: Google + Vosk dự phòng),
│                           hỏi "nghe không" (VoiceCallPrompt), âm lượng, loa, danh bạ, cài đặt, công tắc tổng (AppControl)
├── call/                   Ứng dụng điện thoại mặc định: InCallService, màn hình cuộc gọi
├── voip/                   Cuộc gọi Zalo / WhatsApp
├── notify/                 Đọc thông báo: tin nhắn, cuộc gọi Zalo/WhatsApp, cuộc gọi nhỡ, pin
├── a11y/                   Trợ năng: giữ phím giảm âm lượng, chạm nút Loa, Trả lời trong Zalo/WhatsApp
└── ui/                     Lệnh giọng nói (Commands, VoiceCommandActivity), Cài đặt Trợ Lý, bàn phím gọi
```

- Model nhận dạng offline `vosk-model-small-vn-0.4` (khoảng 32 MB) tự tải về khi build lần đầu, không lưu trong repo.
- Build: mở repo bằng Android Studio → **Build → Build APK(s)**, hoặc `./gradlew assembleRelease` (APK ở `app/build/outputs/apk/release/`). Kiểm thử: `./gradlew testReleaseUnitTest`.
- Mỗi lần đẩy lên nhánh `main`, GitHub Actions tự build và tạo bản phát hành mới. Đẩy lên nhánh `ci-check` để chỉ build + kiểm thử, không tạo bản phát hành.
- Các bản phát hành dùng chung một khóa ký, nên cài đè được lên nhau.
