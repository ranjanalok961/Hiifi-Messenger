package com.demo.hiifi

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class Register : Fragment() {
    private lateinit var nameInput: TextView
    private lateinit var emailInput: TextView
    private lateinit var passwordInput: TextView
    private lateinit var registerButton: Button
    private lateinit var termsCheckBox: CheckBox

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val v = inflater.inflate(R.layout.fragment_register, container, false)

        nameInput = v.findViewById(R.id.nameInput)
        emailInput = v.findViewById(R.id.emailInput)
        passwordInput = v.findViewById(R.id.passwordInput)
        registerButton = v.findViewById(R.id.registerButton)
        termsCheckBox = v.findViewById(R.id.termsCheckbox)

        v.findViewById<TextView>(R.id.loginRedirect).setOnClickListener {
            parentFragmentManager.beginTransaction().replace(R.id.frame, Login()).commit()
        }

        // Disable Register button initially
        registerButton.isEnabled = true

        // Enable button only when checkbox is checked
        termsCheckBox.setOnCheckedChangeListener { _, isChecked ->
            registerButton.isEnabled = isChecked
        }

        registerButton.setOnClickListener {
            val auth = FirebaseAuth.getInstance()
            val fdb = FirebaseFirestore.getInstance()
            val name = nameInput.text.toString()
            val email = emailInput.text.toString()
            val password = passwordInput.text.toString()

            if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(v.context, "Please fill all fields", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            auth.createUserWithEmailAndPassword(email, password).addOnSuccessListener {
                val userId = email.split("@")[0]
                val data = Profile(email, userId, name)

                fdb.collection("User").add(data)
                    .addOnSuccessListener {
                        Toast.makeText(v.context, "Data Saved", Toast.LENGTH_LONG).show()
                    }.addOnFailureListener {
                        Toast.makeText(v.context, "Data Not Saved", Toast.LENGTH_LONG).show()
                    }

                parentFragmentManager.beginTransaction().replace(R.id.frame, Login()).commit()
                Toast.makeText(v.context, "Registered Successfully", Toast.LENGTH_LONG).show()
            }.addOnFailureListener { fail ->
                if (fail.message == "The email address is already in use by another account.") {
                    Toast.makeText(v.context, "Email Already Exists.", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(v.context, "Registration Unsuccessful", Toast.LENGTH_LONG).show()
                }
            }
        }

        return v
    }
}
