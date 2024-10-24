package com.example.messageapp.fragment

import android.util.Log
import android.widget.Toast
import com.example.messageapp.R
import com.example.messageapp.base.BaseFragment
import com.example.messageapp.databinding.FragmentDiscoverBinding
import com.example.messageapp.model.User
import com.example.messageapp.viewmodel.DiscoverFragmentViewModel
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class DiscoverFragment : BaseFragment<FragmentDiscoverBinding, DiscoverFragmentViewModel>() {
    override val layoutResId: Int = R.layout.fragment_discover

    private val db by lazy { Firebase.firestore }

    override fun initView() {
        super.initView()

        binding?.createData?.setOnClickListener {
            val city = hashMapOf(
                "name" to binding?.name?.text.toString(),
                "email" to binding?.email?.text.toString(),
                "password" to binding?.password?.text.toString(),
                "avatar" to ""
            )

            db.collection("users")
                .add(city)
                .addOnSuccessListener {
                    Toast.makeText(requireActivity(), "Create Successful", Toast.LENGTH_SHORT)
                        .show()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(requireActivity(), e.message, Toast.LENGTH_SHORT).show()
                }
        }

        val docRef = db.collection("users")

        docRef.addSnapshotListener { snapshot, e ->
            if (e != null) {
                Log.w("Firestore", "Listen failed.", e)
                return@addSnapshotListener
            }

            if (snapshot != null && !snapshot.isEmpty) {
                val users = ArrayList<User>()
                for (document in snapshot.documents) {
                    val data = document.data as Map<*, *>
                    users.add(
                        User(
                            data["name"].toString(),
                            data["email"].toString(),
                            data["password"].toString(),
                            keyAuth = document.id
                        )
                    ) // Lấy dữ liệu từ mỗi document và thêm vào list
                }

                // Hiển thị danh sách user hoặc xử lý dữ liệu tùy ý
                var text = ""
                users.forEach { user ->
                    text += "name: ${user.name} - email: ${user.email} - password: ${user.password} - keyAuth: ${user.keyAuth} \n\n"
                }
                binding?.tvResult?.text = text
            } else {
                Log.d("Firestore", "Current data: null")
            }
        }
    }
}