# Zalo Clone

Ứng dụng nhắn tin trên Android (Kotlin), lấy cảm hứng từ Zalo: danh sách chat 1-1 và nhóm, hội thoại realtime, danh bạ, kết bạn, nhật ký (feed), khám phá, cài đặt và thông báo đẩy (FCM).

> **Nhánh khuyến nghị:** `update-project-structure` — gồm toàn bộ tính năng chat nhóm / reply / reaction / presence (như `main`) **cộng** refactor **Clean Architecture** ba module (`:app`, `:domain`, `:data`).  
> **Nhánh `main`:** monolithic một module `:app`, `FireBaseInstance` nằm trong `app/utils/`.

## So sánh `main` và `update-project-structure`

| Hạng mục | `main` | `update-project-structure` |
|----------|--------|---------------------------|
| **Module Gradle** | Chỉ `:app` | `:app` + `:domain` + `:data` |
| **Lịch sử commit** | Baseline | **+6 commit** trên `main` (không có commit riêng trên `main` mà thiếu trên nhánh này) |
| **Thống kê diff** | — | ~124 file, **+4409 / −1467** dòng (`main...update-project-structure`) |
| **Firestore / Firebase** | `app/utils/FireBaseInstance.kt` | `data/legacy/FireBaseInstance.kt` (delegate từ repository impl) |
| **Model dữ liệu** | `app/model/` (Parcelable UI) | **3 lớp:** `data/firestore/` → `domain/model/` → `app/model/` + mapper |
| **Session** | `SharePreferenceRepository` + impl trong `app` | `SessionRepository` (`domain`) + `SessionRepositoryImpl` (`:data`) |
| **ViewModel** | Gọi trực tiếp `FireBaseInstance` | Inject **use case** / **repository** (Hilt) |
| **Adapter / service** | Nhiều chỗ gọi `FireBaseInstance` | Đã migrate: inbox typing/avatar, search/friend, `GroupAvatarLoader`, `ReceiverMessageService`, `NotificationReply` |
| **Logic thuần** | `EmotionReactionDetector`, mention trong `app` | `domain/chat/` (`EmotionReactionDetector`, `MentionParser`) |
| **Test** | Không có module test domain | `domain/src/test/` (`DomainTests.kt`) |
| **Code đã xóa / gom** | `app/remote/`, `AppModule`, util trùng | Remote → `data/remote/`; `ReplyNotificationHelper` trong `data/legacy/` |

### Các commit trên `update-project-structure` (chưa có trên `main`)

1. `update project structure` — tách `:domain`, `:data`, repository, use case, mapper  
2. `separate FireBaseInstance from viewmodel` — ViewModel qua use case  
3. `fix warning`  
4. `remove code unuse` — dọn file/model trùng  
5. `update code` — social/diary mapper, ViewModel mở rộng  
6. `not use FirebaseInstance directly in the adapter and service` — adapter + FCM/reply  

### Trạng thái migrate (nhánh `update-project-structure`)

| Lớp | Đã qua Clean Architecture | Còn legacy |
|-----|---------------------------|------------|
| **ViewModel** | Hầu hết (chat, inbox, social, diary, auth, profile, …) | Một số vẫn import `PresenceManager` / `DateUtils` từ `data/legacy` |
| **Repository impl** (`:data`) | Interface đầy đủ trong `:domain` | Implementation **ủy quyền** `FireBaseInstance` |
| **`:app` trực tiếp Firebase** | Không (trừ typealias) | `MessageReplyHelper.fetchUserDisplayName` → `LegacyCompat.FireBaseInstance` |
| **FCM gửi đi** | `NotificationRepository` (có thể) | Token OAuth: `data/legacy/AccessToken.kt` |

---

## Tính năng chính

*(Cùng bộ tính năng trên `main` và `update-project-structure`; khác biệt chủ yếu là **cách tổ chức code**.)*

### Xác thực & tài khoản

- Đăng ký / đăng nhập: xác thực qua **Firestore** (truy vấn `users` theo email + mật khẩu đã lưu).
- **Firebase Auth (Phone)**: dùng trong luồng OTP (ví dụ `ReceiveOTPFragment`).
- Lưu phiên cục bộ qua Hilt / **`SessionRepository`** (`:data` module; trên `main` là `SharePreferenceRepository`).
- Sau khi chấp nhận lời mời kết bạn, đồng bộ danh sách bạn và hội thoại inbox.

### Chat 1-1

- Tin nhắn **realtime** qua Firestore (`messages/{roomId}/chats`).
- Tin văn, ảnh, một ảnh, ghi âm; **cảm xúc (reaction)** trên tin (yêu thích, thích, cười, khóc, giận).
- **Trả lời tin (reply)**: quote tin gốc trên bubble, preview khi soạn, tap quote để cuộn tới tin gốc và **highlight** tạm thời.
- **Hiệu ứng burst cảm xúc**: icon mini bay ra từ chip reaction; phát hiện reaction remote qua `EmotionReactionDetector` (`:domain` trên nhánh refactor).
- Trạng thái đã xem, đếm chưa đọc, chỉ báo đang gõ (inbox sync typing qua `HomeViewModel` trên nhánh refactor).
- **Trạng thái online / last seen** (Firebase Realtime Database qua `PresenceManager` trong `data/legacy`).
- **Chat head** (dịch vụ nổi) khi có thông báo.
- Gửi thông báo FCM: đọc token từ `Tokens/{userId}` → **FCM HTTP v1** (Retrofit, `data/remote`).

### Chat nhóm

- **Tạo nhóm** (`CreateGroupFragment`): chọn tên, thành viên, mở thẳng chat.
- Tin trong `messages/{groupId}/chats`; metadata tại `groups/{groupId}`.
- **Avatar nhóm ghép** (`GroupAvatarView`, **`GroupAvatarLoader`** inject `GroupChatRepository` trên nhánh refactor).
- **Tin chào mừng**; đồng bộ inbox `Conversation{userId}/{groupId}`.
- **Mention (@)** / **@All**, FCM khi bị nhắc.
- **Đang gõ** nhóm; **đã xem** (`memberRead`).
- Reply, reaction, burst, long-press menu; tên người gửi trên bubble nhận.

### Danh bạ & bạn bè

- Danh bạ nhóm theo chữ cái, sticky header, fast scroll.
- Lời mời kết bạn (gửi / nhận / chấp nhận / từ chối / hủy).

### Nhật ký (Diary)

- Đăng bài, ảnh, link preview; thích và bình luận; feed bạn bè (`DiaryRepository` + use case).

### Khác

- Trạng thái (Status): đăng / xem / sửa media.
- Tìm kiếm người dùng, lịch sử tìm kiếm.
- QR, hồ sơ cá nhân, xem trước ảnh.
- **Cloudinary**: upload ảnh / video / audio (`data/legacy/CloudinaryManager.kt`).

## Ảnh màn hình

<div style="display: flex; justify-content: center;">
  <img src="https://github.com/user-attachments/assets/dace1fce-7f7c-4732-9b94-10975e807bd1" alt="Screen Home" width="250"/>
  <img src="https://github.com/user-attachments/assets/c04bf373-8642-43d0-97ca-68d387c24081" alt="Screen Message" width="250"/>
</div>

## Kiến trúc (Clean Architecture — `update-project-structure`)

```
:domain/          # Kotlin thuần — không phụ thuộc Android/Firebase
  model/          # Message, Conversation, User, DiaryPost, …
  repository/     # Interfaces: Chat, Session, GroupChat, Friend, Diary, …
  usecase/
    chat/         # SendMessage, ObserveMessages, CreateGroup, …
    inbox/        # ObserveInbox, GetConversation, …
    social/       # SearchUsers, FriendRequest, GetUserInfo, …
    diary/        # ObserveDiaryFeed, CreatePost, Comments, …
  chat/           # EmotionReactionDetector, MentionParser

:data/            # Firebase, Retrofit, Cloudinary, Hilt bindings
  firestore/      # DTO Firestore (Message, Conversation, …)
  legacy/         # FireBaseInstance, PresenceManager, CloudinaryManager, …
  repository/     # *RepositoryImpl → delegate legacy khi cần
  mapper/         # EntityMapper (firestore ↔ domain)
  remote/         # ApiClient, ApiService, FCM DTO
  session/        # SessionRepositoryImpl
  di/             # RepositoryModule, DataModule

:app/             # UI Android
  fragment/, adapter/, viewmodel/, service/, broadcast/
  model/          # Parcelable UI (Navigation, Intent extras)
  mapper/         # ChatUiMapper, SocialUiMapper, DiaryUiMapper, SessionLanguageMapper
  utils/          # UI helpers; LegacyCompat (typealias tạm)
```

**Luồng phụ thuộc:** `app → domain ← data` (domain không biết Android).

**Luồng dữ liệu điển hình:**

```
UI (Fragment/Adapter)
  → ViewModel (use case)
    → Repository interface (:domain)
      → RepositoryImpl (:data)
        → FireBaseInstance / Firestore / RTDB / Retrofit
```

**Mapper:**

| Mapper | Vai trò |
|--------|---------|
| `EntityMapper` | `data/firestore` ↔ `domain/model` |
| `ChatUiMapper` / `SocialUiMapper` / `DiaryUiMapper` | `domain` ↔ `app/model` (Parcelable) |

## Cấu trúc thư mục `:app` (rút gọn)

```
app/src/main/java/com/example/messageapp/
├── adapter/          # Chat, ListChat, Search, FriendRequest, CreateGroupMember, …
├── broadcast/        # NotificationReply (SendMessageUseCase + GetUserInfoUseCase)
├── bottom_sheet/     # Ảnh, sticker, ngôn ngữ, diary comments, …
├── custom/           # GroupAvatarView, CustomHeaderView, TypingIndicatorView, …
├── fragment/         # Home, Chat, CreateGroup, Diary, Status, …
├── mapper/           # ChatUiMapper, SocialUiMapper, DiaryUiMapper
├── model/            # UI Parcelable (giữ cho Navigation / Intent)
├── service/          # ReceiverMessageService (FCM), ChatHeadService
├── utils/
│   ├── LegacyCompat.kt          # typealias FireBaseInstance, PresenceManager, DateUtils
│   ├── MessageReplyHelper.kt    # Bind quote reply UI
│   ├── GroupAvatarLoader.kt     # @Singleton — GroupChatRepository
│   ├── MentionHelper.kt         # UI @mention (parse core ở domain)
│   ├── EmotionBurstEffect.kt
│   └── FileUtils, GalleryUtils, …
└── viewmodel/        # Hilt + use case (không gọi FireBaseInstance trực tiếp)
```

> Trên **`main`**, cấu trúc tương tự nhưng **không có** `mapper/`, `FireBaseInstance.kt` nằm trong `utils/`, có thêm `remote/` và `di/AppModule` trong `:app`.

## Use case chính (`:domain`)

| Nhóm | Ví dụ |
|------|--------|
| **Chat** | `ObserveMessagesUseCase`, `SendMessageUseCase`, `UpdateTypingUseCase`, `CreateGroupUseCase`, `UploadChatMediaUseCase` |
| **Inbox** | `ObserveInboxUseCase`, `GetConversationUseCase`, `GetUnreadCountUseCase` |
| **Social** | `GetFriendsUseCase`, `SearchUsersUseCase`, `SendFriendRequestUseCase`, `GetUserInfoUseCase` |
| **Diary** | `ObserveDiaryFeedUseCase`, `CreateDiaryPostUseCase`, `ObserveDiaryCommentsUseCase` |

## Mô hình dữ liệu (Firestore / RTDB)

| Collection / path | Mô tả |
|-------------------|--------|
| `users/{userId}` | Hồ sơ, subcollection `friends` |
| `Conversation{userId}/{roomId}` | Inbox; `isGroup = true` cho nhóm; `friendId` = `groupId` (UUID) |
| `groups/{groupId}` | Tên nhóm, `memberIds`, `typing`, `memberRead/{userId}` |
| `messages/{roomId}/chats/{time}` | Tin chat; `emotion`, `replyTo`, `mentions` |
| `Tokens/{userId}` | FCM device token |
| RTDB `status/{userId}` | `online`, `lastSeen` |

**Room id:** chat 1-1 = id ghép hai user; nhóm = UUID (`groups/{groupId}`).

## Công nghệ & phiên bản

| Thành phần | Ghi chú |
|------------|---------|
| Ngôn ngữ | Kotlin **1.9.22** |
| Android Gradle Plugin | **8.2.1** |
| Gradle Wrapper | **8.2** |
| `minSdk` / `targetSdk` | **24** / **34** (`compileSdk` 34) |
| Module | `:app`, `:domain`, `:data` (`update-project-structure`) |
| UI | Material, ViewBinding + Data Binding, Navigation Component |
| DI | **Hilt** 2.48 — `RepositoryModule` trong `:data` |
| Async | Kotlin **Coroutines**, **Flow** |
| Backend | Firestore, RTDB (presence), FCM, Analytics, Crashlytics, Auth (phone) |
| Media | **Cloudinary** (OkHttp) |
| HTTP | **Retrofit** + Gson — FCM HTTP v1 |

> Chat và user data trên **Firestore**; media trên **Cloudinary**. **RTDB** chỉ cho online / last seen.

## Một số Fragment / luồng UI

- **Splash / Intro / Login / Register / OTP** — xác thực.
- **HomeFragment** — inbox 1-1 + nhóm, presence/typing/avatar sync (`HomeViewModel`).
- **CreateGroupFragment** → **ChatFragment** — nhóm mới.
- **ChatFragment** — reply, mention, reaction burst.
- **PhoneBookFragment**, **SearchFragment**, **FriendRequestFragment**.
- **PersonalFragment**, **DiaryFragment**, **DiscoverFragment**, **SettingFragment**, **StatusFragment**, **ScanQRFragment**.

## Chạy dự án

1. **Clone** và checkout nhánh phù hợp:
   - Phát triển / PR: `git checkout update-project-structure`
   - So sánh legacy: `git checkout main`
2. Mở **Android Studio** (AGP 8.2, **JDK 17**).
3. Thêm **`google-services.json`** vào `app/`.
4. Bật **Firestore**, **Realtime Database** (rules `status/`), **Cloud Messaging**, **Authentication** (Phone nếu dùng OTP).
5. **Sync Gradle** (3 module) và Run (API 24+).

### Firebase & biến môi trường

- **`Tokens/{userId}`** — FCM token (lưu khi đăng nhập).
- RTDB **`status/{userId}`** — presence online/offline.

### Cloudinary

Cấu hình trong `data/legacy/CloudinaryManager.kt` (`CLOUD_NAME`, `UPLOAD_PRESET`). Không commit secret production lên Git công khai.

## Thông báo đẩy (FCM HTTP v1)

Endpoint:

`https://fcm.googleapis.com/v1/projects/{PROJECT_ID}/messages:send`

Cần **OAuth2 access token** service account — `data/legacy/AccessToken.kt`, `data/remote/ApiService.kt`.

### Cấu hình (khuyến nghị)

1. Firebase Console → **Service accounts → Generate new private key**.
2. Không commit private key; dùng file local (`.gitignore`) hoặc **Cloud Functions** (khuyến nghị production).
3. Cập nhật `AccessToken.kt` và `ApiService` đúng `{projectId}`.
4. Payload data: **nhóm**, **mention**, **reply** (`NotificationData`, `ReceiverMessageService`, `NotificationReply`).

### HTTP v1 payload (tham khảo)

[Tài liệu FCM HTTP v1](https://firebase.google.com/docs/cloud-messaging/migrate-v1)

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

Nếu service account JSON từng lộ công khai, **thu hồi và tạo lại key** trên Google Cloud Console.

## Việc tiếp theo (gợi ý kỹ thuật)

- Hoàn tất tách `FireBaseInstance` khỏi `*RepositoryImpl` (implementation Firestore thật trong `:data`).
- Gỡ `LegacyCompat` và `MessageReplyHelper` → `GetUserInfoUseCase`.
- FCM gửi từ **Cloud Functions** thay client.
- Phân trang tin nhắn + cache local (Room).

## Đóng góp

Issues và pull request đều được hoan nghênh.

## Giấy phép

README gốc tham chiếu MIT; nếu chưa có file `LICENSE` trong repo, hãy bổ sung file license thống nhất với ý định phát hành của bạn.
