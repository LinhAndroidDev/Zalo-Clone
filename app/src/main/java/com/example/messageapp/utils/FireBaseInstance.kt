package com.example.messageapp.utils

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.messageapp.MyApplication
import com.example.messageapp.R
import com.example.messageapp.model.Conversation
import com.example.messageapp.model.Emotion
import com.example.messageapp.model.Friend
import com.example.messageapp.model.FriendRequest
import com.example.messageapp.model.Message
import com.example.messageapp.model.Sticker
import com.example.messageapp.model.TypeMessage
import com.example.messageapp.model.DiaryLinkPreview
import com.example.messageapp.model.DiaryPost
import com.example.messageapp.model.DiaryPostComment
import com.example.messageapp.model.DiaryPostFirestore
import com.example.messageapp.model.User
import com.example.messageapp.remote.ApiClient
import com.example.messageapp.remote.Token
import com.example.messageapp.remote.request.Data
import com.example.messageapp.remote.request.MessageRequest
import com.example.messageapp.remote.request.NotificationData
import com.example.messageapp.utils.FileUtils.compressImage
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.util.HashMap
import java.util.UUID
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

object FireBaseInstance {
    private val db by lazy { Firebase.firestore }

    private const val PATH_USER = "users"
    private const val PATH_EMAIL = "email"
    private const val PATH_PASSWORD = "password"
    private const val PATH_AVATAR = "avatar"
    private const val PATH_MESSAGE = "messages"
    private const val PATH_CHAT = "chats"
    private const val PATH_TOKEN = "Tokens"
    private const val PATH_IMAGE_COVER = "imageCover"
    private const val PATH_IMAGE = "images"
    private const val PATH_PHOTO = "photo"
    private const val PATH_EMOTION = "emotion"
    private const val PATH_AUDIO = "audios"
    private const val PATH_TYPING = "typing"
    private const val PATH_STICKER = "sticker"
    private const val PATH_FRIEND_REQUESTS = "friendRequests"
    private const val PATH_FRIENDS = "friends"
    private const val PATH_SEARCH_HISTORY = "searchHistory"
    private const val PATH_ITEMS = "items"

    /**
     * This function is used to check the login of the user
     * + By query whereEqualTo @param email and @param password
     * @param email email of user
     * @param password password of user
     * @param success callback when query is successful
     * @param failure callback when query is failed
     */
    fun checkLogin(
        email: String,
        password: String,
        success: (QuerySnapshot) -> Unit,
        failure: (String) -> Unit
    ) {
        db.collection(PATH_USER)
            .whereEqualTo(PATH_EMAIL, email)
            .whereEqualTo(PATH_PASSWORD, password)
            .get()
            .addOnSuccessListener { querySnapshot ->
                success.invoke(querySnapshot)
            }
            .addOnFailureListener { exception ->
                failure.invoke(exception.message.toString())
            }
    }

    /**
     * This function is used to get all users from the FireStore database
     * @param success callback when query is successful
     * @param failure callback when query is failed
     */
    fun getUsers(success: (QuerySnapshot) -> Unit, failure: (String) -> Unit) {
        db.collection(PATH_USER).get().addOnSuccessListener { result ->
            success.invoke(result)
        }.addOnFailureListener { e ->
            failure.invoke(e.message.toString())
        }
    }

    /**
     * This function is used to add a new user to the FiresStore database
     * @param user data user
     * @param success callback when query is successful
     * @param failure callback when query is failed
     */
    fun addUser(
        user: HashMap<String, String>,
        success: (String) -> Unit,
        failure: (String) -> Unit
    ) {
        db.collection(PATH_USER).add(user)
            .addOnSuccessListener {
                success.invoke("Create Successful")
            }.addOnFailureListener { e ->
                failure.invoke(e.message.toString())
            }
    }

    /**
     * This function is used to get all messages from the FireStore database
     * @param idRoom id room of chat room
     * @param success callback when query is successful
     */
    fun getMessage(
        idRoom: String,
        success: (QuerySnapshot?) -> Unit,
        failure: (String) -> Unit
    ) {
        db.collection(PATH_MESSAGE)
            .document(idRoom)
            .collection(PATH_CHAT)
            .orderBy("time", Query.Direction.ASCENDING)
            .addSnapshotListener { value, error ->
                if (error != null) {
                    failure.invoke(error.message.toString())
                    return@addSnapshotListener
                }
                success.invoke(value)
            }
    }

    /**
     * This function is used to send a message to the FireStore database
     * + Here we send a message and create 2 conversations between the sender and the receiver
     * @param message data message
     * @param userId key auth of user
     * @param time time message sent
     * @param conversation data conversation
     * @param nameSender name of sender
     * @param type type of message
     * @param success callback when query is successful
     */
    fun sendMessage(
        message: Message,
        userId: String,
        time: String,
        conversation: Conversation,
        nameSender: String,
        type: TypeMessage = TypeMessage.MESSAGE,
        sendFirst: Boolean,
        success: () -> Unit,
    ) {
        val idRoom = listOf(conversation.friendId, userId).sorted()

        db.collection(PATH_MESSAGE)
            .document(idRoom.toString())
            .collection(PATH_CHAT)
            .document(time)
            .set(message)

        if (sendFirst) {
            Log.e("sendMessage", "sendFirst")
            handleSendMessage(message, userId, time, conversation, nameSender, type, 1)
        } else {
            Log.e("sendMessage", "not sendFirst")
            getConversation(conversation.friendId, userId) { cvt ->
                val num = cvt.numberUnSeen + 1
                handleSendMessage(message, userId, time, conversation, nameSender, type, num)
            }
        }
        success.invoke()
    }

    /**
     * This function is used to handle send a message to the FireStore database
     * + Send a notification to the receiver via the receiver's token.
     * + Update data conversation between sender and receiver
     * @param message data message
     * @param userId key auth of user
     * @param time time message sent
     * @param conversation data conversation
     * @param nameSender name of sender
     * @param type type of message
     * @param numberUnSeen quantity of message unseen
     */
    private fun handleSendMessage(
        message: Message,
        userId: String,
        time: String,
        conversation: Conversation,
        nameSender: String,
        type: TypeMessage = TypeMessage.MESSAGE,
        numberUnSeen: Int
    ) {

        //Get token of receiver to send notification message to receiver
        getTokenMessage(
            conversation.friendId,
            success = { token ->
                val notificationNotification = NotificationData(
                    token = token,
                    data = Data(
                        title = nameSender,
                        body = when (type) {
                            TypeMessage.MESSAGE -> {
                                message.message
                            }

                            TypeMessage.PHOTOS -> {
                                "$nameSender đã gửi ảnh cho bạn"
                            }

                            TypeMessage.SINGLE_PHOTO -> {
                                "$nameSender đã gửi 1 ảnh cho bạn"
                            }

                            TypeMessage.AUDIO -> {
                                "$nameSender đã gửi 1 file ghi âm cho bạn"
                            }
                        },
                        senderId = userId
                    )
                )

                ApiClient.api?.sendMessage(MessageRequest(message = notificationNotification))
                    ?.enqueue(object : Callback<MessageRequest> {
                        override fun onFailure(
                            call: Call<MessageRequest>,
                            t: Throwable
                        ) {
                            Log.e("Send Message", "Send Fail")
                        }

                        override fun onResponse(
                            call: Call<MessageRequest>,
                            response: Response<MessageRequest>
                        ) {
                            Log.e("Send Message", "Send Successful")
                        }
                    })
            },
            failure = {
                Log.e("Send Message", "Token retrieval failed")
            }
        )

        //Create Data Conversation For Sender
        val conversationData = Conversation(
            friendId = conversation.friendId,
            friendImage = conversation.friendImage,
            message = when (type) {
                TypeMessage.MESSAGE -> {
                    message.message
                }

                TypeMessage.PHOTOS -> {
                    "Bạn đã gửi ảnh cho ${conversation.name}"
                }

                TypeMessage.SINGLE_PHOTO -> {
                    "Bạn đã gửi 1 ảnh cho ${conversation.name}"
                }

                TypeMessage.AUDIO -> {
                    "Bạn đã gửi 1 file ghi âm cho ${conversation.name}"
                }
            },
            name = conversation.name,
            person = "Bạn",
            sender = userId,
            time = time,
        )

        //Create Conversation For Sender
        db.collection("Conversation${userId}")
            .document(conversation.friendId)
            .set(conversationData)

        //Create Data Conversation For Receiver
        val conversationFriend = Conversation(
            friendId = userId,
            message = when (type) {
                TypeMessage.MESSAGE -> {
                    message.message
                }

                TypeMessage.PHOTOS -> {
                    "$nameSender đã gửi ảnh cho bạn"
                }

                TypeMessage.SINGLE_PHOTO -> {
                    "$nameSender đã gửi 1 ảnh cho bạn"
                }

                TypeMessage.AUDIO -> {
                    "$nameSender đã gửi 1 file ghi âm cho bạn"
                }
            },
            name = nameSender,
            person = nameSender,
            sender = userId,
            time = time,
            numberUnSeen = numberUnSeen
        )

        //Create Conversation For Receiver
        db.collection("Conversation${conversation.friendId}")
            .document(userId)
            .set(conversationFriend)
    }

    /**
     * This function is used to get all conversations from the FireStore database
     * @param userId key auth of user
     * @param success callback when query is successful
     * @param failure callback when query is failed
     */
    fun getListConversation(
        userId: String,
        success: (QuerySnapshot?) -> Unit,
        failure: (String) -> Unit
    ) {
        db.collection("Conversation${userId}")
            .orderBy("time", Query.Direction.DESCENDING)
            .addSnapshotListener { value, error ->
                if (error != null) {
                    failure.invoke(error.message.toString())
                    return@addSnapshotListener
                }
                success.invoke(value)
            }
    }

    /**
     * This function is used to save token of user to the FireStore database
     * @param userId key auth of user
     * @param data data token
     */
    fun saveTokenMessage(userId: String, data: HashMap<String, String>) {
        db.collection(PATH_TOKEN).document(userId).set(data).addOnSuccessListener {
        }
    }

    /**
     * This function is used to get token of receiver from the FireStore database
     * @param friendId key auth of friend
     * @param success callback when query is successful
     * @param failure callback when query is failed
     */
    private fun getTokenMessage(
        friendId: String,
        success: (String) -> Unit,
        failure: (String) -> Unit
    ) {
        db.collection(PATH_TOKEN).document(friendId)
            .addSnapshotListener { value, error ->
                if (error != null) {
                    failure.invoke(error.message.toString())
                }
                if (value != null && value.exists()) {
                    val tokenObject = value.toObject(Token::class.java)
                    success.invoke(tokenObject?.token ?: "")
                } else {
                    failure.invoke("Token not found")
                }
            }
    }

    /**
     * This function is used to get information user from the FireStore database
     * @param userId key auth of user
     * @param success callback when query is successful
     */
    fun getInfoUser(userId: String, success: (User) -> Unit) {
        db.collection(PATH_USER)
            .document(userId)
            .addSnapshotListener { value, error ->
                if (error != null) {
                    Log.e("getInfoUser", error.message.toString())
                }
                if (value != null && value.exists()) {
                    success.invoke(value.toObject(User::class.java)!!)
                } else {
                    Log.e("getInfoUser", "User not found")
                }
            }
    }

    /**
     * One-time fetch to verify if a user with [userId] exists.
     * Calls [success] with the User if found, [failure] if not found or on error.
     */
    fun getUserById(
        userId: String,
        success: (User) -> Unit,
        failure: (String) -> Unit
    ) {
        if (userId.isBlank()) {
            failure.invoke("ID không hợp lệ")
            return
        }
        db.collection(PATH_USER)
            .document(userId)
            .get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    val user = doc.toObject(User::class.java)
                    if (user != null) {
                        success.invoke(user.copy(keyAuth = doc.id))
                    } else {
                        failure.invoke("Không tìm thấy người dùng")
                    }
                } else {
                    failure.invoke("Không tìm thấy người dùng")
                }
            }
            .addOnFailureListener { failure.invoke(it.message.toString()) }
    }

    /**
     * This function is used to upload image to the Storage Firebase
     * @param context context of activity
     * @param uriPhoto uri of photo
     * @param success callback when upload is successful
     */
    fun uploadImage(
        context: Context,
        uriPhoto: Uri,
        success: (String) -> Unit,
        failure: ((String) -> Unit)? = null
    ) {
        val bytes = context.compressImage(uriPhoto)
        val fileName = "${UUID.randomUUID()}.jpg"
        val folder = PATH_IMAGE // Firebase path: images/*

        CloudinaryManager.uploadBytes(
            fileBytes = bytes,
            fileName = fileName,
            mimeType = "image/jpeg",
            folder = folder,
            onSuccess = success,
            onFailure = { e ->
                Log.e("Check fail uploadImage Cloudinary", "uploadImage failed: ${e.message}", e)
                failure?.invoke(
                    e.message ?: MyApplication.appContext.getString(R.string.error_upload_image)
                )
            }
        )
    }

    /**
     * This function is used to update avatar of user to the FireStore database
     * @param avatar data avatar
     * @param userId key auth of user
     */
    fun updateAvatarUser(avatar: String, userId: String) {
        db.collection(PATH_USER)
            .document(userId)
            .update(PATH_AVATAR, avatar)
    }

    /**
     * This function is used to get conversation from the FireStore database
     * @param friendId key auth of friend
     * @param userId key auth of user
     * @param success callback when query is successful
     */
    fun getConversation(friendId: String, userId: String, success: (Conversation) -> Unit) {
        db.collection("Conversation${friendId}")
            .document(userId)
            .get()
            .addOnSuccessListener { result ->
                val conversation = result.toObject(Conversation::class.java)
                conversation?.let { success.invoke(it) }
            }
    }

    /**
     * This function is used to get conversation from the FireStore database with Realtime
     * @param friendId key auth of friend
     * @param userId key auth of user
     * @param success callback when query is successful
     */
    fun getConversationRlt(friendId: String, userId: String, success: (Conversation) -> Unit) {
        db.collection("Conversation${friendId}")
            .document(userId)
            .addSnapshotListener { value, _ ->
                if(value != null) {
                    val conversation = value.toObject(Conversation::class.java)
                    conversation?.let { success.invoke(it) }
                }
            }
    }

    /**
     * This function is used to update seen message for conversation user and friend
     * @param userId key auth of user
     * @param friendId key auth of friend
     */
    fun seenMessage(userId: String, friendId: String) {
        db.collection("Conversation${friendId}")
            .document(userId)
            .update(
                "seen", "1",
                "numberUnSeen", 0
            )
        db.collection("Conversation${userId}")
            .document(friendId)
            .update(
                "seen", "1",
                "numberUnSeen", 0
            )
    }

    /**
     * This function is used to get number of unread messages from the FireStore database
     * @param userId key auth of user
     * @param number callback number of unread messages
     */
    fun getNumberUnreadMessages(userId: String, number: (Int) -> Unit) {
        db.collection("Conversation${userId}")
            .addSnapshotListener { value, _ ->
                if (value != null) {
                    var num = 0
                    value.forEach { document ->
                        val conversation = document.toObject(Conversation::class.java)
                        num += conversation.numberUnSeen
                    }
                    number.invoke(num)
                }
            }
    }

    /**
     * This function is used to upload list photo to the Storage Firebase
     * @param context context of activity
     * @param uris list uri of photo
     * @param roomId list id room of photo
     * @param process callback when upload is processing
     * @param success callback when upload is successful
     */
    fun uploadListPhoto(
        context: Context,
        uris: ArrayList<Uri>,
        roomId: List<String>,
        process: (Pair<Int, Double>) -> Unit,
        success: (ArrayList<String>) -> Unit
    ) = CoroutineScope(Dispatchers.IO).launch {
            val photos = arrayListOf<String>()
            val deferredList = uris.map { uri ->
                async {
                    val photoUrl = uploadPhoto(context, uri, roomId)
                    photoUrl?.let { photos.add(it) }
                }
            }
            deferredList.awaitAll()
            success.invoke(photos)
        }

    /**
     * This function is used to upload photo to the Storage Firebase
     * @param context context of activity
     * @param uri uri of photo
     * @param idRoom id room of chat room
     */
    private suspend fun uploadPhoto(context: Context, uri: Uri, idRoom: List<String>): String? {
        return suspendCoroutine { continuation ->
            try {
                val bytes = context.compressImage(uri)
                val fileName = "${UUID.randomUUID()}.jpg"
                // Firebase path: photo/<roomId.toString()>/*
                val folder = "$PATH_PHOTO/$idRoom"

                CloudinaryManager.uploadBytes(
                    fileBytes = bytes,
                    fileName = fileName,
                    mimeType = "image/jpeg",
                    folder = folder,
                    onSuccess = { url ->
                        continuation.resume(url)
                    },
                    onFailure = { e ->
                        continuation.resumeWithException(e)
                    }
                )
            } catch (t: Throwable) {
                continuation.resumeWithException(t)
            }
        }
    }

    /**
     * This function is used to upload audio to the Storage Firebase
     * @param roomId id room of audio
     * @param uriAudio uri of audio
     * @param success callback when upload is successful
     */
    fun uploadAudio(roomId: List<String>, uriAudio: Uri, success: (String) -> Unit) {
        val audioFilePath = uriAudio.path
        val audioFile = audioFilePath?.let { File(it) }
        val bytes = audioFile?.takeIf { it.exists() }?.readBytes()

        if (bytes == null) {
            Log.e(
                "Check fail uploadAudio Cloudinary",
                "uploadAudio failed: cannot read file from uri=$uriAudio path=$audioFilePath"
            )
            return
        }

        val fileName = "${UUID.randomUUID()}.mp3"
        // Firebase path: audios/<roomId.toString()>/*
        val folder = "$PATH_AUDIO/$roomId"

        CloudinaryManager.uploadBytes(
            fileBytes = bytes,
            fileName = fileName,
            mimeType = "audio/mpeg",
            folder = folder,
            onSuccess = success,
            onFailure = { e ->
                Log.e("Check fail uploadAudio Cloudinary", "uploadAudio failed: ${e.message}", e)
            }
        )
    }

    /**
     * This function is used to remove message from the FireStore database
     * @param conversation data conversation
     * @param userId key auth of user
     * @param time time message sent
     */
    fun removeMessage(conversation: Conversation, userId: String, time: String) {
        val idRoom = listOf(conversation.friendId, userId).sorted()
        db.collection(PATH_MESSAGE)
            .document(idRoom.toString())
            .collection(PATH_CHAT)
            .document(time)
            .delete()
    }

    /**
     * This function is used to update photo cover of user to the FireStore database
     * @param userId key auth of user
     * @param imageCover data image cover
     */
    fun updateImageCover(userId: String, imageCover: String) {
        db.collection(PATH_USER)
            .document(userId)
            .update(PATH_IMAGE_COVER, imageCover)
    }

    /**
     * This function is used to search friend from the FireStore database
     * @param queryText query text search
     * @param success callback when query is successful
     */
    fun searchFriend(queryText: String, success: (ArrayList<User>) -> Unit) {
        val queryLowerCase = removeAccent(queryText.lowercase())

        db.collection(PATH_USER)
            .get()
            .addOnSuccessListener { result ->
                // toObject() does not include the document ID, so set keyAuth manually
                val friends = result.documents.mapNotNull { doc ->
                    val data = doc.data ?: return@mapNotNull null
                    User(
                        name = data["name"]?.toString() ?: "",
                        email = data["email"]?.toString() ?: "",
                        avatar = data["avatar"]?.toString() ?: "",
                        imageCover = data["imageCover"]?.toString() ?: "",
                        keyAuth = doc.id
                    )
                }.filter { friend ->
                    val nameFriendAccent = removeAccent(friend.name?.lowercase().toString())
                    nameFriendAccent.contains(queryLowerCase)
                }
                if (queryText.isNotEmpty()) {
                    success.invoke(ArrayList(friends))
                } else {
                    success.invoke(arrayListOf())
                }
            }

    }

    /**
     * This function is used to release emotion from the FireStore database
     * @param time time message sent
     * @param idRoom id room of chat room
     * @param data data emotion
     */
    fun releaseEmotion(time: String, idRoom: String, data: Emotion) {
        db.collection(PATH_MESSAGE)
            .document(idRoom)
            .collection(PATH_CHAT)
            .document(time)
            .set(
                mapOf(PATH_EMOTION to data),
                SetOptions.mergeFields(PATH_EMOTION)
            )
    }

    /**
     * This function is used to update typing message for conversation
     * @param userId key auth of user
     * @param friendId key auth of friend
     * @param typing boolean value to indicate if the user is typing
     */
    fun updateTypingMessage(userId: String, friendId: String, typing: Boolean) {
        db.collection("Conversation${userId}")
            .document(friendId)
            .update(PATH_TYPING, typing)
    }

    /**
     * Send a friend request from [fromId] to [toId].
     * Checks for an existing pending request before creating a new one.
     */
    fun sendFriendRequest(
        fromId: String,
        fromName: String,
        fromAvatar: String,
        toId: String,
        toName: String = "",
        toAvatar: String = "",
        success: () -> Unit,
        failure: (String) -> Unit
    ) {
        db.collection(PATH_FRIEND_REQUESTS)
            .whereEqualTo("fromId", fromId)
            .whereEqualTo("toId", toId)
            .whereEqualTo("status", FriendRequest.STATUS_PENDING)
            .get()
            .addOnSuccessListener { result ->
                if (!result.isEmpty) {
                    failure.invoke("Đã gửi lời mời kết bạn rồi")
                    return@addOnSuccessListener
                }
                val docRef = db.collection(PATH_FRIEND_REQUESTS).document()
                val request = FriendRequest(
                    requestId = docRef.id,
                    fromId = fromId,
                    toId = toId,
                    fromName = fromName,
                    fromAvatar = fromAvatar,
                    toName = toName,
                    toAvatar = toAvatar,
                    status = FriendRequest.STATUS_PENDING,
                    createdAt = System.currentTimeMillis()
                )
                docRef.set(request)
                    .addOnSuccessListener { success.invoke() }
                    .addOnFailureListener { failure.invoke(it.message.toString()) }
            }
            .addOnFailureListener { failure.invoke(it.message.toString()) }
    }

    /**
     * Real-time listener for incoming pending friend requests for [userId].
     */
    fun getIncomingFriendRequests(
        userId: String,
        success: (List<FriendRequest>) -> Unit,
        failure: (String) -> Unit
    ) {
        db.collection(PATH_FRIEND_REQUESTS)
            .whereEqualTo("toId", userId)
            .whereEqualTo("status", FriendRequest.STATUS_PENDING)
            .addSnapshotListener { value, error ->
                if (error != null) {
                    failure.invoke(error.message.toString())
                    return@addSnapshotListener
                }
                val list = value?.documents
                    ?.mapNotNull { it.toObject(FriendRequest::class.java) }
                    ?: emptyList()
                success.invoke(list)
            }
    }

    /**
     * Real-time listener for outgoing pending friend requests sent by [userId].
     */
    fun getOutgoingFriendRequests(
        userId: String,
        success: (List<FriendRequest>) -> Unit,
        failure: (String) -> Unit
    ) {
        db.collection(PATH_FRIEND_REQUESTS)
            .whereEqualTo("fromId", userId)
            .whereEqualTo("status", FriendRequest.STATUS_PENDING)
            .addSnapshotListener { value, error ->
                if (error != null) {
                    failure.invoke(error.message.toString())
                    return@addSnapshotListener
                }
                val list = value?.documents
                    ?.mapNotNull { it.toObject(FriendRequest::class.java) }
                    ?: emptyList()
                success.invoke(list)
            }
    }

    /**
     * Accept a friend request. Uses a batch write to atomically:
     * - Update request status to "accepted"
     * - Add both users to each other's friends subcollection
     */
    fun acceptFriendRequest(
        request: FriendRequest,
        myName: String,
        myAvatar: String,
        success: () -> Unit,
        failure: (String) -> Unit
    ) {
        val batch = db.batch()

        val requestRef = db.collection(PATH_FRIEND_REQUESTS).document(request.requestId)
        batch.update(requestRef, "status", FriendRequest.STATUS_ACCEPTED)

        val myFriendRef = db.collection(PATH_USER).document(request.toId)
            .collection(PATH_FRIENDS).document(request.fromId)
        batch.set(
            myFriendRef, Friend(
                name = request.fromName,
                avatar = request.fromAvatar,
                keyAuth = request.fromId,
                since = System.currentTimeMillis()
            )
        )

        // Accepter on sender's phone book: prefer name/avatar stored on the request when sent
        // (avoids empty name if getInfoUser snapshot was incomplete — empty name is omitted from
        // PhoneBook because grouping uses capitalLetters on friend.name).
        val accepterName = request.toName.ifBlank { myName }
        val accepterAvatar = request.toAvatar.ifBlank { myAvatar }

        val theirFriendRef = db.collection(PATH_USER).document(request.fromId)
            .collection(PATH_FRIENDS).document(request.toId)
        batch.set(
            theirFriendRef, Friend(
                name = accepterName,
                avatar = accepterAvatar,
                keyAuth = request.toId,
                since = System.currentTimeMillis()
            )
        )

        // Mirror sendMessage paths: Conversation{me}/{friendDocId}
        val time = DateUtils.getTimeCurrent()
        val becomeFriendsMsg = "Hai bạn đã trở thành bạn bè"

        val convSenderRef =
            db.collection("Conversation${request.fromId}").document(request.toId)
        batch.set(
            convSenderRef,
            Conversation(
                friendId = request.toId,
                friendImage = accepterAvatar,
                message = becomeFriendsMsg,
                name = accepterName,
                person = "Bạn",
                sender = request.fromId,
                time = time,
                numberUnSeen = 0,
                typing = false
            )
        )

        val convAccepterRef =
            db.collection("Conversation${request.toId}").document(request.fromId)
        batch.set(
            convAccepterRef,
            Conversation(
                friendId = request.fromId,
                friendImage = request.fromAvatar,
                message = becomeFriendsMsg,
                name = request.fromName,
                person = request.fromName,
                sender = request.fromId,
                time = time,
                numberUnSeen = 0,
                typing = false
            )
        )

        batch.commit()
            .addOnSuccessListener { success.invoke() }
            .addOnFailureListener { failure.invoke(it.message.toString()) }
    }

    /**
     * Reject a friend request by updating its status to "rejected".
     */
    fun rejectFriendRequest(
        requestId: String,
        success: () -> Unit,
        failure: (String) -> Unit
    ) {
        db.collection(PATH_FRIEND_REQUESTS).document(requestId)
            .update("status", FriendRequest.STATUS_REJECTED)
            .addOnSuccessListener { success.invoke() }
            .addOnFailureListener { failure.invoke(it.message.toString()) }
    }

    /**
     * Cancel an outgoing friend request sent from [fromId] to [toId].
     * Deletes the pending request document from Firestore.
     */
    fun cancelFriendRequest(
        fromId: String,
        toId: String,
        success: () -> Unit,
        failure: (String) -> Unit
    ) {
        db.collection(PATH_FRIEND_REQUESTS)
            .whereEqualTo("fromId", fromId)
            .whereEqualTo("toId", toId)
            .whereEqualTo("status", FriendRequest.STATUS_PENDING)
            .get()
            .addOnSuccessListener { result ->
                if (result.isEmpty) {
                    success.invoke()
                    return@addOnSuccessListener
                }
                val batch = db.batch()
                result.documents.forEach { batch.delete(it.reference) }
                batch.commit()
                    .addOnSuccessListener { success.invoke() }
                    .addOnFailureListener { failure.invoke(it.message.toString()) }
            }
            .addOnFailureListener { failure.invoke(it.message.toString()) }
    }

    /**
     * Real-time listener for the friends list of [userId].
     */
    fun getFriends(
        userId: String,
        success: (List<Friend>) -> Unit,
        failure: (String) -> Unit
    ) {
        db.collection(PATH_USER).document(userId)
            .collection(PATH_FRIENDS)
            .addSnapshotListener { value, error ->
                if (error != null) {
                    failure.invoke(error.message.toString())
                    return@addSnapshotListener
                }
                val list = value?.documents
                    ?.mapNotNull { it.toObject(Friend::class.java) }
                    ?: emptyList()
                success.invoke(list)
            }
    }

    /**
     * Check the friendship status between [myId] and [targetId].
     * Returns one of: "friend", "pending_sent", "pending_received", "none"
     */
    fun getFriendshipStatus(
        myId: String,
        targetId: String,
        result: (String) -> Unit
    ) {
        db.collection(PATH_USER).document(myId)
            .collection(PATH_FRIENDS).document(targetId)
            .get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    result.invoke("friend")
                    return@addOnSuccessListener
                }
                db.collection(PATH_FRIEND_REQUESTS)
                    .whereEqualTo("fromId", myId)
                    .whereEqualTo("toId", targetId)
                    .whereEqualTo("status", FriendRequest.STATUS_PENDING)
                    .get()
                    .addOnSuccessListener { sent ->
                        if (!sent.isEmpty) {
                            result.invoke("pending_sent")
                            return@addOnSuccessListener
                        }
                        db.collection(PATH_FRIEND_REQUESTS)
                            .whereEqualTo("fromId", targetId)
                            .whereEqualTo("toId", myId)
                            .whereEqualTo("status", FriendRequest.STATUS_PENDING)
                            .get()
                            .addOnSuccessListener { received ->
                                if (!received.isEmpty) result.invoke("pending_received")
                                else result.invoke("none")
                            }
                            .addOnFailureListener { result.invoke("none") }
                    }
                    .addOnFailureListener { result.invoke("none") }
            }
            .addOnFailureListener { result.invoke("none") }
    }

    /**
     * Save a user to the current user's search history.
     * Uses the friend's keyAuth as the document ID so duplicate entries are overwritten.
     */
    fun saveSearchHistory(
        myId: String,
        user: User,
        success: () -> Unit = {},
        failure: (String) -> Unit = {}
    ) {
        val data = hashMapOf(
            "name" to (user.name ?: ""),
            "avatar" to (user.avatar ?: ""),
            "keyAuth" to (user.keyAuth ?: ""),
            "searchedAt" to System.currentTimeMillis()
        )
        db.collection(PATH_SEARCH_HISTORY)
            .document(myId)
            .collection(PATH_ITEMS)
            .document(user.keyAuth ?: return)
            .set(data)
            .addOnSuccessListener { success.invoke() }
            .addOnFailureListener { failure.invoke(it.message.toString()) }
    }

    /**
     * Fetch the current user's search history ordered by most recent first.
     */
    fun getSearchHistory(
        myId: String,
        success: (List<User>) -> Unit,
        failure: (String) -> Unit
    ) {
        db.collection(PATH_SEARCH_HISTORY)
            .document(myId)
            .collection(PATH_ITEMS)
            .orderBy("searchedAt", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { result ->
                val list = result.documents.mapNotNull { doc ->
                    val data = doc.data ?: return@mapNotNull null
                    User(
                        name = data["name"]?.toString() ?: "",
                        avatar = data["avatar"]?.toString() ?: "",
                        keyAuth = data["keyAuth"]?.toString() ?: doc.id
                    )
                }
                success.invoke(list)
            }
            .addOnFailureListener { failure.invoke(it.message.toString()) }
    }

    fun getSticker(sticker: Sticker, onSuccess: (List<String>) -> Unit) {
        if (sticker == Sticker.CONGRATULATION) {
            Log.e("getSticker", "init")
        }
        val stickerPaths = arrayListOf<String>()
        db.collection(PATH_STICKER)
            .document(sticker.value)
            .get()
            .addOnSuccessListener {
                it.data?.map { doc ->
                    if (sticker == Sticker.CONGRATULATION) {
                        Log.e("getSticker", "key: ${doc.key}, value: ${doc.value}")
                    }
                    stickerPaths.add(doc.key)
                }
                onSuccess.invoke(stickerPaths)
            }
            .addOnFailureListener {
                Log.e("getSticker", it.message.toString())
            }
    }

    // region Diary posts (Firestore)

    /**
     * Số bài tối đa lấy mỗi chunk (whereIn). Không dùng orderBy trên server để tránh bắt buộc composite index;
     * sắp xếp theo [DiaryPost.createdAtMillis] khi merge.
     */
    private const val DIARY_FEED_LIMIT_PER_CHUNK = 80

    /** Số bài tối đa sau khi merge + sort (client). */
    private const val DIARY_FEED_MAX_DISPLAY = 50

    /**
     * Local file URIs → Cloudinary URLs; [http/https] giữ nguyên (ảnh đã upload khi sửa bài).
     */
    private fun resolveDiaryImageUrls(
        context: Context,
        uris: List<Uri>,
        onDone: (List<String>) -> Unit,
        failure: (String) -> Unit
    ) {
        if (uris.isEmpty()) {
            onDone(emptyList())
            return
        }
        val urls = mutableListOf<String>()
        fun next(index: Int) {
            if (index >= uris.size) {
                onDone(urls)
                return
            }
            val uri = uris[index]
            val scheme = uri.scheme?.lowercase()
            if (scheme == "http" || scheme == "https") {
                urls.add(uri.toString())
                next(index + 1)
            } else {
                uploadImage(
                    context,
                    uri,
                    success = { url ->
                        urls.add(url)
                        next(index + 1)
                    },
                    failure = failure
                )
            }
        }
        next(0)
    }

    /**
     * Uploads local images then creates [DiaryPostFirestore.COLLECTION] document.
     */
    fun createDiaryPost(
        context: Context,
        authorId: String,
        authorName: String,
        authorAvatarUrl: String,
        content: String,
        localImageUris: List<Uri>,
        linkPreview: DiaryLinkPreview? = null,
        success: (postId: String) -> Unit,
        failure: (String) -> Unit
    ) {
        val col = db.collection(DiaryPostFirestore.COLLECTION)
        fun writePost(imageUrls: List<String>) {
            val doc = col.document()
            val data = hashMapOf<String, Any>(
                DiaryPostFirestore.FIELD_AUTHOR_ID to authorId,
                DiaryPostFirestore.FIELD_AUTHOR_NAME to authorName,
                DiaryPostFirestore.FIELD_AUTHOR_AVATAR to authorAvatarUrl,
                DiaryPostFirestore.FIELD_CONTENT to content,
                DiaryPostFirestore.FIELD_IMAGE_URLS to imageUrls,
                DiaryPostFirestore.FIELD_CREATED_AT to FieldValue.serverTimestamp(),
                DiaryPostFirestore.FIELD_LIKE_COUNT to 0,
                DiaryPostFirestore.FIELD_COMMENT_COUNT to 0
            )
            if (linkPreview != null) {
                data[DiaryPostFirestore.FIELD_LINK_PREVIEW] = hashMapOf(
                    DiaryPostFirestore.LINK_FIELD_URL to linkPreview.url,
                    DiaryPostFirestore.LINK_FIELD_TITLE to linkPreview.title,
                    DiaryPostFirestore.LINK_FIELD_DESCRIPTION to linkPreview.description,
                    DiaryPostFirestore.LINK_FIELD_IMAGE_URL to linkPreview.imageUrl.orEmpty()
                )
            }
            doc.set(data)
                .addOnSuccessListener { success(doc.id) }
                .addOnFailureListener {
                    failure(it.message ?: context.getString(R.string.error_save_post))
                }
        }
        resolveDiaryImageUrls(context, localImageUris, onDone = { writePost(it) }, failure = failure)
    }

    fun getDiaryPost(
        postId: String,
        success: (DiaryPost) -> Unit,
        failure: (String) -> Unit
    ) {
        if (postId.isBlank()) {
            failure(MyApplication.appContext.getString(R.string.error_load_post))
            return
        }
        db.collection(DiaryPostFirestore.COLLECTION).document(postId).get()
            .addOnSuccessListener { doc ->
                if (!doc.exists()) {
                    failure(MyApplication.appContext.getString(R.string.error_load_post))
                    return@addOnSuccessListener
                }
                val post = DiaryPostFirestore.fromDocument(doc)
                if (post != null) success(post)
                else failure(MyApplication.appContext.getString(R.string.error_load_post))
            }
            .addOnFailureListener {
                failure(it.message ?: MyApplication.appContext.getString(R.string.error_load_post))
            }
    }

    fun updateDiaryPost(
        context: Context,
        postId: String,
        editorUserId: String,
        content: String,
        imageUris: List<Uri>,
        linkPreview: DiaryLinkPreview?,
        success: () -> Unit,
        failure: (String) -> Unit
    ) {
        val ref = db.collection(DiaryPostFirestore.COLLECTION).document(postId)
        ref.get()
            .addOnSuccessListener { snap ->
                if (!snap.exists()) {
                    failure(context.getString(R.string.error_load_post))
                    return@addOnSuccessListener
                }
                val authorId = snap.getString(DiaryPostFirestore.FIELD_AUTHOR_ID).orEmpty()
                if (authorId != editorUserId) {
                    failure(context.getString(R.string.error_update_post))
                    return@addOnSuccessListener
                }
                resolveDiaryImageUrls(
                    context,
                    imageUris,
                    onDone = { urls ->
                        val updates = hashMapOf<String, Any>(
                            DiaryPostFirestore.FIELD_CONTENT to content,
                            DiaryPostFirestore.FIELD_IMAGE_URLS to urls,
                            DiaryPostFirestore.FIELD_UPDATED_AT to FieldValue.serverTimestamp()
                        )
                        if (linkPreview != null) {
                            updates[DiaryPostFirestore.FIELD_LINK_PREVIEW] = hashMapOf(
                                DiaryPostFirestore.LINK_FIELD_URL to linkPreview.url,
                                DiaryPostFirestore.LINK_FIELD_TITLE to linkPreview.title,
                                DiaryPostFirestore.LINK_FIELD_DESCRIPTION to linkPreview.description,
                                DiaryPostFirestore.LINK_FIELD_IMAGE_URL to linkPreview.imageUrl.orEmpty()
                            )
                        } else {
                            updates[DiaryPostFirestore.FIELD_LINK_PREVIEW] = FieldValue.delete()
                        }
                        ref.update(updates)
                            .addOnSuccessListener { success() }
                            .addOnFailureListener {
                                failure(it.message ?: context.getString(R.string.error_update_post))
                            }
                    },
                    failure = failure
                )
            }
            .addOnFailureListener {
                failure(it.message ?: context.getString(R.string.error_load_post))
            }
    }

    fun deleteDiaryPost(
        postId: String,
        editorUserId: String,
        success: () -> Unit,
        failure: (String) -> Unit
    ) {
        val ref = db.collection(DiaryPostFirestore.COLLECTION).document(postId)
        ref.get()
            .addOnSuccessListener { snap ->
                if (!snap.exists()) {
                    success()
                    return@addOnSuccessListener
                }
                val authorId = snap.getString(DiaryPostFirestore.FIELD_AUTHOR_ID).orEmpty()
                if (authorId != editorUserId) {
                    failure(MyApplication.appContext.getString(R.string.error_delete_post))
                    return@addOnSuccessListener
                }
                ref.delete()
                    .addOnSuccessListener { success() }
                    .addOnFailureListener {
                        failure(it.message ?: MyApplication.appContext.getString(R.string.error_delete_post))
                    }
            }
            .addOnFailureListener {
                failure(it.message ?: MyApplication.appContext.getString(R.string.error_delete_post))
            }
    }

    /**
     * Real-time diary feed: posts where [authorId] is in `me + friends`.
     * Firestore [Query.whereIn] allows at most 10 values — friends are chunked.
     */
    fun observeDiaryFeed(
        userId: String,
        onPosts: (List<DiaryPost>) -> Unit,
        onError: (String) -> Unit
    ): () -> Unit {
        val postsCol = db.collection(DiaryPostFirestore.COLLECTION)
        val chunkPosts = mutableMapOf<Int, Map<String, DiaryPost>>()
        val likedByMe = mutableMapOf<String, Boolean>()
        val likeRegs = mutableMapOf<String, ListenerRegistration>()
        val postChunkRegs = mutableListOf<ListenerRegistration>()
        var friendsReg: ListenerRegistration? = null

        fun mergeAndEmit() {
            val merged = linkedMapOf<String, DiaryPost>()
            chunkPosts.values.forEach { map ->
                map.forEach { (id, post) -> merged[id] = post }
            }
            val list = merged.values
                .map { p -> p.copy(likedByMe = likedByMe[p.id] ?: false) }
                .sortedByDescending { it.createdAtMillis }
                .take(DIARY_FEED_MAX_DISPLAY)
            onPosts(list)
        }

        fun syncMyLikeListeners(visiblePostIds: Set<String>) {
            val toRemove = likeRegs.keys - visiblePostIds
            toRemove.forEach { pid ->
                likeRegs.remove(pid)?.remove()
            }
            val toAdd = visiblePostIds - likeRegs.keys
            toAdd.forEach { postId ->
                val reg = postsCol.document(postId)
                    .collection(DiaryPostFirestore.SUB_LIKES)
                    .document(userId)
                    .addSnapshotListener { snap, _ ->
                        likedByMe[postId] = snap?.exists() == true
                        mergeAndEmit()
                    }
                likeRegs[postId] = reg
            }
        }

        fun mergeKeys(): Set<String> {
            val keys = mutableSetOf<String>()
            chunkPosts.values.forEach { m -> keys.addAll(m.keys) }
            return keys
        }

        fun attachPostListeners(authorIds: List<String>) {
            postChunkRegs.forEach { it.remove() }
            postChunkRegs.clear()
            chunkPosts.clear()
            if (authorIds.isEmpty()) {
                mergeAndEmit()
                syncMyLikeListeners(emptySet())
                return
            }
            // whereIn tối đa 10 giá trị — chunk theo authorId.
            // Không orderBy trên Firestore: tránh lỗi index + listener gọi onError lặp khi chưa deploy composite index.
            val chunks = authorIds.distinct().chunked(10)
            chunks.forEachIndexed { chunkIndex, chunk ->
                val reg = postsCol
                    .whereIn(DiaryPostFirestore.FIELD_AUTHOR_ID, chunk)
                    .limit(DIARY_FEED_LIMIT_PER_CHUNK.toLong())
                    .addSnapshotListener { snap, err ->
                        if (err != null) {
                            onError(
                                err.message ?: MyApplication.appContext.getString(R.string.error_load_posts)
                            )
                            return@addSnapshotListener
                        }
                        val map = snap?.documents?.mapNotNull { doc ->
                            DiaryPostFirestore.fromDocument(doc, likedByMe = false)
                                ?.let { d -> d.id to d }
                        }?.toMap().orEmpty()
                        chunkPosts[chunkIndex] = map
                        mergeAndEmit()
                        syncMyLikeListeners(mergeKeys())
                    }
                    postChunkRegs.add(reg)
            }
            mergeAndEmit()
            syncMyLikeListeners(mergeKeys())
        }

        friendsReg = db.collection(PATH_USER).document(userId)
            .collection(PATH_FRIENDS)
            .addSnapshotListener { value, error ->
                if (error != null) {
                    onError(
                        error.message ?: MyApplication.appContext.getString(R.string.error_friends_list)
                    )
                    return@addSnapshotListener
                }
                val friendIds = value?.documents?.map { it.id }.orEmpty()
                val authorIds = (listOf(userId) + friendIds).distinct().filter { it.isNotBlank() }
                likeRegs.values.forEach { it.remove() }
                likeRegs.clear()
                likedByMe.clear()
                attachPostListeners(authorIds)
            }

        return {
            friendsReg?.remove()
            friendsReg = null
            postChunkRegs.forEach { it.remove() }
            postChunkRegs.clear()
            likeRegs.values.forEach { it.remove() }
            likeRegs.clear()
            chunkPosts.clear()
            likedByMe.clear()
        }
    }

    fun toggleDiaryPostLike(
        postId: String,
        userId: String,
        currentlyLiked: Boolean,
        success: () -> Unit,
        failure: (String) -> Unit
    ) {
        val postRef = db.collection(DiaryPostFirestore.COLLECTION).document(postId)
        val likeRef = postRef.collection(DiaryPostFirestore.SUB_LIKES).document(userId)
        val batch = db.batch()
        if (currentlyLiked) {
            batch.delete(likeRef)
            batch.update(postRef, DiaryPostFirestore.FIELD_LIKE_COUNT, FieldValue.increment(-1))
        } else {
            batch.set(likeRef, mapOf("createdAt" to FieldValue.serverTimestamp()))
            batch.update(postRef, DiaryPostFirestore.FIELD_LIKE_COUNT, FieldValue.increment(1))
        }
        batch.commit()
            .addOnSuccessListener { success() }
            .addOnFailureListener {
                failure(
                    it.message ?: MyApplication.appContext.getString(R.string.error_toggle_like)
                )
            }
    }

    fun addDiaryComment(
        postId: String,
        authorId: String,
        authorName: String,
        authorAvatarUrl: String,
        text: String,
        success: (commentId: String) -> Unit,
        failure: (String) -> Unit
    ) {
        val postRef = db.collection(DiaryPostFirestore.COLLECTION).document(postId)
        val newRef = postRef.collection(DiaryPostFirestore.SUB_COMMENTS).document()
        val data = hashMapOf<String, Any>(
            DiaryPostFirestore.COMMENT_FIELD_AUTHOR_ID to authorId,
            DiaryPostFirestore.COMMENT_FIELD_AUTHOR_NAME to authorName,
            DiaryPostFirestore.COMMENT_FIELD_AUTHOR_AVATAR to authorAvatarUrl,
            DiaryPostFirestore.COMMENT_FIELD_TEXT to text.trim(),
            DiaryPostFirestore.COMMENT_FIELD_CREATED_AT to FieldValue.serverTimestamp()
        )
        val batch = db.batch()
        batch.set(newRef, data)
        batch.update(postRef, DiaryPostFirestore.FIELD_COMMENT_COUNT, FieldValue.increment(1))
        batch.commit()
            .addOnSuccessListener { success(newRef.id) }
            .addOnFailureListener {
                failure(
                    it.message ?: MyApplication.appContext.getString(R.string.error_send_comment)
                )
            }
    }

    fun observeDiaryComments(
        postId: String,
        onUpdate: (List<DiaryPostComment>) -> Unit,
        onError: (String) -> Unit
    ): ListenerRegistration {
        return db.collection(DiaryPostFirestore.COLLECTION).document(postId)
            .collection(DiaryPostFirestore.SUB_COMMENTS)
            .orderBy(DiaryPostFirestore.COMMENT_FIELD_CREATED_AT, Query.Direction.ASCENDING)
            .limit(100)
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    onError(
                        err.message ?: MyApplication.appContext.getString(R.string.error_load_comments)
                    )
                    return@addSnapshotListener
                }
                val list = snap?.documents?.mapNotNull { doc ->
                    DiaryPostFirestore.commentFromDocument(postId, doc)
                }.orEmpty()
                onUpdate(list)
            }
    }

    // endregion Diary posts
}