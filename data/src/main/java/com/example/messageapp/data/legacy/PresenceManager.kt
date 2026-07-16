package com.example.messageapp.data.legacy

import android.util.Log
import com.example.messageapp.data.firestore.UserPresence
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.database
import com.google.firebase.Firebase
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PresenceManager @Inject constructor() {

    private val database: FirebaseDatabase by lazy { Firebase.database }
    private var connectedUserId: String? = null
    private var pendingConnectUserId: String? = null
    private var connectedListener: ValueEventListener? = null

    fun connect(userId: String) {
        if (userId.isBlank()) return
        connectedUserId = userId
        pendingConnectUserId = userId
        removeConnectedListener()

        val connectedRef = database.getReference(PATH_INFO_CONNECTED)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val connected = snapshot.getValue(Boolean::class.java) == true
                if (!connected) {
                    Log.d(TAG, "RTDB not connected yet, waiting…")
                    return
                }
                val uid = pendingConnectUserId ?: return
                if (uid != connectedUserId) return

                publishOnline(uid)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "connected listener cancelled: code=${error.code} ${error.message}")
            }
        }
        connectedListener = listener
        connectedRef.addValueEventListener(listener)
    }

    fun disconnect(userId: String) {
        if (userId.isBlank()) return
        if (connectedUserId == userId) {
            pendingConnectUserId = null
            connectedUserId = null
            removeConnectedListener()
        }

        val ref = database.getReference("$PATH_STATUS/$userId")
        ref.onDisconnect().cancel()
        ref.updateChildren(offlinePayload())
            .addOnSuccessListener {
                Log.d(TAG, "disconnect success for $userId")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "disconnect update failed for $userId", e)
            }
    }

    fun observePresence(userId: String, onChange: (UserPresence) -> Unit): () -> Unit {
        if (userId.isBlank()) {
            onChange(UserPresence())
            return {}
        }
        val ref = database.getReference("$PATH_STATUS/$userId")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                onChange(parsePresence(snapshot))
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "observePresence cancelled for $userId: code=${error.code} ${error.message}")
                onChange(UserPresence())
            }
        }
        ref.addValueEventListener(listener)
        return { ref.removeEventListener(listener) }
    }

    private fun publishOnline(userId: String) {
        val ref = database.getReference("$PATH_STATUS/$userId")
        ref.onDisconnect().updateChildren(offlinePayload())
            .addOnSuccessListener {
                Log.d(TAG, "onDisconnect registered for $userId")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "onDisconnect setup failed for $userId", e)
            }
        ref.updateChildren(onlinePayload())
            .addOnSuccessListener {
                Log.d(TAG, "online=true for $userId")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "connect update failed for $userId", e)
            }
    }

    private fun removeConnectedListener() {
        connectedListener?.let { listener ->
            database.getReference(PATH_INFO_CONNECTED).removeEventListener(listener)
        }
        connectedListener = null
    }

    private fun offlinePayload(): Map<String, Any> = mapOf(
        KEY_ONLINE to false,
        KEY_LAST_SEEN to ServerValue.TIMESTAMP,
    )

    private fun onlinePayload(): Map<String, Any> = mapOf(
        KEY_ONLINE to true,
        KEY_LAST_SEEN to ServerValue.TIMESTAMP,
    )

    private fun parsePresence(snapshot: DataSnapshot): UserPresence {
        if (!snapshot.exists()) return UserPresence()
        val online = snapshot.child(KEY_ONLINE).getValue(Boolean::class.java) == true
        val lastSeen = snapshot.child(KEY_LAST_SEEN).getValue(Long::class.java) ?: 0L
        return UserPresence(online = online, lastSeen = lastSeen)
    }

    companion object {
        private const val TAG = "PresenceManager"
        private const val PATH_INFO_CONNECTED = ".info/connected"
        private const val PATH_STATUS = "status"
        private const val KEY_ONLINE = "online"
        private const val KEY_LAST_SEEN = "lastSeen"
    }
}
