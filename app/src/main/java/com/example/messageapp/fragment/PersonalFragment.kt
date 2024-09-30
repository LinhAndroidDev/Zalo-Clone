package com.example.messageapp.fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.messageapp.R
import com.example.messageapp.base.BaseFragment
import com.example.messageapp.databinding.FragmentPersonalBinding
import com.example.messageapp.viewmodel.PersonalFragmentViewModel

class PersonalFragment : BaseFragment<FragmentPersonalBinding, PersonalFragmentViewModel>() {
    override val layoutResId = R.layout.fragment_personal
}