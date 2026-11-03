# Trợ Lý Nghe Gọi

App Android giúp người khiếm thị nghe gọi điện thoại và nghe tin nhắn bằng giọng nói tiếng Việt. Toàn bộ nhận dạng giọng nói và giọng đọc chạy **offline**, không cần internet (riêng Zalo/WhatsApp thì bản thân chúng cần mạng).

## Tính năng

| Tình huống | App làm gì |
|---|---|
| Có cuộc gọi sim đến | Đọc tên trong danh bạ (hoặc đọc từng số), hỏi *"Bạn có muốn nghe không?"*. Nói **"nghe"** → nhấc máy và bật loa ngoài. Nói **"không"** → từ chối. Phím tăng âm lượng = nghe; phím giảm âm lượng = **tắt chuông, ngừng hỏi** (không từ chối). Trên màn hình phải **giữ** nút mới từ chối được. Trong lúc app chờ câu trả lời, chuông cuộc gọi sim được tắt để tiếng chuông không lọt vào micro gây nhận nhầm. |
| Có cuộc gọi Zalo / WhatsApp | Như trên, dùng nút Trả lời/Từ chối trong thông báo của Zalo/WhatsApp; sau khi nghe máy, app tự bấm nút **Loa** (cần bật Trợ năng). Nếu không bấm được loa, app nói *"hãy áp máy lên tai"*. |
| Cuộc gọi nhỡ (sim, Zalo, WhatsApp) | Đọc to: *"Bạn vừa nhỡ cuộc gọi từ …"* |
| Tin nhắn Zalo, WhatsApp, SMS | Đọc to người gửi và nội dung. Tin nhóm mặc định chỉ lưu lại, không đọc to. Đang gọi điện thì không đọc, lưu lại để nghe sau. Máy để im lặng **vẫn đọc**. Có thể bật **giờ yên tĩnh** (22h–6h, mặc định tắt) để không đọc tin ban đêm. |
| Bấm đúp nút nguồn | Mở chế độ nghe lệnh (xem bảng lệnh bên dưới). |
| Pin | Báo pin yếu (20%, 10%, 5%), cắm sạc, rút sạc, sạc đầy. |

### Lệnh giọng nói (sau khi bấm đúp nút nguồn và nghe tiếng bíp)

Mọi câu hỏi của app đều kết thúc bằng *"… sau tiếng bíp"*, rồi kêu một tiếng bíp to, dài khoảng 2 giây. Nói sau khi tiếng bíp tắt.

- **"Gọi Mẹ"**, **"Gọi cho anh Hưng"** – gọi theo tên trong danh bạ (app hỏi lại để xác nhận)
- **"Gọi số"** rồi đọc từng chữ số (ví dụ "không chín một hai…"), đọc xong im lặng chờ – app đọc lại số để xác nhận rồi mới gọi. Có thể nói liền "gọi số không chín một hai…".
- **"Gọi lại"** – gọi lại người vừa gọi đến / gọi nhỡ
- **"Đọc tin nhắn"** – đọc các tin chưa nghe
- **"Cuộc gọi nhỡ"** – đọc danh sách cuộc gọi nhỡ trong 24 giờ
- **"Mấy giờ"**, **"Pin"**
- **"Danh bạ"** – đọc tên trong danh bạ
- **"To lên" / "Nhỏ xuống"** – chỉnh độ to giọng đọc
- **"Nói chậm" / "Nói nhanh"** – chỉnh tốc độ đọc
- **"Hướng dẫn"** – nghe lại danh sách lệnh

## Tải APK

Mỗi lần có thay đổi trên nhánh `main`, GitHub Actions tự build và tạo một bản **Release** có file `TroLyNgheGoi-vX.Y.Z.apk`. Vào tab **Releases** của repo, tải file `.apk` mới nhất về điện thoại.

Các bản cập nhật đều được ký cùng một khóa (`keystore/release.jks`), nên cài bản mới đè lên bản cũ được mà **không mất cài đặt và quyền**. Đừng xóa hay đổi file khóa này.

## Cài đặt trên máy Samsung (người sáng mắt làm một lần, khoảng 15 phút)

1. **Tắt tạm "Chặn tự động" (Auto Blocker)** nếu máy báo không cho cài: Cài đặt → Bảo mật và quyền riêng tư → Chặn tự động → Tắt. Cài xong có thể bật lại.
2. Mở file APK để cài.
3. Mở biểu tượng **"Cài đặt Trợ Lý"** (biểu tượng thứ hai của app). Bấm **"Sửa"** ở từng mục ❌ cho tới khi tất cả là ✅:
   - Đặt làm **ứng dụng điện thoại mặc định** (bắt buộc để báo cuộc gọi sim)
   - Cấp quyền micro, danh bạ, gọi điện, nhật ký cuộc gọi, thông báo
   - Bật **quyền đọc thông báo** (đọc tin nhắn, báo cuộc gọi Zalo/WhatsApp)
   - Bật **Hỗ trợ tiếp cận / Trợ năng** → "Trợ Lý Nghe Gọi" (bấm loa trong Zalo/WhatsApp)
   - Nếu Samsung báo **"Cài đặt bị hạn chế"**: Cài đặt → Ứng dụng → Trợ Lý Nghe Gọi → bấm ⋮ góc trên → **"Cho phép cài đặt hạn chế"**, rồi bật lại quyền.
   - Hiển thị trên ứng dụng khác, không giới hạn pin, hiện toàn màn hình khi khóa máy
   - Tắt **"Tạm dừng hoạt động nếu không dùng"** (để Android không tự thu hồi quyền)
   - **Giọng đọc tiếng Việt**: nếu ❌, cài/tải dữ liệu giọng tiếng Việt cho "Dịch vụ giọng nói của Google" (Speech Services by Google) để dùng offline.
4. Cài **nút nguồn** để mở lệnh giọng nói (chọn một trong hai):
   - **Nhấn giữ nút nguồn** (khuyên dùng): Cài đặt → Tính năng nâng cao → Phím sườn → Nhấn và giữ → **Trợ lý kỹ thuật số** → chọn **Trợ Lý Nghe Gọi** (nằm trong mục "Ứng dụng khác"). Nếu máy hỏi, chọn đặt làm ứng dụng trợ lý mặc định.
   - **Nhấn đúp nút nguồn**: Cài đặt → Tính năng nâng cao → Phím sườn → Nhấn hai lần → Mở ứng dụng → **Trợ Lý Nghe Gọi**.
5. Pin: Cài đặt → Pin → Giới hạn sử dụng nền → **Ứng dụng không bao giờ ngủ** → thêm Trợ Lý Nghe Gọi, **Zalo**, **WhatsApp**. Đặt Pin của 3 app này là **"Không hạn chế"**.
6. Trong "Cài đặt Trợ Lý": chọn **độ to giọng đọc** (mặc định mức 3 – rất to), rồi bấm **"Thử giọng đọc"**, **"Giả lập cuộc gọi đến"** và **"Thử nhận lệnh giọng nói"**. Nên kéo cả thanh **Nhạc chuông** lên tối đa trong Cài đặt → Âm thanh.
7. Nhập danh bạ với tên dễ đọc (ví dụ "Mẹ", "Anh Hưng") – tên tiếng Việt thường được nhận dạng tốt hơn tên tiếng Anh hay biệt danh có ký hiệu.

### Danh sách kiểm tra trước khi trao máy

- [ ] Gọi sim đến khi **màn hình khóa** → nghe đọc tên, nói "nghe" → nhấc máy, loa ngoài bật
- [ ] Gọi sim đến, nói "không" → từ chối
- [ ] Gọi sim đến, bấm phím tăng âm lượng → nhấc máy
- [ ] Gọi từ số **không có trong danh bạ** → đọc từng chữ số
- [ ] Để nhỡ một cuộc gọi → nghe báo cuộc gọi nhỡ; bấm đúp nút nguồn, nói "gọi lại"
- [ ] Gọi **Zalo** đến (màn hình khóa và mở khóa) → nói "nghe" → nhấc máy và bật loa
- [ ] Gọi **WhatsApp** đến → như trên
- [ ] Nhắn tin Zalo, WhatsApp, SMS → nghe đọc nội dung
- [ ] Bấm đúp nút nguồn → "gọi Mẹ" → "đúng" → gọi đi, loa ngoài bật
- [ ] **Khởi động lại máy** rồi lặp lại 1–2 mục trên (app không cần mở lại)
- [ ] Vuốt tắt hết app ở đa nhiệm rồi thử lại

## Người khác dùng máy để gọi (khách đến thăm, người hỗ trợ)

Khi Trợ Lý là ứng dụng điện thoại mặc định, app **Điện thoại của Samsung sẽ không cho gọi** (bấm "Lúc khác" là nó tự đóng). Thay vào đó dùng biểu tượng **"Điện thoại"** (nền xanh dương) của Trợ Lý:

- **Bàn phím**: bấm số → **GỌI**. Bên dưới số hiện tên nếu có trong danh bạ. Nút **Lưu số** để thêm vào danh bạ.
- **Gần đây**: nhật ký cuộc gọi (gọi đến, gọi đi, gọi nhỡ). Nút **GỌI** để gọi lại; chạm vào dòng để đưa số lên bàn phím.
- **Danh bạ**: ô tìm kiếm (gõ không dấu cũng được, ví dụ "nguyen"), nút **+ Thêm**.
- Trong lúc gọi có nút **BÀN PHÍM** để bấm phím khi gọi tổng đài.
- App **Danh bạ** của Samsung vẫn dùng bình thường; bấm gọi từ đó cuộc gọi vẫn đi.
- Cuộc gọi nhỡ hiện thông báo trên thanh trạng thái; chạm vào để mở danh sách Gần đây.

Nên **gỡ biểu tượng Điện thoại của Samsung khỏi màn hình chính** và đặt biểu tượng "Điện thoại" của Trợ Lý vào đúng chỗ đó, để khách cầm máy là thấy ngay.

## Độ to

- Giọng đọc phát qua thanh âm lượng **"Chuông báo"**. App tự đặt thanh này về mức đã chọn (mặc định 100%) trước mỗi lần đọc, nên lỡ kéo nhỏ cũng không sao.
- Ngoài ra app **khuếch đại giọng đọc bằng phần mềm** (chuẩn hóa + nén động), đo trên giọng tiếng Việt: mức 2 / 3 / 4 to hơn khoảng +4 / +6 / +8 dB. Mức 4 có thể hơi rè.
- Lệnh **"to lên"**: tăng âm lượng tới 100% rồi tăng mức khuếch đại. **"Nhỏ xuống"**: ngược lại.
- Khi nghe máy, app đưa **âm lượng cuộc gọi lên tối đa** (sim và Zalo/WhatsApp).
- Loa điện thoại có giới hạn vật lý. Nếu người dùng nghe rất kém, nên dùng thêm loa Bluetooth hoặc tai nghe/máy trợ thính Bluetooth.

## App có cần luôn mở không?

Không. App không chạy như app thường mà dựa vào 3 dịch vụ do Android quản lý:

- **Ứng dụng điện thoại mặc định**: Android tự đánh thức app mỗi khi có cuộc gọi sim.
- **Quyền đọc thông báo** và **Trợ năng**: Android giữ chạy thường trực và tự bật lại nếu bị tắt.

Ngoài ra app có "người gác" chạy khoảng 15 phút một lần và sau khi bật máy, để xin kết nối lại dịch vụ nếu bị Samsung ngắt và **đọc to cảnh báo** nếu mất quyền quan trọng (tối đa 6 tiếng một lần). Vuốt tắt app, "Đóng tất cả", khởi động lại máy đều không ảnh hưởng. Chỉ cần **không bấm "Buộc dừng"** trong Cài đặt → Ứng dụng.

## Giới hạn đã biết

- Bật loa trong Zalo/WhatsApp dựa vào việc tìm nút "Loa" trên màn hình cuộc gọi. Nếu Zalo/WhatsApp đổi giao diện sau một lần cập nhật, có thể không bật được loa (app vẫn nghe máy được và sẽ nói "hãy áp máy lên tai"). Có thể tắt tự cập nhật Zalo/WhatsApp trong Play Store.
- Khi đang trong một cuộc gọi mà có cuộc gọi chờ, app chỉ đọc tên người gọi (micro đang bận).
- Nhận dạng giọng nói offline chỉ hiểu các lệnh cố định và tên có trong danh bạ, không hiểu câu tự do.
- Khi máy để chế độ rung/im lặng: app **vẫn đọc tên người gọi và tin nhắn** (người khiếm thị không nhìn thấy máy đang im lặng). Muốn yên tĩnh ban đêm thì bật "giờ yên tĩnh" trong Cài đặt Trợ Lý.
- Nhận dạng giọng nói chỉ chấp nhận câu ngắn gồm toàn từ của một lệnh ("nghe", "nghe máy", "không", "từ chối"...). Tiếng ồn, tiếng TV, người xung quanh nói chuyện bị bỏ qua.

## Cấu trúc code

```
app/src/main/java/vn/blindsupport/app/
├── App.kt, Watchdog.kt          Khởi động, "người gác", nhận sự kiện bật máy
├── core/                        Giọng đọc (TTS), nhận dạng giọng nói (Vosk), âm lượng, danh bạ, cài đặt
├── call/                        Ứng dụng điện thoại mặc định: InCallService, màn hình cuộc gọi
├── voip/                        Cuộc gọi Zalo / WhatsApp
├── notify/                      Đọc thông báo: tin nhắn, cuộc gọi Zalo, pin
├── a11y/                        Trợ năng: bấm nút Loa/Trả lời trong Zalo, WhatsApp
└── ui/                          Màn hình lệnh giọng nói, cài đặt, bàn phím gọi
```

Model nhận dạng giọng nói tiếng Việt (`vosk-model-small-vn-0.4`, khoảng 32MB) được tự tải về khi build lần đầu, không lưu trong repo.

### Build trên máy tính

Mở thư mục repo bằng Android Studio rồi chọn **Build → Build APK(s)**, hoặc chạy `./gradlew assembleRelease`. File APK nằm ở `app/build/outputs/apk/release/`.
