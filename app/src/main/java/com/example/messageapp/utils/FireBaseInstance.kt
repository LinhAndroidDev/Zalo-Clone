package com.example.messageapp.utils

import com.example.messageapp.model.Conversation
import com.example.messageapp.model.Message
import com.example.messageapp.model.User
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.util.HashMap

object FireBaseInstance {
    private val db by lazy { Firebase.firestore }

    private const val USERS = "users"

    /**
     * This function is used to get all users from the Firestore database
     */
    fun getUsers(success: (QuerySnapshot) -> Unit, failure: (String) -> Unit) {
        db.collection(USERS).get().addOnSuccessListener { result ->
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
        db.collection(USERS).add(user)
            .addOnSuccessListener {
                success.invoke("Create Successful")
            }.addOnFailureListener { e ->
                failure.invoke(e.message.toString())
            }
    }

    fun getMessage(
        idRoom: String,
        success: (QuerySnapshot?) -> Unit,
        failure: (String) -> Unit
    ) {
        db.collection("messages")
            .document(idRoom)
            .collection("chats")
            .orderBy("time", Query.Direction.ASCENDING)
            .addSnapshotListener { value, error ->
                if(error != null) {
                    failure.invoke(error.message.toString())
                    return@addSnapshotListener
                }
                success.invoke(value)
            }
    }

    fun sendMessage(
        message: Message,
        keyAuth: String,
        time: String,
        friend: User,
        nameSender: String,
        avatarSender: String
    ) {
        val idRoom = listOf(friend.keyAuth.toString(), keyAuth).sorted()

        db.collection("messages")
            .document(idRoom.toString())
            .collection("chats")
            .document(time)
            .set(message)
            .addOnCompleteListener {

                val conversationData = Conversation(
                    friendId = friend.keyAuth.toString(),
                    friendImage = friend.avatar.toString(),
                    message = message.message,
                    name = friend.name.toString(),
                    person = "Bạn",
                    sender = keyAuth,
                    time = time
                )

                db.collection("Conversation${keyAuth}")
                    .document(friend.keyAuth.toString())
                    .set(conversationData)

                val conversationFriend = Conversation(
                    friendId = keyAuth,
                    friendImage = avatarSender,
                    message = message.message,
                    name = nameSender,
                    person = nameSender,
                    sender = friend.keyAuth.toString(),
                    time = time
                )

                db.collection("Conversation${friend.keyAuth.toString()}")
                    .document(keyAuth)
                    .set(conversationFriend)
            }
    }

    fun getListConversation(keyAuth: String, success: (QuerySnapshot?) -> Unit, failure: (String) -> Unit) {
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
}