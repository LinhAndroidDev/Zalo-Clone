# Zalo Clone

Ứng dụng nhắn tin trên Android (Kotlin), lấy cảm hứng từ Zalo: danh sách chat 1-1 và nhóm, hội thoại realtime, danh bạ, kết bạn, nhật ký (feed), khám phá, cài đặt và thông báo đẩy (FCM).

> **Nhánh `create-group`:** mở rộng so với `main` với chat nhóm, trạng thái online, mention, trả lời tin, cảm xúc (reaction) và hiệu ứng burst. README này mô tả đầy đủ tính năng trên nhánh `create-group`.

## Tính năng chính

### Xác thực & tài khoản

- Đăng ký / đăng nhập: xác thực qua **Firestore** (truy vấn `users` theo email + mật khẩu đã lưu).
- **Firebase Auth (Phone)**: dùng trong luồng OTP (ví dụ `ReceiveOTPFragment`).
- Lưu phiên cục bộ qua Hilt / `SessionRepository` (`:data` module).
- Sau khi chấp nhận lời mời kết bạn, đồng bộ danh sách bạn và hội thoại inbox.

### Chat 1-1

- Tin nhắn **realtime** qua Firestore (`messages/{roomId}/chats`).
- Tin văn, ảnh, một ảnh, ghi âm; **cảm xúc (reaction)** trên tin (yêu thích, thích, cười, khóc, giận).
- **Trả lời tin (reply)**: quote tin gốc trên bubble, preview khi soạn, tap quote để cuộn tới tin gốc và **highlight** tạm thời.
- **Hiệu ứng burst cảm xúc**: icon mini bay ra từ chip reaction khi người gửi thả cảm xúc; người nhận thấy hiệu ứng tương tự khi đang mở `ChatFragment` (phát hiện qua snapshot `messages`).
- Trạng thái đã xem, đếm chưa đọc, chỉ báo đang gõ.
- **Trạng thái online / last seen** của bạn bè (Firebase Realtime Database qua `PresenceManager`).
- **Chat head** (dịch vụ nổi) khi có thông báo.
- Gửi thông báo FCM tới người nhận: đọc token từ `Tokens/{userId}` rồi gọi **FCM HTTP v1** (Retrofit).

### Chat nhóm

- **Tạo nhóm** từ menu Home (`CreateGroupFragment`): chọn tên, chọn thành viên từ danh sách bạn (tối thiểu 1), tạo xong mở thẳng màn chat.
- Tin nhắn chung trong phòng `messages/{groupId}/chats`; metadata nhóm tại `groups/{groupId}`.
- **Avatar nhóm ghép** từ avatar thành viên (`GroupAvatarView`, `GroupAvatarLoader`); sắp xếp ổn định khi nhóm 2 người.
- **Tin chào mừng** tự động khi tạo nhóm; đồng bộ hàng inbox `Conversation{userId}/{groupId}` cho mọi thành viên.
- **Mention (@)**: gõ `@` để nhắc thành viên hoặc `@All` trong nhóm; gửi kèm metadata `mentions` trên tin; thông báo FCM riêng khi bị nhắc.
- **Đang gõ** trong nhóm: lưu `typing` / `typingUserId` trên document nhóm.
- **Chưa đọc & đã xem**: đếm tin chưa đọc; hiển thị ai đã xem tin cuối (`memberRead` trên `groups/{groupId}`).
- Hỗ trợ reply, reaction, burst cảm xúc và long-press menu giống chat 1-1; hiển thị tên người gửi trên bubble nhận.

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

## Kiến trúc (Clean Architecture — multi-module)

```
:domain/          # Entity thuần Kotlin, repository interfaces, use cases
:domain/src/main/kotlin/com/example/messageapp/domain/
  model/          # Message, Conversation, User, …
  repository/     # ChatRepository, SessionRepository, …
  usecase/        # ObserveMessagesUseCase, SendMessageUseCase, …
  chat/           # EmotionReactionDetector, MentionParser

:data/            # Firebase, Retrofit, Cloudinary, repository impl
:data/src/main/java/com/example/messageapp/data/
  legacy/         # FireBaseInstance (legacy, đang tách dần)
  repository/     # ChatRepositoryImpl, …
  mapper/         # EntityMapper (Firestore ↔ domain)
  di/             # Hilt RepositoryModule

:app/             # UI — Fragment, ViewModel, Adapter, mapper UI
  mapper/         # ChatUiMapper (domain ↔ Parcelable UI model)
```

**Luồng phụ thuộc:** `app → domain ← data`

**Đã refactor chính:** `ChatFragmentViewModel`, `CreateGroupViewModel`, `HomeViewModel`, `LoginFragmentViewModel` dùng use case + repository. Legacy `FireBaseInstance` đã chuyển sang `:data` (app truy cập qua `utils/LegacyCompat.kt` cho code chưa migrate).

## Cấu trúc thư mục app (rút gọn)

```
app/src/main/java/com/example/messageapp/
├── adapter/          # RecyclerView adapters (Chat, ListChat, Mention, CreateGroupMember, …)
├── argument/         # Safe Args / navigation args
├── base/             # BaseFragment, BaseViewModel, CoreInterface
├── bottom_sheet/     # Bottom sheets (ảnh, sticker, ngôn ngữ, …)
├── broadcast/        # NotificationReply — trả lời từ notification (kèm replyTo)
├── custom/           # Custom views (GroupAvatarView, CustomHeaderView, …)
├── dialog/           # Dialog fragments
├── di/               # Hilt modules (AppModule)
├── fragment/         # Home, Chat, CreateGroup, Diary, …
├── helper/           # Hằng số, layout helper
├── library/          # Thành phần tái sử dụng (OTP, audio wave, fast scroll)
├── model/            # Message, Conversation, GroupChat, UserPresence, Emotion, …
├── remote/           # Retrofit ApiClient, ApiService, request DTOs
├── service/          # FCM ReceiverMessageService, ChatHeadService
├── utils/
│   ├── FireBaseInstance.kt      # Firestore: chat, nhóm, friend, diary, FCM trigger
│   ├── PresenceManager.kt       # Online / last seen (Realtime Database)
│   ├── MentionHelper.kt         # Parse & gợi ý @mention trong nhóm
│   ├── MessageReplyHelper.kt    # Bind quote reply, preview inbox/notification
│   ├── EmotionBurstEffect.kt    # Hiệu ứng particle khi thả cảm xúc
│   ├── EmotionReactionDetector.kt # Phát hiện reaction remote cho burst
│   ├── GroupAvatarLoader.kt     # Tải avatar thành viên cho avatar nhóm
│   └── CloudinaryManager.kt, DateUtils.kt, …
├── viewmodel/
├── MainActivity.kt
├── MyApplication.kt
├── PersonalActivity.kt, PreviewPhotoActivity.kt, …
└── res/
    ├── layout/       # fragment_create_group, layout_message_reply_quote, view_group_avatar, …
    └── navigation/   # navigation_main.xml (Home → CreateGroup → Chat)
```

## Mô hình dữ liệu (Firestore / RTDB)

| Collection / path | Mô tả |
|-------------------|--------|
| `users/{userId}` | Hồ sơ, subcollection `friends` |
| `Conversation{userId}/{roomId}` | Hàng inbox; `isGroup = true` cho nhóm; `friendId` = `groupId` (UUID) |
| `groups/{groupId}` | Tên nhóm, `memberIds`, `typing`, `memberRead/{userId}` |
| `messages/{roomId}/chats/{time}` | Tin chat; field `emotion`, `replyTo`, `mentions` |
| `Tokens/{userId}` | FCM device token |
| RTDB `status/{userId}` | `online`, `lastSeen` (presence) |

**Room id:** chat 1-1 dùng id ghép hai user; chat nhóm dùng UUID lưu tại `groups/{groupId}`.

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
| Backend phía app | **Firebase**: Firestore (chat, user, nhóm), **Realtime Database** (presence), Cloud Messaging, Analytics, Crashlytics, Installations, Auth (phone) |
| Media upload | **Cloudinary** (OkHttp multipart) |
| HTTP client | **Retrofit** + Gson, OkHttp |
| Khác | Glide, Media3 ExoPlayer, ZXing / Code scanner, PhotoView |

> **Lưu ý:** Chat và user data nằm trên **Firestore**; file media upload lên **Cloudinary**. **Realtime Database** chỉ dùng cho trạng thái online / last seen, không dùng Firebase Storage cho chat.

## Một số Fragment / luồng UI

- **Splash / Intro / Login / Register / OTP** — vào app và xác thực.
- **HomeFragment** — danh sách hội thoại (1-1 + nhóm), gợi ý kết bạn; menu **Tạo nhóm**.
- **CreateGroupFragment** — chọn thành viên, đặt tên, tạo nhóm → **ChatFragment**.
- **ChatFragment** — hội thoại 1-1 hoặc nhóm (reply bar, mention picker, reaction burst).
- **PhoneBookFragment**, **SearchFragment**, **FriendRequestFragment**.
- **PersonalFragment**, **DiaryFragment**, **DiscoverFragment**, **SettingFragment**, **StatusFragment**, **ScanQRFragment**.

## Thay đổi chính so với nhánh `main`

| Hạng mục | Mô tả ngắn |
|----------|------------|
| Tạo & chat nhóm | `CreateGroupFragment`, `GroupChat`, gửi tin tới `messages/{groupId}` |
| Avatar nhóm | Ghép avatar thành viên, `GroupAvatarView` |
| Mention | `@` thành viên / `@All`, FCM nhắc tên |
| Presence | Online & last seen qua RTDB + `PresenceManager` |
| Read receipt nhóm | `memberRead`, số chưa đọc, ai đã xem tin cuối |
| Reply tin | Quote block, scroll + highlight tin gốc |
| Cảm xúc | Toggle reaction, burst local + remote khi đang ở chat |
| Notification | Reply từ notification kèm `replyTo`; mention / group payload |
| UX | Back từ chat, sửa spacing tin dài, preview ảnh long-press, delay emoji |

## Chạy dự án

1. **Clone** repository và checkout nhánh cần dùng (ví dụ `create-group`).
2. Mở bằng **Android Studio** (khuyến nghị AGP 8.2 / JDK 17).
3. Thêm **`google-services.json`** của Firebase vào `app/`.
4. Bật **Firestore**, **Realtime Database** (rules cho `status/`), **Cloud Messaging**, **Authentication** (Phone nếu dùng OTP).
5. **Đồng bộ Gradle** và Run trên thiết bị / emulator API 24+.

### Firebase & biến môi trường

- Collection token: **`Tokens/{userId}`** — FCM token thiết bị (lưu khi đăng nhập / refresh).
- RTDB presence: app ghi `status/{userId}` khi online/offline; cần rule cho phép user đọc/ghi node của mình và đọc bạn bè.

### Cloudinary (upload ảnh / video / audio)

Trong `CloudinaryManager.kt` có `CLOUD_NAME` và `UPLOAD_PRESET`. Khi fork, tạo preset trên Cloudinary và cập nhật giá trị — **không** commit secret server-side lên Git công khai.

## Thông báo đẩy (FCM HTTP v1)

App gửi tin nhắn FCM qua:

`https://fcm.googleapis.com/v1/projects/{PROJECT_ID}/messages:send`

Cần **OAuth2 access token** service account (scope `https://www.googleapis.com/auth/firebase.messaging`).

### Các bước cấu hình (khuyến nghị)

1. Firebase Console → **Project settings → Service accounts → Generate new private key**.
2. **Không** commit private key công khai; đọc JSON local (`.gitignore`) hoặc dùng backend proxy.
3. Cập nhật **`AccessToken.kt`** và **`ApiService.kt`** (`{projectId}/messages:send`) khớp project của bạn.
4. Phụ thuộc: `firebase-messaging`, `google-auth-library-oauth2-http` (trong `app/build.gradle.kts`).

Payload hỗ trợ thêm data cho **chat nhóm**, **mention**, **reply** (`NotificationData` / `NotificationReply`).

### HTTP v1 payload (tham khảo)

Tài liệu: [Migrate to FCM HTTP v1](https://firebase.google.com/docs/cloud-messaging/migrate-v1).

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

Nếu service account JSON từng lộ trên repo công khai, **thu hồi và tạo lại key** trên Google Cloud Console.

## Đóng góp

Issues và pull request đều được hoan nghênh.

## Giấy phép

README gốc tham chiếu MIT; nếu chưa có file `LICENSE` trong repo, hãy bổ sung file license thống nhất với ý định phát hành của bạn.
