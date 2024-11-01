package com.example.messageapp.utils

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.messageapp.model.Conversation
import com.example.messageapp.model.Message
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
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.HashMap
import java.util.UUID

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
        keyAuth: String,
        time: String,
        conversation: Conversation,
        nameSender: String,
        success: () -> Unit
    ) {
        val idRoom = listOf(conversation.friendId, keyAuth).sorted()

        db.collection(PATH_MESSAGE)
            .document(idRoom.toString())
            .collection(PATH_CHAT)
            .document(time)
            .set(message)
            .addOnCompleteListener {

                //Get token of receiver to send notification message to receiver
                getTokenMessage(
                    conversation.friendId,
                    success = { token ->
                        val notificationNotification = NotificationData(
                            token = token,
                            data = Data(title = nameSender, body = message.message, senderId = keyAuth)
                        )

                        ApiClient.api?.sendMessage(MessageRequest(message = notificationNotification))
                            ?.enqueue(object : Callback<MessageRequest> {
                                override fun onFailure(call: Call<MessageRequest>, t: Throwable) {
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

                    })

                //Create Data Conversation For Sender
                val conversationData = Conversation(
                    friendId = conversation.friendId,
                    friendImage = conversation.friendImage,
                    message = message.message,
                    name = conversation.name,
                    person = "Bạn",
                    sender = keyAuth,
                    time = time
                )

                //Create Conversation For Sender
                db.collection("Conversation${keyAuth}")
                    .document(conversation.friendId)
                    .set(conversationData)

                //Create Data Conversation For Receiver
                val conversationFriend = Conversation(
                    friendId = keyAuth,
                    message = message.message,
                    name = nameSender,
                    person = nameSender,
                    sender = keyAuth,
                    time = time
                )

                //Create Conversation For Receiver
                db.collection("Conversation${conversation.friendId}")
                    .document(keyAuth)
                    .set(conversationFriend)
            }
        success.invoke()
    }

    /**
     * This function is used to get all conversations from the FireStore database
     */
    fun getListConversation(
        keyAuth: String,
        success: (QuerySnapshot?) -> Unit,
        failure: (String) -> Unit
    ) {
        db.collection("Conversation${keyAuth}")
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
    fun saveTokenMessage(keyAuth: String, data: HashMap<String, String>) {
        db.collection(PATH_TOKEN).document(keyAuth).set(data).addOnSuccessListener {
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
    fun getInfoUser(keyAuth: String, success: (User) -> Unit) {
        db.collection(PATH_USER)
            .document(keyAuth)
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
    fun updateAvatarUser(avatar: String, keyAuth: String) {
        db.collection(PATH_USER)
            .document(keyAuth)
            .update("avatar", avatar)
    }
}