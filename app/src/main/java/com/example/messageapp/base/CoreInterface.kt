package com.example.messageapp.base

interface CoreInterface {
    interface AndroidView {
        fun initView() {}

        fun onClickView() {}

        fun bindData() {}
    }
}