# Zalo Clone

A modern messaging application built with Android, following the architecture and features of Zalo.

## Features

### 1. Authentication
- User registration and login
- Secure authentication using Firebase

### 2. Chat Features
- Real-time messaging
- Support for text messages
- Image sharing capabilities
- Message reactions (like, laugh, cry, angry, favorite)
- Message copy and delete options
- Message seen status
- Chat head floating window

### 3. Contact Management
- Phone book with alphabetical grouping
- Sticky header for contact groups
- Fast scroll alphabet navigation
- Friend request system
- Friend suggestions

### 4. User Interface
- Modern Material Design
- Smooth animations and transitions
- Custom header views
- Bottom navigation
- Responsive layouts

### 5. Additional Features
- Personal profile management
- Diary/News feed
- Discover section
- Search functionality
- Settings management

## App Screenshots
<div style="display: flex; justify-content: center;">
  <img src="https://github.com/user-attachments/assets/dace1fce-7f7c-4732-9b94-10975e807bd1" alt="Screen Home" width="250"/>
  <img src="https://github.com/user-attachments/assets/c04bf373-8642-43d0-97ca-68d387c24081" alt="Screen Message" width="250"/>
</div>

## Project Structure
```
app/
├── src/
│   ├── main/
│   │   ├── java/com/example/messageapp/
│   │   │   ├── adapter/           # RecyclerView adapters
│   │   │   ├── base/             # Base classes
│   │   │   ├── bottom_sheet/     # Bottom sheet dialogs
│   │   │   ├── custom/           # Custom views
│   │   │   ├── fragment/         # UI fragments
│   │   │   ├── helper/           # Helper classes
│   │   │   ├── model/            # Data models
│   │   │   ├── service/          # Background services
│   │   │   ├── utils/            # Utility classes
│   │   │   ├── viewmodel/        # ViewModels
│   │   │   └── MainActivity.kt   # Main activity
│   │   └── res/                  # Resources
│   └── test/                     # Unit tests
└── build.gradle                  # App level build config
```

## Technical Stack

### Architecture
- MVVM (Model-View-ViewModel) architecture
- Clean Architecture principles
- Dependency Injection using Hilt

### Libraries
- Firebase (Authentication, Realtime Database, Storage)
- Coroutines for asynchronous operations
- Navigation Component
- ViewBinding
- RecyclerView with custom adapters
- Custom animations and transitions

### Key Components

#### Fragments
- HomeFragment: Main chat list and friend suggestions
- ChatFragment: Individual chat conversations
- PersonalFragment: User profile management
- DiaryFragment: News feed and posts
- DiscoverFragment: Content discovery
- SettingFragment: App settings
- FriendRequestFragment: Friend request management
- PhoneBookFragment: Contact management
- SearchFragment: User search functionality

#### Features
- Real-time messaging
- Image sharing
- Message reactions
- Contact management
- User search
- Profile management

## Setup

1. Clone the repository
2. Add your Firebase configuration
3. Build and run the project

## Requirements
- Android Studio Arctic Fox or newer
- Android SDK 21+
- Kotlin 1.5+

## Contributing
Feel free to submit issues and enhancement requests.

## License
This project is licensed under the MIT License - see the LICENSE file for details.

## Configure push notification with Firebase cloud messaging
### 1. Add library to dependencies:

  ``` bash
implementation "com.google.firebase:firebase-messaging:24.0.3
```

### 2. Get file json key in project setting of Firebase:
   
   - *Project -> Setting -> Service accounts -> Select Java -> Click Generate new private key:*
   <div style="display: flex; justify-content: center;">
      <img src="https://github.com/LinhAndroidDev/Zalo-Clone/blob/main/Screenshot%202024-12-07%20at%2013.14.38.png?raw=true" alt="Screen Home" width="800"/>
   </div>

### 3. Generate access token from json key:
```kotlin
object AccessToken {
    private const val FIREBASE_MESSAGING_SCOPE =
        "https://www.googleapis.com/auth/firebase.messaging"

    fun getAccessToken(): String? {
        try {
            val jsonString = "{\n" +
                    "  \"type\": \"service_account\",\n" +
                    "  \"project_id\": \"YOUR_PROJECT_ID\",\n" +
                    "  \"private_key_id\": \"YOUR_PRIVATE_KEY_ID\",\n" +
                    "  \"private_key\": \"-----BEGIN PRIVATE KEY-----\n" +
                    "MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQCy7so6W2Qxq6EU\n" +
                    "qag73gKJaSCpTSjXmQZpj+NY/Orrmg7qqGW+UQvDfbUqMelXlNye8Wt7fxZRZlB9\n" +
                    "K4Pp3s2xvFnAX022M+47WO8A6YJ4jDNBLz5mgxqcgkgIwsuvhw49trRp1K0CiLwU\n" +
                    "GCaf7OCxXTdbGYY6sfNrRKp3FgDgQnDdgLNjzb6GenCAm4Fg5NB7KLJpCJYy0ZYB\n" +
                    "OX7VQvok7xshz1H4i4w68yTcsM2bHn8toLaI+0FlXuD235gHYFtLsWUXpZHpV8e3\n" +
                    "raX0+hhqImbiDikU/Wl4n8UjchTpowNuzJyWsyd4QQFfj4Rky0CMUizwOhUVqylI\n" +
                    "k3K8VHSrAgMBAAECggEAAwusdoTStkO5GKxwmCBFw8f9zdPp442PveE8memoJ/11\n" +
                    "zomyPaSMgjXUDRVPZvW6MZxjW3VE0Hrg2NiSRLtNnf5aOruEi5rjra/sVYQj++BL\n" +
                    "CQuAUOdfXxWam0eRhvnSBLvk4z5C+Z2RMfhdQ/CYvHwdMphDICGiRoujKSa/OhHw\n" +
                    "JOy1kSP24qPbWBElyZyCcIo4rpC8gItZl2qGFBTacuhtahqwqXOhEQJJuruHNM1q\n" +
                    "gAne43gyfsU7nkqD0/lwDgiHMLCrbdjGWWuCuIMT/vzIt89dBR/EUVeAEQYUzZ6a\n" +
                    "JLgPvkpim2+a9Knhm5hx8zTeZ9RaMuc2L70rEAZ/cQKBgQDh81DIdfg/8vr96w17\n" +
                    "EG+z0gpfx0By6VqBaSUMpAJ+C9Z9d3ASyLwg0/k+XbUzAQUFZd4kHaSv4aQaKs5B\n" +
                    "NH+quHPn6/6k4CRi9IAt8REj4JN+Cwyn9T5PoSWROk6/vwdep0uobG1A1fBSsQnW\n" +
                    "ZEj9oXLncbzpvnmC5kZvajjXUQKBgQDKurc0vuU1dWDtuZZR33p+THsaHFmfCMgs\n" +
                    "2/vG2fH9s7A8sn69uErEpL5Kx6xT9PNs0wjtrfyAFJo2yNMBHEEvYIDLQ1Jx6K4+\n" +
                    "DE2uK6iUmBEZle0A9GvO4xvJiPJCwsitxUbJK0a6RKIQCj4D89/iYXJ3P6nMnatC\n" +
                    "y46UM5JFOwKBgQCSKg85Di9gVvOMrLBUysYnwhkZ6lBDxbbZfkYMTlCab1f6Y/go\n" +
                    "/pfMeLOEZ6Qe8WrpGgPAwzhU2peIoeY5AhgQPTAleGGLEMAZD2eX0Jkw50ciQ02V\n" +
                    "nS0I4AroTprAqXfAAGMN+c4XIg5Lv+DIQqmBAR7On6IAZ0o9pm8sBb/tcQKBgHLe\n" +
                    "ZRx/5cPqpGdOtvvhErkpgL8EvUs9YJ76bqj3qQRFomBiCypYmBTf++rHRL+1lZBd\n" +
                    "6zsxUFcKVW8hT13bspuzpIaHuNlOLByAQCumFTlNCLNkngvicouhZ4dED3EAiVDc\n" +
                    "7QTjfonghattAkKfFoZhDMjAy+dilz2btUgICKMtAoGAR0CohUE2XOyZ9UpEoYrr\n" +
                    "BQl0sVBNsbaNNEAZY3L0Ks8kMt3iMIHO553UOc2eP37ppT9lJoNchnjUpDJBQUOA\n" +
                    "9u9Ko8DPxAt7ZFOjALhHFbf0Svj7UEqeEE3wFnhsRf22vdrGI+dvdsXzWzz6p/7k\n" +
                    "NOMR/8IbDJBb+Xf7NuV1uNo=\n" +
                    "-----END PRIVATE KEY-----\n\",\n" +
                    "  \"client_email\": \"firebase-adminsdk-4f5r1@YOUR_PROJECT_ID.iam.gserviceaccount.com\",\n" +
                    "  \"client_id\": \"YOUR_CLIENT_ID\",\n" +
                    "  \"auth_uri\": \"https://accounts.google.com/o/oauth2/auth\",\n" +
                    "  \"token_uri\": \"https://oauth2.googleapis.com/token\",\n" +
                    "  \"auth_provider_cert_url\": \"https://www.googleapis.com/oauth2/v1/certs\",\n" +
                    "  \"client_x_cert_url\": \"https://www.googleapis.com/robot/v1/metadata/x509/firebase-adminsdk-4f5r1%40YOUR_PROJECT_ID.iam.gserviceaccount.com\",\n" +
                    "  \"universe_domain\": \"googleapis.com\"\n" +
                    "}"
            val stream: InputStream =
                ByteArrayInputStream(jsonString.toByteArray(StandardCharsets.UTF_8))
            val googleCredentials =
                GoogleCredentials.fromStream(stream).createScoped(FIREBASE_MESSAGING_SCOPE)
            googleCredentials.refresh()
            return googleCredentials.accessToken.tokenValue
        } catch (e: Exception) {
            Log.e("AccessToken", "getAccessToken: " + e.localizedMessage)
            return null
        }
    }
}
```

### 4. Create Api Service
```
interface ApiService {
    @POST("YOUR_PROJECT_ID/messages:send")
    @Headers(
        "Content-Type: application/json",
        "Accept: application/json"
        )
    fun sendMessage(
        @Body message: MessageRequest,
        @Header("Authorization") accessToken: String = "Bearer ${AccessToken.getAccessToken()}"
    ): Call<MessageRequest>
}
```

## Document Migrate from legacy FCM APIs to HTTP v1
- document: https://firebase.google.com/docs/cloud-messaging/migrate-v1
```
{
   "message":{
      "token":"bk3RNwTe3H0:CI2k_HHwgIpoDKCIZvvDMExUdFQ3P1...",
      "notification":{
        "body":"This is an FCM notification message!",
        "title":"FCM Message"
      }
   }
}
```
