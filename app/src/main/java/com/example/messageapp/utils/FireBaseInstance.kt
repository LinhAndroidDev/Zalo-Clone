package com.example.messageapp.utils

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
}