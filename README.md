# Trợ Lý Nghe Gọi

App Android giúp người khiếm thị nghe gọi điện thoại và nghe tin nhắn bằng giọng nói tiếng Việt. Toàn bộ nhận dạng giọng nói và giọng đọc chạy **offline**, không cần internet (riêng Zalo/WhatsApp thì bản thân chúng cần mạng).

## Tính năng

| Tình huống | App làm gì |
|---|---|
| Có cuộc gọi sim đến | Chuông máy reo trước **6 giây** (chỉnh được 0/3/6/10 giây), máy **rung liên tục**, rồi app nói *"Có điện thoại! Cuộc gọi từ …. Bạn có muốn nghe không?"* (tên trong danh bạ, hoặc đọc từng số). Nói **"nghe"** → nhấc máy và bật loa ngoài. Nói **"không"** → từ chối. **Bấm phím bất kỳ ở cạnh máy** (tăng/giảm âm lượng, nguồn) = **nghe máy** – Android giữ phím lúc đổ chuông và không cho biết phím nào, nên mọi phím đều là nghe. Trên màn hình phải **giữ** nút mới từ chối được. Trong lúc app chờ câu trả lời, chuông thật được tắt để không lọt vào micro; giữa các lần hỏi app tự phát tiếng chuông to (kể cả khi máy để im lặng). |
| Có cuộc gọi Zalo / WhatsApp | Chuông Zalo/WhatsApp reo khoảng 3 giây rồi app mới hỏi (Zalo đổ chuông ngắn hơn cuộc gọi sim). Bấm phím bất kỳ ở cạnh máy cũng nghe máy. Còn lại như trên, dùng nút Trả lời/Từ chối trong thông báo của Zalo/WhatsApp; sau khi nghe máy, app tự bấm nút **Loa** (cần bật Trợ năng). Nếu không bấm được loa, app nói *"hãy áp máy lên tai"*. Vì chuông Zalo/WhatsApp không tắt được, lúc này app chỉ nhận **"nghe"**, "nghe máy", "bắt máy", "trả lời" (không nhận "có", "ừ", "vâng" – tiếng chuông hay bị nghe nhầm thành các từ này). |
| Cuộc gọi nhỡ (sim, Zalo, WhatsApp) | Đọc to: *"Bạn vừa nhỡ cuộc gọi từ …"*, luôn kèm tên (danh bạ máy, sim, Zalo/WhatsApp) hoặc số điện thoại. Zalo báo lại cả cuộc gọi sim nhỡ của người đã kết bạn: app vẫn đọc đúng tên/số và nói là cuộc gọi sim. |
| Tin nhắn Zalo, WhatsApp, SMS | Đọc to người gửi và nội dung. Tin nhóm mặc định chỉ lưu lại (riêng với tin cá nhân), không đọc to; nói **"tin nhóm"** để nghe. Đang gọi điện thì không đọc, lưu lại để nghe sau. Máy để im lặng **vẫn đọc**. **Giờ yên tĩnh** (22h–6h) **mặc định bật**: ban đêm không đọc to tin nhắn (vẫn báo cuộc gọi), hết giờ yên tĩnh app nhắc số tin mới một lần. |
| **Giữ phím giảm âm lượng ~1,5 giây** (mọi hãng máy) hoặc nút nguồn (Samsung) | Máy rung nhẹ rồi mở chế độ nghe lệnh (xem bảng lệnh bên dưới). Bấm nhanh phím giảm âm lượng vẫn giảm âm lượng bình thường; khi app đang đọc (ví dụ đọc tin nhắn) thì bấm nhanh để **dừng đọc**. Màn hình đang tắt app vẫn thử nhận phím và tự bật sáng màn hình; nếu máy không rung thì bấm nút nguồn cho sáng rồi giữ lại. Cần bật Trợ năng – nếu Trợ năng bị tắt hoặc bị hệ thống ngắt, app **đọc to cảnh báo** ngay ở lần kiểm tra kế tiếp. |
| Pin | Báo pin yếu (20%, 10%, 5%), cắm sạc, rút sạc, sạc đầy. |

### Lệnh giọng nói (sau khi giữ phím giảm âm lượng và nghe tiếng bíp)

Mọi câu hỏi của app đều kết thúc bằng *"… sau tiếng bíp"*, rồi kêu một tiếng bíp to, dài khoảng 2 giây. Nói sau khi tiếng bíp tắt.

- **"Gọi Mẹ"**, **"Gọi chị Trang"**, **"Gọi Hưng"** – gọi theo tên trong danh bạ, nói cả tên hoặc một phần tên (không cần đúng thứ tự, không phân biệt dấu). Đúng một người khớp: app hỏi *"Bạn muốn gọi cho …, đúng không?"*. Nhiều người khớp (tối đa 6): app đọc *"Số 1: …. Số 2: …"* rồi hỏi gọi ai – nói **số thứ tự** ("hai", "số hai", "người thứ hai") là gọi ngay; nói "hủy" để thôi. Nhiều hơn 6 người: app xin nói thêm tên.
- **"Gọi"** kèm số điện thoại, ví dụ **"gọi không ba bốn năm…"** (có hoặc không có chữ "số"), nói liền một hơi hoặc ngập ngừng đều được. App đọc lại: *"Bạn muốn gọi số …, đúng không? Nói đúng để thực hiện cuộc gọi, nói không để hủy."*
- Chỉ nói **"gọi"** rồi dừng: app hỏi gọi ai.
- **"Gọi lại"** – gọi lại người vừa gọi đến / gọi nhỡ
- **"Đọc tin nhắn"** – đọc các tin cá nhân chưa nghe (bấm nhanh phím giảm âm lượng để dừng)
- **"Tin nhóm"** – đọc tối đa 10 tin nhóm mới nhất
- **"Cuộc gọi nhỡ"** – đọc danh sách cuộc gọi nhỡ trong 24 giờ
- **"Mấy giờ"**, **"Pin"**
- **"Danh bạ"** – đọc tên trong danh bạ
- **"To lên" / "Nhỏ xuống"** – chỉnh độ to giọng đọc
- **"Nói chậm" / "Nói nhanh"** – chỉnh tốc độ đọc
- **"Hướng dẫn"** – nghe lại danh sách lệnh

Nghe lệnh dùng **bộ nhận dạng giọng nói Google** có sẵn trên máy (nghe tên người và chữ số chính xác hơn nhiều); khi mất mạng hoặc máy không có thì tự chuyển sang bộ nhận dạng offline (kém chính xác hơn). Câu hỏi "nghe hay không" khi có cuộc gọi đến vẫn dùng bộ offline.

App nghe **trọn câu** (chờ bạn im lặng khoảng 1,5 giây) rồi mới xử lý, nên ngập ngừng giữa câu không sao. Từ đệm như "à", "ừ", "đi", "nhé", "ạ" được bỏ qua. Câu dài hơn lệnh quá 2 chữ (thường là tiếng TV) bị bỏ qua. Chữ app nghe được hiện trên màn hình (cho người hỗ trợ xem) và được ghi vào nhật ký trong Cài đặt Trợ Lý.

Khi app hỏi lại *"Gọi cho …, phải không?"*: nói **"đúng"**, "đúng rồi", "phải", "vâng", "có", "gọi đi" để gọi; **"không"**, "sai", "thôi", "hủy" để hủy. Câu dài hoặc lẫn từ khác không được tính – im lặng thì app tự hủy.

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
   - **Giọng đọc tiếng Việt** và **"đã tải về máy"**: nếu ❌, cài/tải dữ liệu giọng tiếng Việt cho "Dịch vụ giọng nói của Google" (Speech Services by Google) để dùng offline. Nếu giọng đọc bị lỗi (ví dụ mất mạng mà giọng chưa tải về), app phát **3 tiếng tít ngắn** thay vì im lặng.
   - Nếu màn hình hiện danh sách **"tên trong danh bạ KHÔNG gọi được bằng giọng nói"**: đổi các tên đó trong danh bạ sang chữ tiếng Việt đơn giản.
4. Cài **nút nguồn** để mở lệnh giọng nói (chọn một trong hai):
   - **Nhấn giữ nút nguồn** (khuyên dùng): Cài đặt → Tính năng nâng cao → Phím sườn → Nhấn và giữ → **Trợ lý kỹ thuật số** → chọn **Trợ Lý Nghe Gọi** (nằm trong mục "Ứng dụng khác"). Nếu máy hỏi, chọn đặt làm ứng dụng trợ lý mặc định.
   - **Nhấn đúp nút nguồn**: Cài đặt → Tính năng nâng cao → Phím sườn → Nhấn hai lần → Mở ứng dụng → **Trợ Lý Nghe Gọi**.
5. Pin: Cài đặt → Pin → Giới hạn sử dụng nền → **Ứng dụng không bao giờ ngủ** → thêm Trợ Lý Nghe Gọi, **Zalo**, **WhatsApp**. Đặt Pin của 3 app này là **"Không hạn chế"**.
6. Trong "Cài đặt Trợ Lý": chọn **độ to giọng đọc** (mặc định mức 3 – rất to), rồi bấm **"Thử giọng đọc"**, **"Giả lập cuộc gọi đến"** và **"Thử nhận lệnh giọng nói"**. Nên kéo cả thanh **Nhạc chuông** lên tối đa trong Cài đặt → Âm thanh.
7. Nhập danh bạ với tên dễ đọc (ví dụ "Mẹ", "Anh Hưng") – tên tiếng Việt thường được nhận dạng tốt hơn tên tiếng Anh hay biệt danh có ký hiệu.

### Danh sách kiểm tra trước khi trao máy

- [ ] Gọi sim đến khi **màn hình khóa** → nghe đọc tên, nói "nghe" → nhấc máy, loa ngoài bật
- [ ] Gọi sim đến, nói "không" → từ chối
- [ ] Gọi sim đến, bấm phím bất kỳ ở cạnh máy → nhấc máy
- [ ] Gọi từ số **không có trong danh bạ** → đọc từng chữ số
- [ ] Để nhỡ một cuộc gọi → nghe báo cuộc gọi nhỡ; bấm đúp nút nguồn, nói "gọi lại"
- [ ] Gọi **Zalo** đến (màn hình khóa và mở khóa) → nói "nghe" → nhấc máy và bật loa
- [ ] Gọi **WhatsApp** đến → như trên
- [ ] Nhắn tin Zalo, WhatsApp, SMS → nghe đọc nội dung
- [ ] Bấm đúp nút nguồn → "gọi Mẹ" → "đúng" → gọi đi, loa ngoài bật
- [ ] **Khởi động lại máy** rồi lặp lại 1–2 mục trên (app không cần mở lại)
- [ ] Vuốt tắt hết app ở đa nhiệm rồi thử lại

## Cài trên máy Oppo (ColorOS)

ColorOS tắt app chạy nền rất mạnh. Làm cho **cả Trợ Lý Nghe Gọi, Zalo và WhatsApp**:

1. Cài đặt → Quản lý ứng dụng → chọn app → bật **Cho phép tự khởi chạy** và **Cho phép ứng dụng khác khởi chạy**.
2. Cùng màn hình → **Mức sử dụng pin** → bật **Cho phép hoạt động nền**, tắt tối ưu hóa pin.
3. Cùng màn hình → Quyền → bật **Hiển thị trên màn hình khóa**, **Cửa sổ bật lên / mở giao diện khi chạy nền**, **Hiển thị trên ứng dụng khác**.
4. Mở danh sách app gần đây → **khóa** thẻ Trợ Lý Nghe Gọi.
5. Nút nguồn của Oppo chỉ mở được Camera / Trợ lý Google, nên dùng **giữ phím giảm âm lượng** để mở lệnh giọng nói.
6. Giọng đọc tiếng Việt: cần wifi một lần để tải giọng Google tiếng Việt (mục "Giọng đọc tiếng Việt" trong Cài đặt Trợ Lý).

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

Ngoài ra app có "người gác" chạy khoảng 15 phút một lần và sau khi bật máy, để xin kết nối lại dịch vụ nếu bị Samsung ngắt và **đọc to cảnh báo** nếu mất quyền quan trọng hoặc Trợ năng / dịch vụ đọc thông báo bị ngắt (vấn đề mới báo ngay, vấn đề cũ nhắc lại mỗi 6 tiếng). Vuốt tắt app, "Đóng tất cả", khởi động lại máy đều không ảnh hưởng. Chỉ cần **không bấm "Buộc dừng"** trong Cài đặt → Ứng dụng.

## Giới hạn đã biết

- Bật loa trong Zalo/WhatsApp dựa vào việc tìm nút "Loa" trên màn hình cuộc gọi. Nếu Zalo/WhatsApp đổi giao diện sau một lần cập nhật, có thể không bật được loa (app vẫn nghe máy được và sẽ nói "hãy áp máy lên tai"). Có thể tắt tự cập nhật Zalo/WhatsApp trong Play Store.
- Khi đang trong một cuộc gọi mà có cuộc gọi chờ, app chỉ đọc tên người gọi (micro đang bận).
- Nhận dạng giọng nói offline chỉ hiểu các lệnh cố định và tên có trong danh bạ, không hiểu câu tự do.
- Khi máy để chế độ rung/im lặng: app **vẫn đọc tên người gọi và tin nhắn** (người khiếm thị không nhìn thấy máy đang im lặng). Giờ yên tĩnh ban đêm mặc định bật; có thể tắt trong Cài đặt Trợ Lý.
- Âm lượng "Chuông báo" chỉ được kéo lên trong lúc app đang đọc, đọc xong trả lại mức cũ (báo thức đồng hồ không bị to lên).
- Nhận dạng giọng nói chỉ chấp nhận câu ngắn gồm toàn từ của một lệnh ("nghe", "nghe máy", "không", "từ chối"...), cả ở câu hỏi nghe máy, bước xác nhận "đúng/không" và lệnh giọng nói. Tiếng ồn, tiếng TV, người xung quanh nói chuyện bị bỏ qua.
- Tin nhắn thoại Zalo chỉ được báo "một tin nhắn thoại", chưa nghe được nội dung. Chưa gọi Zalo/WhatsApp đi bằng giọng nói được (chỉ gọi sim).

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
