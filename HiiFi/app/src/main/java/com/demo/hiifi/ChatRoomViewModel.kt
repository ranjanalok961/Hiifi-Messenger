package com.demo.hiifi

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class ChatRoomViewModel : ViewModel() {
    val chatData : MutableLiveData<String> by lazy {
        MutableLiveData<String>()
    }
    fun setData(name: String){
        chatData.value = name
    }
}