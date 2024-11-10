package com.example.messageapp.utils

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.messageapp.model.Conversation
import com.example.messageapp.model.Message
import com.example.messageapp.model.TypeMessage
import com.example.messageapp.model.User
import com.example.messageapp.remote.ApiClient
import com.example.messageapp.remote.Token
import com.example.messageapp.remote.request.Data
import com.example.messageapp.remote.request.MessageRequest
import com.example.messageapp.remote.request.NotificationData
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.HashMap
import java.util.UUID
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

object FireBaseInstance {
    private val db by lazy { Firebase.firestore }
    private val storage by lazy { Firebase.storage.reference }

    private const val PATH_USER = "users"
    private const val PATH_MESSAGE = "messages"
    private const val PATH_CHAT = "chats"
    private const val PATH_TOKEN = "Tokens"
    private const val PATH_IMAGE = "images"

    /**
     * This function is used to check the login of the user
     * + By query whereEqualTo @param email and @param password
     */
    fun checkLogin(
        email: String,
        password: String,
        success: (QuerySnapshot) -> Unit,
        failure: (String) -> Unit
    ) {
        db.collection(PATH_USER)
            .whereEqualTo("email", email)
            .whereEqualTo("password", password)
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
     * + Then send a notification to the receiver via the receiver's token.
     */
    fun sendMessage(
        message: Message,
        userId: String,
        time: String,
        conversation: Conversation,
        nameSender: String,
        type: TypeMessage = TypeMessage.MESSAGE,
        success: () -> Unit,
    ) {
        val idRoom = listOf(conversation.friendId, userId).sorted()

        db.collection(PATH_MESSAGE)
            .document(idRoom.toString())
            .collection(PATH_CHAT)
            .document(time)
            .set(message)

        getConversation(conversation.friendId, userId) { cvt ->

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
            val num = cvt.numberUnSeen + 1
            val conversationFriend = Conversation(
                friendId = userId,
                message = if (type == TypeMessage.MESSAGE) message.message else "$nameSender đã gửi ảnh cho bạn",
                name = nameSender,
                person = nameSender,
                sender = userId,
                time = time,
                numberUnSeen = num
            )

            //Create Conversation For Receiver
            db.collection("Conversation${conversation.friendId}")
                .document(userId)
                .set(conversationFriend)
        }
        success.invoke()
    }

    /**
     * This function is used to get all conversations from the FireStore database
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
     */
    fun saveTokenMessage(userId: String, data: HashMap<String, String>) {
        db.collection(PATH_TOKEN).document(userId).set(data).addOnSuccessListener {
        }
    }

    /**
     * This function is used to get token of receiver from the FireStore database
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
     * This function is used to upload image to the Storage Firebase
     */
    fun uploadImage(context: Context, uriPhoto: Uri, success: (String) -> Unit) {
        storage.child(PATH_IMAGE)
            .child(UUID.randomUUID().toString())
            .putBytes(context.compressImage(uriPhoto))
            .addOnSuccessListener { taskSnapshot->
                taskSnapshot.storage.downloadUrl.addOnSuccessListener { uri ->
                    success.invoke(uri.toString())
                }
            }.addOnFailureListener {
                Log.e("Upload Photo", "Fail")
            }
    }

    /**
     * This function is used to update avatar of user to the FireStore database
     */
    fun updateAvatarUser(avatar: String, userId: String) {
        db.collection(PATH_USER)
            .document(userId)
            .update("avatar", avatar)
    }

    /**
     * This function is used to get conversation from the FireStore database
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

    fun uploadListPhoto(
        context: Context,
        uris: ArrayList<Uri>,
        friendId: String,
        userId: String,
        process: (Pair<Int, Double>) -> Unit,
        success: (ArrayList<String>) -> Unit
    ) = CoroutineScope(Dispatchers.IO).launch {
            val idRoom = listOf(friendId, userId).sorted()
            val photos = arrayListOf<String>()
            val deferredList = uris.map { uri ->
                async {
                    val photoUrl = uploadPhoto(context, uri, idRoom)
                    photoUrl?.let { photos.add(it) }
                }
            }
            deferredList.awaitAll()
            success.invoke(photos)
        }

    private suspend fun uploadPhoto(context: Context, uri: Uri, idRoom: List<String>): String? {
        return suspendCoroutine { continuation ->
            val storageRef = storage.child("photo")
                .child(idRoom.toString())
                .child(UUID.randomUUID().toString())

            storageRef.putBytes(context.compressImage(uri))
                .addOnSuccessListener { taskSnapshot ->
                    taskSnapshot.storage.downloadUrl.addOnSuccessListener { uri ->
                        continuation.resume(uri.toString())  // Trả về URL của ảnh
                    }.addOnFailureListener {
                        continuation.resumeWithException(it)  // Đảm bảo xử lý lỗi
                    }
                }
                .addOnFailureListener {
                    continuation.resumeWithException(it)  // Đảm bảo xử lý lỗi
                }
        }
    }
}