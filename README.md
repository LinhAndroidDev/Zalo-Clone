# Zalo Clone

Ứng dụng nhắn tin trên Android (Kotlin), lấy cảm hứng từ Zalo: danh sách chat, hội thoại realtime, danh bạ, kết bạn, nhật ký (feed), khám phá, cài đặt và thông báo đẩy (FCM).

## Tính năng chính

### Xác thực & tài khoản

- Đăng ký / đăng nhập: xác thực qua **Firestore** (truy vấn `users` theo email + mật khẩu đã lưu).
- **Firebase Auth (Phone)**: dùng trong luồng OTP (ví dụ `ReceiveOTPFragment`).
- Lưu phiên cục bộ (SharedPreferences) qua Hilt / repository.

### Chat

- Tin nhắn **realtime** qua Firestore (`messages/{roomId}/chats`).
- Tin văn, ảnh, một ảnh, ghi âm; cảm xúc (reaction) trên tin.
- Trạng thái đã xem, đếm chưa đọc, chỉ báo đang gõ.
- **Chat head** (dịch vụ nổi) khi có thông báo.
- Gửi thông báo FCM tới người nhận: đọc token một lần từ collection `Tokens/{userId}` rồi gọi **FCM HTTP v1** (Retrofit), tránh gửi trùng khi token thay đổi và không gửi nếu token rỗng.

### Danh bạ & bạn bè

- Danh bạ nhóm theo chữ cái, sticky header, fast scroll.
- Lời mời kết bạn (gửi / nhận / chấp nhận / từ chối / hủy), đồng bộ subcollection `friends` trên `users`.

### Nhật ký (Diary)

- Đăng bài, ảnh, link preview; thích và bình luận; feed theo bạn bè (Firestore + listener).

### Khác

- Trạng thái (Status): đăng / xem media.
- Tìm kiếm người dùng, lịch sử tìm kiếm.
- QR (quét mã), một số màn hồ sơ / xem trước ảnh.
- **Cloudinary**: upload ảnh / video / audio chat (unsigned preset trong code — nên tách ra cấu hình riêng khi fork).

## Ảnh màn hình

<div style="display: flex; justify-content: center;">
  <img src="https://github.com/user-attachments/assets/dace1fce-7f7c-4732-9b94-10975e807bd1" alt="Screen Home" width="250"/>
  <img src="https://github.com/user-attachments/assets/c04bf373-8642-43d0-97ca-68d387c24081" alt="Screen Message" width="250"/>
</div>

## Cấu trúc thư mục (rút gọn)

```
app/src/main/java/com/example/messageapp/
├── adapter/          # RecyclerView adapters
├── argument/         # Safe Args / navigation args
├── base/             # BaseFragment, BaseViewModel, CoreInterface
├── bottom_sheet/     # Bottom sheets (ảnh, sticker, ngôn ngữ, …)
├── broadcast/        # Ví dụ xử lý reply notification
├── custom/           # Custom views
├── dialog/           # Dialog fragments
├── di/               # Hilt modules (AppModule)
├── fragment/         # Các màn Fragment (Home, Chat, Diary, …)
├── helper/           # Hằng số, layout helper
├── library/          # Thành phần tái sử dụng (OTP, audio wave, fast scroll)
├── model/            # Data class / Firestore models
├── remote/           # Retrofit ApiClient, ApiService, request DTOs
├── service/          # FCM service, ChatHeadService
├── utils/            # FireBaseInstance, Cloudinary, AccessToken, …
├── viewmodel/
├── MainActivity.kt
├── MyApplication.kt
├── PersonalActivity.kt, PreviewPhotoActivity.kt, …
└── res/
```

## Công nghệ & phiên bản

| Thành phần | Ghi chú |
|------------|---------|
| Ngôn ngữ | Kotlin **1.9.22** |
| Android Gradle Plugin | **8.2.1** |
| Gradle Wrapper | **8.2** |
| `minSdk` / `targetSdk` | **24** / **34** (`compileSdk` 34) |
| UI | Material, ViewBinding + Data Binding, Navigation Component |
| DI | **Hilt** 2.48 |
| Async | Kotlin **Coroutines**, Flow |
| Backend phía app | **Firebase**: Firestore, Cloud Messaging, Analytics, Crashlytics, Installations, Auth (phone) |
| Media upload | **Cloudinary** (OkHttp multipart) |
| HTTP client | **Retrofit** + Gson, OkHttp |
| Khác | Glide, Media3 ExoPlayer, ZXing / Code scanner, PhotoView |

> Lưu ý: README trước đây ghi “Realtime Database / Storage” — trong repo hiện tại **không** dùng Realtime Database hay Firebase Storage cho chat; dữ liệu chat và user chủ yếu nằm trên **Firestore**, file media đẩy lên **Cloudinary**.

## Một số Fragment / luồng UI

- **Splash / Intro / Login / Register / OTP** — vào app và xác thực.
- **HomeFragment** — danh sách hội thoại, gợi ý kết bạn.
- **ChatFragment** — hội thoại 1-1.
- **PhoneBookFragment**, **SearchFragment**, **FriendRequestFragment**.
- **PersonalFragment**, **DiaryFragment**, **DiscoverFragment**, **SettingFragment**, **StatusFragment**, **ScanQRFragment**.

## Chạy dự án

1. **Clone** repository.
2. Mở bằng **Android Studio** (khuyến nghị bản tương thích AGP 8.2 / JDK 17).
3. Thêm **`google-services.json`** của Firebase vào `app/` (từ Firebase Console → Project settings).
4. **Đồng bộ Gradle** và Run trên thiết bị / emulator API 24+.

### Firebase & biến môi trường

- Bật **Firestore**, **Cloud Messaging**, **Authentication** (Phone nếu dùng OTP), và các dịch vụ bạn cần trên cùng project với `google-services.json`.
- Collection/token thông báo: document **`Tokens/{userId}`** chứa FCM token thiết bị (app lưu khi đăng nhập / refresh token).

### Cloudinary (upload ảnh / video / audio)

Trong `CloudinaryManager.kt` đang có `CLOUD_NAME` và `UPLOAD_PRESET`. Khi fork sang project riêng, hãy tạo preset unsigned (hoặc signed) trên Cloudinary và cập nhật giá trị — **không** commit secret ký server-side lên Git công khai.

## Thông báo đẩy (FCM HTTP v1)

App gửi tin nhắn FCM qua endpoint:

`https://fcm.googleapis.com/v1/projects/{PROJECT_ID}/messages:send`

Cần **OAuth2 access token** của service account có quyền gửi tin (scope `https://www.googleapis.com/auth/firebase.messaging`).

### Các bước cấu hình (khuyến nghị)

1. Trên Firebase Console: **Project settings → Service accounts → Generate new private key** (file JSON).
2. **Không** dán toàn bộ private key vào README hay commit công khai. Với bản build local có thể:
   - đọc JSON từ file chỉ tồn tại trên máy (đã thêm vào `.gitignore`), hoặc
   - dùng backend proxy gửi FCM (an toàn hơn cho production).
3. Cập nhật **`AccessToken.kt`**: nạp credential từ JSON hợp lệ của project bạn (logic tương tự `GoogleCredentials.fromStream(...).createScoped(...).refresh()`).
4. Cập nhật **`ApiService.kt`**: đường dẫn `@POST("{projectId}/messages:send")` khớp **`project_id`** trong JSON.
5. Phụ thuộc Gradle: `com.google.firebase:firebase-messaging`, `com.google.auth:google-auth-library-oauth2-http` (đã khai trong `app/build.gradle.kts`).

### HTTP v1 payload (tham khảo)

Tài liệu Google: [Migrate to FCM HTTP v1](https://firebase.google.com/docs/cloud-messaging/migrate-v1).

Ví dụ khung JSON (token do server/app điền):

```json
{
  "message": {
    "token": "<FCM_DEVICE_TOKEN>",
    "notification": {
      "title": "Tiêu đề",
      "body": "Nội dung"
    }
  }
}
```

### Cảnh báo bảo mật

Nếu service account JSON hoặc private key từng bị đưa vào repo / README công khai, hãy **thu hồi và tạo lại key** trên Google Cloud Console và cập nhật ứng dụng.

## Đóng góp

Issues và pull request đều được hoan nghênh.

## Giấy phép

README gốc tham chiếu MIT; nếu chưa có file `LICENSE` trong repo, hãy bổ sung file license thống nhất với ý định phát hành của bạn.
