package com.example.messageapp.utils

import android.util.Log
import com.example.messageapp.model.Conversation
import com.example.messageapp.model.Message
import com.example.messageapp.model.User
import com.example.messageapp.remote.ApiClient
import com.example.messageapp.remote.Token
import com.example.messageapp.remote.request.Data
import com.example.messageapp.remote.request.Notification
import com.example.messageapp.remote.request.NotificationData
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.HashMap

object FireBaseInstance {
    private val db by lazy { Firebase.firestore }

    private const val PATH_USER = "users"
    private const val PATH_MESSAGE = "messages"
    private const val PATH_CHAT = "chats"
    private const val PATH_TOKEN = "Tokens"

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
     * This function is used to get all users from the Firestore database
     */
    fun getUsers(success: (QuerySnapshot) -> Unit, failure: (String) -> Unit) {
        db.collection(PATH_USER).get().addOnSuccessListener { result ->
            success.invoke(result)
        }.addOnFailureListener { e ->
            failure.invoke(e.message.toString())
        }
    }

    /**
     * This function is used to add a new user to the Firestore database
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
     * This function is used to get all messages from the Firestore database
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
     * This function is used to send a message to the Firestore database
     * + Here we send a message and create 2 conversations between the sender and the receiver
     * + Then send a notification to the receiver via the receiver's token.
     */
    fun sendMessage(
        message: Message,
        keyAuth: String,
        time: String,
        friend: User,
        nameSender: String,
        avatarSender: String
    ) {
        val idRoom = listOf(friend.keyAuth.toString(), keyAuth).sorted()

        db.collection(PATH_MESSAGE)
            .document(idRoom.toString())
            .collection(PATH_CHAT)
            .document(time)
            .set(message)
            .addOnCompleteListener {

                //Get token of receiver to send notification message to receiver
                getTokenMessage(friend.keyAuth.toString(),
                    success = { token ->
                        val notificationData = NotificationData(
                            token = token,
                            Data(nameSender, message.message)
                        )

                        ApiClient.api?.sendMessage(Notification(message = notificationData))
                            ?.enqueue(object : Callback<Notification> {
                                override fun onFailure(call: Call<Notification>, t: Throwable) {
                                    Log.e("Send Message", "Send Fail")
                                }

                                override fun onResponse(
                                    call: Call<Notification>,
                                    response: Response<Notification>
                                ) {
                                    Log.e("Send Message", "Send Successful")
                                }

                            })
                    },
                    failure = {

                    })

                //Create Data Conversation For Sender
                val conversationData = Conversation(
                    friendId = friend.keyAuth.toString(),
                    friendImage = friend.avatar.toString(),
                    message = message.message,
                    name = friend.name.toString(),
                    person = "Bạn",
                    sender = keyAuth,
                    time = time
                )

                //Create Conversation For Sender
                db.collection("Conversation${keyAuth}")
                    .document(friend.keyAuth.toString())
                    .set(conversationData)

                //Create Data Conversation For Receiver
                val conversationFriend = Conversation(
                    friendId = keyAuth,
                    friendImage = avatarSender,
                    message = message.message,
                    name = nameSender,
                    person = nameSender,
                    sender = friend.keyAuth.toString(),
                    time = time
                )

                //Create Conversation For Receiver
                db.collection("Conversation${friend.keyAuth.toString()}")
                    .document(keyAuth)
                    .set(conversationFriend)
            }
    }

    /**
     * This function is used to get all conversations from the Firestore database
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
     * This function is used to save token of user to the Firestore database
     */
    fun saveTokenMessage(keyAuth: String, data: HashMap<String, String>) {
        db.collection(PATH_TOKEN).document(keyAuth).set(data).addOnSuccessListener {
        }
    }

    /**
     * This function is used to get token of receiver from the Firestore database
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
}