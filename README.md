# Trợ Lý Nghe Gọi

App Android giúp người khiếm thị **nghe gọi điện thoại, gọi đi bằng giọng nói và nghe tin nhắn** (sim, Zalo, WhatsApp, SMS) bằng tiếng Việt. Người dùng không cần nhìn màn hình: app đọc to mọi thứ và nghe câu trả lời bằng giọng nói hoặc phím cạnh máy.

- Hỗ trợ Android 10 trở lên. Máy mục tiêu: **OPPO A92** (Android 11 / ColorOS 11.1); đã thử trên Samsung.
- Một người sáng mắt (người hỗ trợ) cần cài đặt máy một lần, khoảng 15–20 phút.

---

## 1. Cuộc gọi đến

### Cuộc gọi sim

1. App kéo **âm lượng chuông máy lên 100%** (gọi xong trả lại mức cũ), chuông máy reo **6 giây** trước (chỉnh được 0 / 3 / 6 / 10 giây), máy **rung liên tục** suốt lúc có cuộc gọi. Máy đang để chế độ im lặng/rung thì Android không cho app bật chuông – 6 giây đầu sẽ im, nhưng sau đó app vẫn đọc tên và phát chuông riêng như bình thường.
2. App tắt chuông thật (để tiếng chuông không lọt vào micro) và nói ngắn gọn:
   *"Có điện thoại! **Hưng Thỏ** gọi. Sau tiếng bíp, nói: nghe. Hoặc bấm phím cạnh máy để nghe."*
   Số chưa lưu danh bạ thì đọc từng chữ số; số ẩn thì nói "số ẩn".
3. Tiếng bíp ngắn → app nghe ngay sau tiếng bíp, khoảng **9 giây**.
4. Nếu chưa trả lời: app phát **tiếng chuông của riêng app** khoảng 1,5 giây (vẫn kêu khi máy để im lặng), rồi hỏi ngắn: *"Bạn có muốn nghe máy không? Sau tiếng bíp, nói: nghe."* (nghe 7 giây) – lặp lại cho tới khi người gọi cúp máy.

> Nhà mạng thường chỉ cho đổ chuông khoảng 30–40 giây rồi tự cắt (bên gọi có thể nghe "đường dây bận"). Với cài đặt mặc định, lần nghe thứ nhất xong ở khoảng giây 22, lần thứ hai ở khoảng giây 33. Nếu người dùng hay trả lời chậm, giảm "Đổ chuông trước khi đọc tên" xuống 3 giây.

Cách trả lời:

| Muốn | Cách làm |
|---|---|
| Nghe máy | Nói **"nghe"** – chỉ cần app nghe ra chữ "nghe" ở bất kỳ đâu trong câu là nhấc máy, dù có thêm chữ khác ("nghe em", "ừ nghe đi", "sẽ nghe"…); ngoài ra "bắt máy", "trả lời", "có", "vâng", "ừ" – hoặc **bấm phím bất kỳ ở cạnh máy** (tăng/giảm âm lượng, nguồn) – hoặc chạm **nửa trên màn hình** (nút NGHE màu xanh) |
| Từ chối | Nói **"không"** (hoặc "không nghe", "từ chối", "tắt máy", "cúp máy"; "không / chưa / đừng" đứng ngay trước "nghe" thì không tính là nghe máy), app hỏi lại *"Từ chối cuộc gọi, đúng không?"* – nói **"đúng"** (hoặc "không" lần nữa) mới từ chối – hoặc **giữ lâu** nửa dưới màn hình (nút TỪ CHỐI màu đỏ) |

Nghe máy xong app tự **bật loa ngoài** và đưa **âm lượng cuộc gọi lên tối đa**.

> Im lặng hoặc tiếng ồn (mèo kêu, quạt, TV…) **không bao giờ** làm app từ chối cuộc gọi: chữ "không" chỉ được tính khi bộ nhận dạng rất chắc chắn, và vẫn phải xác nhận lại. Không trả lời gì thì app tiếp tục đổ chuông và hỏi lại.
>
> Vì sao mọi phím cạnh máy đều là "nghe": lúc cuộc gọi sim đổ chuông, Android giữ phím lại và chỉ báo cho app là "người dùng vừa bấm phím", không cho biết phím nào.

### Cuộc gọi Zalo / WhatsApp

1. Chuông Zalo/WhatsApp reo khoảng **3 giây** (Zalo đổ chuông ngắn hơn sim), rồi app nói: *"Có điện thoại Zalo! … gọi. Sau tiếng bíp, nói: nghe, để nghe."* Các lần sau như cuộc gọi sim.
2. Vì chuông Zalo/WhatsApp vẫn kêu (app không tắt được), app chỉ nhận câu có chữ **"nghe"** (như trên), "bắt máy", "trả lời" để nghe máy (không nhận "có", "ừ", "vâng" – tiếng chuông hay bị nghe nhầm thành các từ này). Nói **"không"** để từ chối (có hỏi lại).
   - Trên **Android 11 trở xuống** (như OPPO A92), Zalo báo cuộc gọi theo kiểu cũ. App nhận ra cuộc gọi qua chữ "Cuộc gọi thoại đến / Cuộc gọi video đến" trong thông báo, hoặc trên màn hình cuộc gọi của Zalo (có nút Trả lời / Từ chối). Khi nói "nghe", app tự chạm nút **Trả lời** của Zalo.
3. **Phím cạnh máy không dùng được** để nghe cuộc gọi Zalo/WhatsApp (Android không chuyển phím cho app), nên app không nhắc cách này.
4. Nghe máy xong, app tự bật **loa ngoài** bằng Trợ năng:
   - chờ màn hình cuộc gọi Zalo/WhatsApp hiện ổn định và cuộc gọi đã kết nối, rồi chạm nút **Loa** một lần;
   - nếu máy đang nối Bluetooth và Zalo hiện bảng chọn âm thanh, app chạm tiếp **"Loa ngoài"**;
   - không bật được thì app nói *"Chưa bật được loa ngoài, hãy áp máy lên tai."* (trên Android 11 như A92, app còn kiểm tra lại được loa đã bật thật chưa).

### Cuộc gọi nhỡ

App đọc to *"Bạn vừa nhỡ cuộc gọi từ …"*, luôn kèm **tên** (danh bạ máy, sim, Zalo/WhatsApp) hoặc **số điện thoại**. Cuộc gọi Zalo/WhatsApp nhỡ thì nói *"…cuộc gọi Zalo từ …"*. Zalo còn báo lại cả cuộc gọi sim nhỡ của người đã kết bạn: app vẫn đọc đúng tên/số (lấy từ nhật ký cuộc gọi) và có thể báo vài lần – càng giúp người dùng nghe rõ ai gọi.

---

## 2. Ra lệnh bằng giọng nói

### Cách mở

- **Giữ phím GIẢM âm lượng khoảng 1,5 giây** (mọi hãng máy): máy rung nhẹ, app nói *"Mời nói lệnh sau tiếng bíp"*. Màn hình đang tắt app cũng tự bật sáng; nếu máy không rung thì bấm nút nguồn cho sáng rồi giữ lại.
- Samsung có thêm: nhấn giữ hoặc nhấn đúp **nút nguồn** (xem phần cài đặt).
- Bấm nhanh phím giảm âm lượng vẫn giảm âm lượng bình thường; **khi app đang đọc** (ví dụ đọc tin nhắn) thì bấm nhanh để **dừng đọc**.

### Danh sách lệnh

**Gọi điện**

| Nói | App làm |
|---|---|
| **"Gọi [tên]"** – ví dụ "gọi Mẹ", "gọi chị Trang", "gọi Hưng" | Gọi sim người trong danh bạ (xem cách tìm tên bên dưới) |
| **"Gọi Zalo [tên]"**, **"Gọi WhatsApp [tên]"** | Gọi qua Zalo / WhatsApp, rồi tự bật loa ngoài |
| **"Gọi số"** kèm số, ví dụ "gọi số không ba bốn năm…" | Gọi theo số điện thoại |
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

- Nói **cả tên hoặc một phần tên**, không cần đúng thứ tự, **không phân biệt dấu** (danh bạ lưu "Hung Tho" vẫn gọi được khi nói "Hưng Thỏ"). Tuy vậy nên lưu danh bạ **có dấu** để giọng đọc đọc tên cho dễ nghe.
- **Một người khớp:** app hỏi *"Bạn muốn gọi cho …, đúng không? Nói: đúng, để gọi. Nói: không, để hủy."*
- **Nhiều người khớp (tối đa 6):** app đọc *"Có 3 người. Số 1: …. Số 2: …. Số 3: …. Bạn muốn gọi ai?"* – nói **số thứ tự** ("hai", "số hai", "người thứ hai") là **gọi ngay**; nói "hủy" để thôi. Hai người trùng hẳn tên thì đọc kèm 3 số cuối. Cùng một số điện thoại chỉ đọc một lần.
- **Hơn 6 người khớp / không tìm thấy:** app xin nói thêm tên, hoặc nói lại.

### Gọi theo số

- Nên nói **"gọi số"** rồi đọc số – có chữ "số" bộ nhận dạng nghe số chuẩn hơn.
- Số bắt đầu bằng 0 mà nghe **chưa đủ 10 số** (11 số với máy bàn), app nói *"Tôi mới nghe được 7 số: …. Số điện thoại cần 10 số. Hãy đọc lại cả số điện thoại."* Số ngắn như 113, 1900… gọi bình thường.
- App đọc lại: *"Bạn muốn gọi số 0 3 4 5, 0 5 3, 0 0 3, đúng không? Nói: đúng, để thực hiện cuộc gọi. Nói: đọc lại, để đọc lại số. Nói: không, để hủy."*

### Trả lời "đúng / không"

- Đồng ý: **"đúng"**, "đúng rồi", "phải", "vâng", "có", "gọi đi" (kể cả khi bộ nhận dạng nghe thành "Đốm").
- Từ chối: **"không"**, "sai", "thôi", "hủy".
- Im lặng: app hỏi lại một lần rồi hủy.

### Bộ nhận dạng giọng nói

- Lệnh giọng nói dùng **bộ nhận dạng giọng nói của Google** có sẵn trên máy – nghe tên và số chính xác. Cần **mạng** (wifi/4G), hoặc máy đã có **gói tiếng Việt ngoại tuyến** của Google (trong ứng dụng Google → Cài đặt → Giọng nói). Khi mất mạng mà không có gói này, app tự dùng bộ nhận dạng offline dự phòng (kém chính xác hơn nhiều).
- Câu hỏi "nghe hay không" khi có cuộc gọi đến luôn dùng bộ offline (không cần mạng).
- Sau tiếng bíp, nếu người dùng chưa nói gì trong khoảng 5 giây, app bíp ngắn và **nghe thêm một lượt** (tổng khoảng 10 giây). Đã bắt đầu nói thì app chờ người dùng **im lặng khoảng 1,5 giây** mới coi là nói xong, nên nói chậm, ngập ngừng không sao.
- Chữ app nghe được hiện trên màn hình (cho người hỗ trợ xem) và được ghi vào **nhật ký nhận dạng** (Cài đặt Trợ Lý → "Gửi nhật ký nhận dạng giọng nói").

---

## 3. Tin nhắn và thông báo khác

- **Tin Zalo, WhatsApp, SMS:** đọc to người gửi và nội dung (lọc emoji, cắt tin quá dài). Tin thoại Zalo chỉ được báo "một tin nhắn thoại".
- **Tin nhóm:** mặc định chỉ lưu lại (riêng với tin cá nhân), không đọc to; nói "tin nhóm" để nghe (app đọc *"nhóm hello, Thỏ Ngoan: Hi"*). Bật mục "Đọc to cả tin nhắn NHÓM" trong Cài đặt Trợ Lý nếu muốn đọc to cả tin nhóm. App nhận ra tin nhóm Zalo qua tiêu đề "Nhóm: …", tiêu đề cuộc trò chuyện khác tên người gửi, hoặc có nhiều người gửi (Zalo không tự đánh dấu tin nhóm).
- Đang gọi điện thì không đọc, lưu lại; gọi xong app nhắc số tin mới.
- Máy để im lặng / rung **vẫn đọc** (người khiếm thị không nhìn thấy máy đang im lặng).
- **Giờ yên tĩnh 22h–6h mặc định bật:** ban đêm không đọc tin nhắn (vẫn báo cuộc gọi); hết giờ yên tĩnh app nhắc số tin mới một lần.
- **Pin:** báo pin yếu (20%, 10%, 5%), cắm sạc, rút sạc, sạc đầy.
- **Cảnh báo:** nếu app mất quyền quan trọng, hoặc Trợ năng / dịch vụ đọc thông báo bị hệ thống ngắt, app **đọc to cảnh báo** (vấn đề mới báo ngay, vấn đề cũ nhắc lại mỗi 6 tiếng).

---

## 4. Tải và cài APK

Vào tab **Releases** của repo, tải file `TroLyNgheGoi-vX.Y.Z.apk` mới nhất về điện thoại và mở để cài. Mỗi lần có thay đổi trên nhánh `main`, GitHub Actions tự build và tạo bản Release mới.

Các bản đều ký cùng một khóa (`keystore/release.jks`), nên **cài bản mới đè lên bản cũ** được mà không mất cài đặt và quyền. Đừng xóa hay đổi file khóa này.

App có 3 biểu tượng: **Trợ Lý Nghe Gọi** (mở lệnh giọng nói), **Cài đặt Trợ Lý** (cho người hỗ trợ), **Điện thoại** (bàn phím gọi cho người sáng mắt).

---

## 5. Cài đặt lần đầu (người hỗ trợ làm)

### 5.1. Màn hình "Cài đặt Trợ Lý"

Mở biểu tượng **Cài đặt Trợ Lý**, làm cho tới khi mọi mục ở phần 1 có dấu ✅ (bấm **"Sửa"** ở mục ❌):

- **Là ứng dụng điện thoại mặc định** – bắt buộc để báo cuộc gọi sim.
- Quyền **micro, danh bạ, gọi điện, nhật ký cuộc gọi, trạng thái điện thoại, hiện thông báo** – bấm "Cấp tất cả quyền cơ bản".
- **Quyền đọc thông báo** – đọc tin nhắn, báo cuộc gọi Zalo/WhatsApp.
- **Hỗ trợ tiếp cận / Trợ năng** → "Trợ Lý Nghe Gọi" – giữ phím giảm âm lượng để ra lệnh, tự bật loa Zalo/WhatsApp. Đường dẫn trên Samsung: Cài đặt → Hỗ trợ → Ứng dụng đã cài đặt → Trợ Lý Nghe Gọi. Nếu Samsung báo **"Cài đặt bị hạn chế"**: Thông tin ứng dụng → ⋮ → **"Cho phép cài đặt hạn chế"**, rồi bật lại.
- **Hiển thị trên ứng dụng khác**, **không giới hạn pin**, **không tự thu hồi quyền**, **hiện màn hình cuộc gọi khi khóa máy**.
- **Giọng đọc tiếng Việt** và **"đã tải về máy"** – nếu ❌, tải giọng tiếng Việt cho "Dịch vụ giọng nói của Google". Nếu giọng đọc lỗi, app phát **3 tiếng tít ngắn** thay vì im lặng.
- **Nhận dạng giọng nói Google** – nếu ❌, cài/cập nhật ứng dụng Google từ CH Play.
- **Bộ nhận dạng giọng nói offline** (dự phòng).

Các phần khác trên màn hình:

- **2. Thử nghiệm:** "Thử giọng đọc"; chọn **độ to** Mức 1–4 (nút đang chọn **màu xanh, có dấu ✓**; mặc định Mức 3); "Đọc chậm hơn / nhanh hơn" (hiện tốc độ mới); "Thử nhận lệnh giọng nói"; "Giả lập cuộc gọi đến"; "Gửi nhật ký nhận dạng giọng nói"; "Gửi thông tin màn hình Zalo (lỗi loa ngoài)"; "Gửi nhật ký thông báo Zalo / WhatsApp" (khi cuộc gọi hay tin Zalo/WhatsApp không được báo hoặc báo trễ: ghi giờ app nhận từng thông báo và việc app đã làm, nhìn vào biết Zalo trễ hay app trễ).
- **3. Tùy chọn:** hỏi "nghe không" bằng giọng nói; **đổ chuông trước khi đọc tên** (0/3/6/10 giây, bấm để đổi); **rung khi có cuộc gọi**; tự bật loa ngoài; âm lượng cuộc gọi tối đa; báo cuộc gọi Zalo/WhatsApp; đọc tin Zalo / WhatsApp / SMS / tin nhóm / nội dung tin; báo pin; giữ phím giảm âm lượng để mở lệnh; giờ yên tĩnh.
- **4. Cách mở lệnh giọng nói** và **5. Cài đặt riêng theo hãng máy** (hướng dẫn hiện theo máy đang dùng).
- **6. Tạm tắt hoặc gỡ Trợ Lý** – xem mục 7 bên dưới.

### 5.2. Máy OPPO (ColorOS)

ColorOS tắt app chạy nền rất mạnh. Làm cho **cả Trợ Lý Nghe Gọi, Zalo và WhatsApp**:

1. Cài đặt → Quản lý ứng dụng → chọn app → bật **Cho phép tự khởi chạy** và **Cho phép ứng dụng khác khởi chạy**.
2. Cùng màn hình → **Mức sử dụng pin** → bật **Cho phép hoạt động nền**, tắt tối ưu hóa pin.
3. Cùng màn hình → Quyền → bật **Hiển thị trên màn hình khóa**, **Cửa sổ bật lên / mở giao diện khi chạy nền**, **Hiển thị trên ứng dụng khác**.
4. Mở danh sách app gần đây → **khóa** thẻ Trợ Lý Nghe Gọi và Zalo.
   - Cài đặt → Pin → Cài đặt khác: tắt **Tối ưu hóa chờ khi ngủ** (nếu có). Mục này cắt mạng khi máy nằm yên, tắt màn hình, làm Zalo không nhận tin cho tới khi bật màn hình.
   - Trong Zalo → Cá nhân → Cài đặt → Thông báo: bật báo tin nhắn mới và cuộc gọi đến. Cuộc trò chuyện nào bị "tắt thông báo" thì app cũng không đọc được.
5. Nút nguồn của OPPO chỉ mở được Camera / Trợ lý Google, nên dùng **giữ phím giảm âm lượng** để mở lệnh giọng nói.
6. Cần wifi một lần để tải giọng đọc tiếng Việt của Google. Nên thử **tắt mạng** rồi ra một lệnh: nếu nhật ký ghi **[Google]** là máy có gói nhận dạng tiếng Việt ngoại tuyến; ghi **[offline]** là đang dùng bộ dự phòng.

### 5.3. Máy Samsung

1. Nếu máy báo không cho cài: Cài đặt → Bảo mật và quyền riêng tư → **Chặn tự động** → Tắt (cài xong có thể bật lại).
2. Nút nguồn mở lệnh (tùy chọn): Cài đặt → Tính năng nâng cao → Phím sườn → **Nhấn và giữ** → Trợ lý kỹ thuật số → **Trợ Lý Nghe Gọi**; hoặc **Nhấn hai lần** → Mở ứng dụng → Trợ Lý Nghe Gọi.
3. Pin: Cài đặt → Pin → Giới hạn sử dụng nền → **Ứng dụng không bao giờ ngủ** → thêm Trợ Lý Nghe Gọi, Zalo, WhatsApp; đặt pin 3 app là **Không hạn chế**.

### 5.4. Danh bạ

- Lưu tên **có dấu, dễ đọc** (ví dụ "Mẹ", "Anh Hưng", "Chị Trang").
- Gọi Zalo/WhatsApp bằng giọng nói dùng dòng **"Gọi Zalo" / "Gọi thoại WhatsApp"** mà Zalo/WhatsApp tự đồng bộ vào danh bạ máy, nên người đó cần có trong danh bạ máy với đúng số đăng ký Zalo/WhatsApp.
- Một số ứng dụng đồng hồ thông minh (ví dụ "Call Bridge") tự tạo danh bạ giả tên "Zalo …" với số ảo – các mục này không gọi được; máy không cài các ứng dụng đó thì không gặp.

---

## 6. Danh sách kiểm tra trước khi trao máy

Làm trên **chính chiếc máy sẽ trao**, với giọng người dùng thật:

**Cuộc gọi đến**
- [ ] Gọi sim đến khi **màn hình khóa**: chuông reo ~6 giây, máy rung, nghe đọc tên; nói "nghe" → nhấc máy, loa ngoài bật
- [ ] Gọi sim đến, nói "không" → từ chối
- [ ] Gọi sim đến, **bấm phím bất kỳ ở cạnh máy** → nhấc máy
- [ ] Gọi sim đến, im lặng (để TV/quạt chạy) → nghe tiếng chuông của app và câu hỏi ngắn lặp lại; cuộc gọi **không** bị từ chối
- [ ] Gọi từ số **không có trong danh bạ** → đọc từng chữ số
- [ ] Để nhỡ một cuộc gọi sim → nghe báo cuộc gọi nhỡ kèm tên/số; giữ phím giảm âm lượng, nói "gọi lại"
- [ ] Gọi **Zalo** đến (màn hình khóa và mở khóa) → nói "nghe" → nhấc máy, **loa ngoài bật**
- [ ] Gọi **WhatsApp** đến → như trên

**Gọi đi**
- [ ] "Gọi [tên]" → "đúng" → gọi đi, loa ngoài bật
- [ ] "Gọi [một phần tên trùng nhiều người]" → nghe danh sách → nói số thứ tự → gọi đi
- [ ] "Gọi số …" đọc đủ 10 số → "đúng" → gọi đi; thử nói "đọc lại"
- [ ] "Gọi Zalo [tên]", "Gọi WhatsApp [tên]" → mở cuộc gọi Zalo/WhatsApp, loa ngoài bật

**Khác**
- [ ] Nhắn tin Zalo, WhatsApp, SMS → nghe đọc nội dung; "đọc tin nhắn"; bấm nhanh phím giảm âm lượng để dừng
- [ ] **Khởi động lại máy** rồi lặp lại 1–2 mục trên (không mở app)
- [ ] Vuốt xóa hết app ở đa nhiệm, bấm dọn dẹp → giữ phím giảm âm lượng vẫn mở lệnh; Trợ năng vẫn ✅
- [ ] Để máy qua đêm, màn tắt → sáng ra gọi sim và Zalo đến vẫn được báo
- [ ] Người dùng tự làm một mình: mở lệnh, gọi một người, nghe tin nhắn, nghe một cuộc gọi đến

---

## 7. Tạm tắt, bật lại, gỡ ứng dụng

Ở **cuối trang** Cài đặt Trợ Lý (mục 6):

- **"Tạm tắt hết"** (nút đỏ, có hỏi xác nhận): app ngừng mọi việc – không đọc tên người gọi, không đọc tin nhắn, không nghe lệnh, không bật loa, không báo pin; **tự tắt Trợ năng** (phím giảm âm lượng trở lại bình thường); trả lại âm lượng; ngừng "người gác". Sau đó máy mở màn hình **chọn ứng dụng mặc định**: chạm **"Ứng dụng điện thoại"** rồi chọn ứng dụng **Điện thoại gốc** của máy (Android bắt buộc người dùng tự chọn). Trong lúc chưa chọn lại, app vẫn hiện màn hình cuộc gọi bình thường để máy không bao giờ mất cuộc gọi. Đầu trang Cài đặt hiện dòng đỏ "Trợ Lý đang TẠM TẮT".
- **"Bật lại Trợ Lý"** (nút xanh, cùng chỗ): bật lại mọi tính năng. Android bắt buộc người dùng tự bật lại **Trợ năng** và chọn lại Trợ Lý làm **ứng dụng điện thoại** – bấm "Sửa" ở các mục ❌ đầu trang.
- **"Gỡ hẳn ứng dụng – trả máy như cũ"** (nút đỏ, có hỏi xác nhận): máy trở về như chưa cài – Android tự trả lại ứng dụng Điện thoại gốc, tự xóa mọi quyền và Trợ năng của app. Sau khi gỡ, nên gọi thử một cuộc vào máy để kiểm tra.

---

## 8. Người khác dùng máy để gọi (khách, người hỗ trợ)

Khi Trợ Lý là ứng dụng điện thoại mặc định, app Điện thoại gốc của máy có thể không cho gọi. Dùng biểu tượng **"Điện thoại"** (nền xanh dương) của Trợ Lý:

- **Bàn phím:** bấm số → **GỌI**; dưới số hiện tên nếu có trong danh bạ; nút **Lưu số**.
- **Gần đây:** nhật ký cuộc gọi; nút **GỌI** để gọi lại.
- **Danh bạ:** ô tìm kiếm (gõ không dấu cũng được), nút **+ Thêm**.
- Trong lúc gọi có nút **BÀN PHÍM** (bấm phím khi gọi tổng đài); nút **kết thúc phải giữ lâu** để tránh chạm nhầm.
- App Danh bạ của máy vẫn dùng bình thường.

Nên đặt biểu tượng "Điện thoại" của Trợ Lý vào chỗ biểu tượng Điện thoại gốc trên màn hình chính.

---

## 9. Độ to

- Giọng đọc phát qua thanh âm lượng **"Chuông báo"**. App đặt thanh này về mức đã chọn (mặc định 100%) trong lúc đọc, đọc xong **trả lại mức cũ** (báo thức đồng hồ không bị to lên).
- App **khuếch đại giọng đọc bằng phần mềm**: Mức 2 / 3 / 4 to hơn khoảng +4 / +6 / +8 dB. Mức 4 có thể hơi rè.
- Lệnh **"to lên"**: tăng âm lượng tới 100% rồi tăng mức khuếch đại; **"nhỏ xuống"**: ngược lại.
- Khi nghe máy, app đưa **âm lượng cuộc gọi lên tối đa**.
- Người nghe rất kém nên dùng thêm loa hoặc tai nghe Bluetooth (khi có tai nghe, app không bật loa ngoài).

---

## 10. App có cần luôn mở không?

Không. App dựa vào các dịch vụ do Android quản lý: **ứng dụng điện thoại mặc định** (Android đánh thức app khi có cuộc gọi sim), **quyền đọc thông báo** và **Trợ năng** (Android giữ chạy thường trực). Ngoài ra có "người gác" chạy khoảng 15 phút một lần và sau khi bật máy để kết nối lại dịch vụ và cảnh báo khi mất quyền. Vuốt tắt app, "Đóng tất cả", khởi động lại máy đều không ảnh hưởng. **Không bấm "Buộc dừng"** app trong Cài đặt → Ứng dụng.

---

## 11. Giới hạn đã biết

- **Phím cạnh máy** chỉ nghe được **cuộc gọi sim**, không nghe được cuộc gọi Zalo/WhatsApp.
- **Bật loa Zalo/WhatsApp** dựa vào việc chạm nút "Loa" trên màn hình cuộc gọi. Nếu Zalo/WhatsApp đổi giao diện sau khi cập nhật, có thể không bật được loa (app vẫn nghe máy được và nói "hãy áp máy lên tai"). Có thể tắt tự cập nhật Zalo/WhatsApp trong CH Play. Trên Android 12 trở lên, app không kiểm tra lại được loa đã bật thật hay chưa.
- **Lệnh giọng nói cần mạng** (hoặc gói tiếng Việt ngoại tuyến của Google). Bộ nhận dạng dự phòng khi mất mạng nghe tên và số kém.
- Khi đang trong một cuộc gọi mà có cuộc gọi chờ, app chỉ đọc tên người gọi (micro đang bận).
- Tin nhắn thoại Zalo chỉ được báo, chưa nghe được nội dung.
- Danh bạ giả do ứng dụng đồng hồ thông minh (ví dụ "Call Bridge") tạo ra không gọi được.

---

## 12. Dành cho người phát triển

```
app/src/main/java/vn/blindsupport/app/
├── App.kt, Watchdog.kt     Khởi động, "người gác", nhận sự kiện bật máy
├── core/                   Giọng đọc (Speaker), nhận dạng giọng nói (SpeechInput: Google + Vosk dự phòng),
│                           hỏi "nghe không" (VoiceCallPrompt), âm lượng, loa, danh bạ, cài đặt, công tắc tổng (AppControl)
├── call/                   Ứng dụng điện thoại mặc định: InCallService, màn hình cuộc gọi
├── voip/                   Cuộc gọi Zalo / WhatsApp
├── notify/                 Đọc thông báo: tin nhắn, cuộc gọi Zalo/WhatsApp, cuộc gọi nhỡ, pin
├── a11y/                   Trợ năng: giữ phím giảm âm lượng, bấm/chạm nút Loa, Trả lời trong Zalo/WhatsApp
└── ui/                     Lệnh giọng nói (Commands, VoiceCommandActivity), Cài đặt Trợ Lý, bàn phím gọi
```

- Model nhận dạng offline `vosk-model-small-vn-0.4` (khoảng 32 MB) tự tải về khi build lần đầu, không lưu trong repo.
- Build: mở repo bằng Android Studio → **Build → Build APK(s)**, hoặc `./gradlew assembleRelease` (APK ở `app/build/outputs/apk/release/`). Kiểm thử: `./gradlew testReleaseUnitTest`.
- Đẩy lên nhánh `ci-check` để GitHub Actions chỉ build + kiểm thử, không tạo bản phát hành.
