package com.demo.hiifi

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {
    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        val auth = FirebaseAuth.getInstance()
        val uId = auth.currentUser?.uid
        val sharedPreferences = getSharedPreferences("Login", Context.MODE_PRIVATE)
        val savedUser = sharedPreferences.getString("User","")
        if(uId.toString() == savedUser){
            startActivity(Intent(this, HomePage::class.java))
            finish()
        }else{
            supportFragmentManager.beginTransaction().replace(R.id.frame, Login()).commit()
        }
    }
}