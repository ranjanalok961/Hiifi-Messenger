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
import android.view.MotionEvent
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.NestedScrollView
import com.bumptech.glide.Glide
import com.google.firebase.firestore.FirebaseFirestore
import de.hdodenhof.circleimageview.CircleImageView

class ProfileScreen : AppCompatActivity() {

    private lateinit var userName: TextView
    private lateinit var userId: TextView

    private var initialY = 0f
    private var initialHeight = 200
    private var initialImageSize = 80

    private lateinit var sharedPreferences: SharedPreferences
    private val userProfiles: MutableList<Profile> = mutableListOf()
    private lateinit var profile : Profile
    private lateinit var profileImageButton : ImageView

    private var id : String ? = null
    @SuppressLint("ClickableViewAccessibility", "MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile_screen)
        sharedPreferences = getSharedPreferences("Login", Context.MODE_PRIVATE)
        loadUserProfiles()
        userName = findViewById(R.id.userName)
        userId = findViewById(R.id.UserId)
        id = intent.getStringExtra("id")
        Log.d("UserId", id.toString())
        val db = FirebaseFirestore.getInstance()

        db.collection("User")
            .whereEqualTo("id", id) // Query to match a specific user
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    val userProfile = documents.toObjects(Profile::class.java).first()
                    Log.d("UserName", "Fetched User: ${userProfile.name}") // Adjust based on Profile model
                    userName.text = userProfile.name
                    userId.text = userProfile.id
                } else {
                }
            }
            .addOnFailureListener { e ->
            }

        val nestedScrollView = findViewById<NestedScrollView>(R.id.nestedScrollView)
        val profileImage = findViewById<CircleImageView>(R.id.res_user_image)
        GetImage.getImage(this, id.toString()) { imageUrl ->
            val finalImageUrl = if (imageUrl == "default") R.drawable.profile else imageUrl
            Glide.with(this)
                .load(finalImageUrl)
                .placeholder(R.drawable.profile)
                .error(R.drawable.profile)
                .into(profileImage)
        }
        profileImage.setOnClickListener{
            showFullImageDialog()
        }
        val headerLayout = findViewById<RelativeLayout>(R.id.header_layout)

        nestedScrollView.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialY = event.rawY
                    initialHeight = headerLayout.height
                    initialImageSize = profileImage.layoutParams.width
                }

                MotionEvent.ACTION_MOVE -> {
                    val deltaY = event.rawY - initialY
                    if (deltaY.toInt() !=  0) { // Dragging down
                        val newHeight = initialHeight + deltaY.toInt()
                        val newImageSize = initialImageSize + deltaY.toInt() / 3// Scale image slower

                        if(newHeight >= 200 && newHeight <= 700){
                            // Set new header height
                            val params = headerLayout.layoutParams
                            params.height = newHeight
                            headerLayout.layoutParams = params

                            // Scale Profile Image
                            profileImage.layoutParams.width = newImageSize
                            profileImage.layoutParams.height = newImageSize
                            profileImage.requestLayout()
                        }
                    }
                }
            }
            false
        }

        profileImageButton = findViewById(R.id.profile_message_button)
        profileImageButton.setOnClickListener {
            val intent = Intent(this, chatRoomPage::class.java).apply {
                putExtra("name", profile.name)
                putExtra("id", profile.id)
            }
            startActivity(intent)
        }

    }
    private fun loadUserProfiles() {
        val id = intent.getStringExtra("id")
        val db = FirebaseFirestore.getInstance()
        db.collection("User")
            .get()
            .addOnSuccessListener { documents ->
                userProfiles.clear()
                userProfiles.addAll(documents.toObjects(Profile::class.java))
                profile = userProfiles.find { it.id == id }!!
                Log.d("FilteredPro", "$profile")
            }
            .addOnFailureListener {
                Log.e("Firestore", "Error fetching user profiles")
            }
    }

    private fun showFullImageDialog() {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_full_image)
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val fullImageView = dialog.findViewById<ImageView>(R.id.fullImageView)
        val closeDialog = dialog.findViewById<ImageView>(R.id.closeDialog)

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

