package com.demo.hiifi

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class Login : Fragment() {
    private lateinit var emailInput: TextView
    private lateinit var passwordInput: TextView
    private lateinit var sharedPreferences: SharedPreferences

    @SuppressLint("MissingInflatedId")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val v = inflater.inflate(R.layout.fragment_login, container, false)
        val auth = FirebaseAuth.getInstance()

        emailInput = v.findViewById(R.id.loginEmailInput)
        passwordInput = v.findViewById(R.id.loginPasswordInput)
        sharedPreferences = requireContext().getSharedPreferences("Login", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()

        v.findViewById<TextView>(R.id.registerRedirect).setOnClickListener {
            parentFragmentManager.beginTransaction().replace(R.id.frame, Register()).commit()
        }

        v.findViewById<TextView>(R.id.loginButton).setOnClickListener {
            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(v.context, "Please enter email and password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            auth.signInWithEmailAndPassword(email, password).addOnSuccessListener {
                val uId = auth.currentUser?.uid
                editor.putString("User", uId.toString())
                editor.apply()

                val fdb = FirebaseFirestore.getInstance()
                fdb.collection("User")
                    .get()
                    .addOnSuccessListener { documents ->
                        val user = documents.toObjects(Profile::class.java).firstOrNull { it.email == email }
                        if (user != null) {
                            editor.putString("Uid", user.id.toString())
                            editor.apply()

                            Toast.makeText(v.context, "Login Successfully", Toast.LENGTH_LONG).show()
                            startActivity(Intent(requireActivity(), HomePage::class.java))
                        } else {
                        }
                    }
                    .addOnFailureListener {
                    }
            }.addOnFailureListener { fail ->
                if (fail.message == "The supplied auth credential is incorrect, malformed or has expired.") {
                    Toast.makeText(v.context, "Credential is Incorrect", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(v.context, "Login Unsuccessful", Toast.LENGTH_LONG).show()
                }
            }
        }

        v.findViewById<TextView>(R.id.forgotPassword).setOnClickListener {
            showForgotPasswordDialog()
        }

        return v
    }

    private fun showForgotPasswordDialog() {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Reset Password")

        val input = EditText(requireContext())
        input.hint = "Enter your email"
        builder.setView(input)

        builder.setPositiveButton("Send") { _, _ ->
            val email = input.text.toString().trim()
            if (email.isNotEmpty()) {
                FirebaseAuth.getInstance().sendPasswordResetEmail(email)
                    .addOnSuccessListener {
                        Toast.makeText(requireContext(), "Password reset email sent!", Toast.LENGTH_LONG).show()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_LONG).show()
                    }
            } else {
                Toast.makeText(requireContext(), "Please enter a valid email", Toast.LENGTH_SHORT).show()
            }
        }

        builder.setNegativeButton("Cancel") { dialog, _ ->
            dialog.dismiss()
        }

        builder.show()
    }
}
