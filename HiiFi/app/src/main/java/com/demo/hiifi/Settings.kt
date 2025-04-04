package com.demo.hiifi

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.firestore.FirebaseFirestore
import de.hdodenhof.circleimageview.CircleImageView

class Settings : AppCompatActivity() {
    private lateinit var user_Status: TextView
    private lateinit var user_name: TextView
    private lateinit var userId: TextView
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var profileImage: CircleImageView
    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_settings)

        user_name =  findViewById<TextView>(R.id.user_name)
        userId  = findViewById<TextView>(R.id.user_iid)
        user_Status = findViewById<TextView>(R.id.user_status)
        sharedPreferences = getSharedPreferences("Login", Context.MODE_PRIVATE)
        profileImage = findViewById(R.id.settings_profile_image)

        getData()

        val recyclerView = findViewById<RecyclerView>(R.id.settings_list)
        recyclerView.layoutManager = LinearLayoutManager(this)

        val settingsItems = listOf(
            SettingsItem(R.drawable.baseline_key_24, "Account", "Security notifications, change number"),
            SettingsItem(R.drawable.baseline_lock_clock_24, "Privacy", "Block contacts, disappearing messages"),
            SettingsItem(R.drawable.baseline_face_24, "Avatar", "Create, edit, profile photo"),
            SettingsItem(R.drawable.gallary, "Lists", "Manage people and groups"),
            SettingsItem(R.drawable.baseline_message_24, "Chats", "Theme, wallpapers, chat history"),
            SettingsItem(R.drawable.baseline_notifications_24, "Notifications", "Message, group & call tones"),
            SettingsItem(R.drawable.baseline_data_usage_24, "Storage and data", "Network usage, auto-download"),
            SettingsItem(R.drawable.baseline_language_24, "App language", "English (device's language)"),
            SettingsItem(R.drawable.baseline_help_24, "Help", "Help centre, contact us, privacy policy"),
            SettingsItem(R.drawable.baseline_people_24, "Invite a friend", "")
        )

        recyclerView.adapter = SettingsAdapter(settingsItems)


        var layout = findViewById<LinearLayout>(R.id.profile_section)
        layout.setOnClickListener{
            val intent = Intent(this,MyProfile::class.java)
            startActivity(intent)
        }

        profileImage.setOnClickListener{
            showFullImageDialog()
        }

    }

    private fun getData(){
        val id = sharedPreferences.getString("Uid", "").toString()
        val db = FirebaseFirestore.getInstance()
        db.collection("User")
            .whereEqualTo("id", id)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    val userProfile = documents.toObjects(Profile::class.java).first()
                    user_name.text = userProfile!!.name.toString()
                    userId.text = userProfile.id.toString()
                } else {
                }
            }
            .addOnFailureListener {
            }
        val bioCollection = db.collection("Bio")
        bioCollection.document(id)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val bio = document.toObject(Bio::class.java) ?: Bio()
                    if(bio.about != null){
                        user_Status.text = bio.about
                    }
                    if (bio.imageUrl != null){
                        Glide.with(this).clear(profileImage)
                        Glide.with(this)
                            .load(bio.imageUrl)
                            .placeholder(R.drawable.profile)
                            .error(R.drawable.profile)
                            .into(profileImage)
                    }
                    Log.d("MyProfile", "Retrieved Bio: $bio")
                } else {
                    Log.d("MyProfile", "No document found")
                }
            }
            .addOnFailureListener { e ->
                Log.e("MyProfile", "Error fetching bio", e)
            }
    }
    private fun showFullImageDialog() {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_full_image)
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val fullImageView = dialog.findViewById<ImageView>(R.id.fullImageView)
        val closeDialog = dialog.findViewById<ImageView>(R.id.closeDialog)
        val id = sharedPreferences.getString("Uid", "").toString()
        GetImage.getImage(this, id.toString()) { imageUrl ->
            val finalImageUrl = if (imageUrl == "default") R.drawable.profile else imageUrl
            Glide.with(this)
                .load(finalImageUrl)
                .placeholder(R.drawable.profile)
                .error(R.drawable.profile)
                .into(fullImageView)
        }
        // Close dialog on click
        closeDialog.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }
}