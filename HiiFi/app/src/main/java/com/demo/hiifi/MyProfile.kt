package com.demo.hiifi

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.bumptech.glide.Glide
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import de.hdodenhof.circleimageview.CircleImageView

class MyProfile : AppCompatActivity() {

    private lateinit var profileImage: CircleImageView
    private lateinit var cameraIcon: CircleImageView
    private lateinit var nameSection: LinearLayout
    private lateinit var aboutSection: LinearLayout
    private lateinit var phoneSection: LinearLayout
    private lateinit var name: TextView
    private lateinit var about: TextView
    private lateinit var phone: TextView
    private var imageUri: Uri? = null

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_my_profile)
        initialize()
        CloudinaryManager.init(this)
        getData()
        val toolbar = findViewById<Toolbar>(R.id.MyProfileToolbar)
        setSupportActionBar(toolbar)
        toolbar.title = "Profile"

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener {
            startActivity(Intent(this, Settings::class.java))
            finish()
        }

        cameraIcon.setOnClickListener {
            showEditProfileImageBottomSheet()
        }
        nameSection.setOnClickListener {
            showEditTextBottomSheet("name",name.text.toString())
        }
        aboutSection.setOnClickListener {
            showEditTextBottomSheet("about",about.text.toString())
        }
        phoneSection.setOnClickListener {
            showEditTextBottomSheet("phone",phone.text.toString())
        }
        profileImage.setOnClickListener{
            showFullImageDialog()
        }
    }

    override fun onResume() {
        super.onResume()
        getData()
    }

    private fun initialize() {
        profileImage = findViewById(R.id.My_profile_image)
        cameraIcon = findViewById(R.id.my_profile_camera_icon)
        nameSection = findViewById(R.id.Name_Section)
        aboutSection = findViewById(R.id.About_Section)
        phoneSection = findViewById(R.id.Phone_Section)
        name = findViewById(R.id.my_profile_name)
        about = findViewById(R.id.my_profile_about)
        phone = findViewById(R.id.my_profile_phone)
    }
    private fun getData() {
        val sharedPreferences = getSharedPreferences("Login", Context.MODE_PRIVATE)
        val userId = sharedPreferences.getString("Uid", "").toString()
        val db = FirebaseFirestore.getInstance()
        val bioCollection = db.collection("Bio")
        bioCollection.document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val bio = document.toObject(Bio::class.java) ?: Bio()
                    if(bio.about != null){
                        about.text = bio.about
                    }
                    if (bio.phone != null) {
                        phone.text = bio.phone
                    }
                    if (bio.imageUrl != null){
                        Glide.with(this).clear(profileImage)
                        Glide.with(this)
                            .load(bio.imageUrl)
                            .placeholder(R.drawable.profile)
                            .error(R.drawable.profile)
                            .into(profileImage)
                    }else{
                        profileImage.setImageResource(R.drawable.profile)
                    }
                    Log.d("MyProfile", "Retrieved Bio: $bio")
                } else {
                    Log.d("MyProfile", "No document found")
                }
            }
            .addOnFailureListener { e ->
                Log.e("MyProfile", "Error fetching bio", e)
            }

        val userCollection = db.collection("User")
        userCollection.whereEqualTo("id", userId).get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    val bio = document.toObject(Profile::class.java)
                    name.text = bio.name
                }
            }
            .addOnFailureListener { e ->
                Log.e("MyProfile", "Error finding user", e)
            }
    }
    private fun showFullImageDialog() {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_full_image)
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val fullImageView = dialog.findViewById<ImageView>(R.id.fullImageView)
        val closeDialog = dialog.findViewById<ImageView>(R.id.closeDialog)

        val sharedPreferences = getSharedPreferences("Login", Context.MODE_PRIVATE)
        val userId = sharedPreferences.getString("Uid", "").toString()
        val db = FirebaseFirestore.getInstance()
        val bioCollection = db.collection("Bio")
        bioCollection.document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val bio = document.toObject(Bio::class.java) ?: Bio()
                    if (bio.imageUrl != null){
                        Glide.with(this)
                            .load(bio.imageUrl)
                            .placeholder(R.drawable.profile)
                            .error(R.drawable.profile)
                            .into(fullImageView)
                    }else{
                        fullImageView.setImageResource(R.drawable.profile)
                    }
                    Log.d("MyProfile", "Retrieved Bio: $bio")
                } else {
                    Log.d("MyProfile", "No document found")
                }
            }
            .addOnFailureListener { e ->
                Log.e("MyProfile", "Error fetching bio", e)
            }
        // Close dialog on click
        closeDialog.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }
    @SuppressLint("MissingInflatedId")
    private fun showEditTextBottomSheet(type: String,text : String) {
        val bottomSheetDialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.bottom_sheet_edit_text, null)
        bottomSheetDialog.setContentView(view)

        val title: TextView = view.findViewById(R.id.bottomSheet_title)
        title.text = "Enter your $type"
        val editName: EditText = view.findViewById(R.id.bottomSheet_edit_name)
        editName.setText(text)
        val cancelBtn: TextView = view.findViewById(R.id.bottomSheet_cancel)
        val saveBtn: TextView = view.findViewById(R.id.bottomSheet_save)

        cancelBtn.setOnClickListener {
            editName.setText("")
            bottomSheetDialog.dismiss()
        }

        saveBtn.setOnClickListener {
            val enteredName = editName.text.toString().trim()
            if (enteredName.isNotEmpty()) {
                when (type) {
                    "name" -> {
                        name.text = enteredName
                        update(type, enteredName)
                    }
                    "about" -> {
                        about.text = enteredName
                        update(type, enteredName)
                    }
                    "phone" -> {
                        phone.text = enteredName
                        update(type, enteredName)
                    }
                }
                editName.clearFocus()
            }
            bottomSheetDialog.dismiss()
        }
        bottomSheetDialog.show()
    }

    @SuppressLint("MissingInflatedId")
    private fun showEditProfileImageBottomSheet() {
        val bottomSheetDialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.profile_photo_dilogue, null)
        bottomSheetDialog.setContentView(view)
        val closeIcon: ImageView = view.findViewById(R.id.bio_close_icon)
        val deleteIcon: ImageView = view.findViewById(R.id.bio_delete_icon)
        deleteIcon.setOnClickListener {
            val sharedPreferences = getSharedPreferences("Login", Context.MODE_PRIVATE)
            val userId = sharedPreferences.getString("Uid", "").toString()
            val db = FirebaseFirestore.getInstance()

            val bioCollection = db.collection("Bio")
            bioCollection.document(userId)
                .get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val bio = document.toObject(Bio::class.java) ?: Bio()
                        if(bio.imageUrlID != null){
//                            profileImage.setImageResource(R.drawable.profile)
                            deleteFromCloudinary(bio.imageUrlID)
                        }
                        val data = mapOf(
                            "id" to userId,
                            "imageUrl" to null,
                            "imageUrlID"  to null
                        )
                        bioCollection.document(userId)
                            .set(data, SetOptions.merge())
                            .addOnSuccessListener {
                                Log.d("MyProfile", "Bio deleted successfully!")
                            }
                    } else {
                        Log.d("MyProfile", "No document found")
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("MyProfile", "Error fetching bio", e)
                }
        }
        closeIcon.setOnClickListener {
            bottomSheetDialog.dismiss()
        }

        val cameraIcon: LinearLayout = view.findViewById(R.id.bio_camera_icon)
        val gallaryIcon: LinearLayout = view.findViewById(R.id.bio_gallary_icon)
        gallaryIcon.setOnClickListener{
            openGallery()
        }

        bottomSheetDialog.show()
    }
    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, 101)
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 101 && resultCode == Activity.RESULT_OK) {
            imageUri = data?.data
            profileImage.setImageURI(data?.data)
            imageUri?.let { uploadToCloudinary(it) }
        }
    }
    private fun uploadToCloudinary(fileUri: Uri) {

        MediaManager.get().upload(fileUri)  // 🔥 Use Uri directly!
            .option("folder", "UniDataHub/Images") // Store in Folder
            .callback(object : UploadCallback {
                override fun onStart(requestId: String?) {
                    Log.d("Cloudinary", "Upload started")
                }

                override fun onProgress(requestId: String?, bytes: Long, totalBytes: Long) {

                }

                override fun onSuccess(requestId: String?, resultData: MutableMap<Any?, Any?>?) {
                    val imageUrl = resultData?.get("url").toString()
                    Log.d("ImageUrl", imageUrl)
                    val publicId = resultData?.get("public_id").toString()
                    val sharedPreferences = getSharedPreferences("Login", Context.MODE_PRIVATE)
                    val userId = sharedPreferences.getString("Uid", "").toString()
                    val db = FirebaseFirestore.getInstance()

                    val bioCollection = db.collection("Bio")
                    val data = mapOf(
                        "id" to userId,
                        "imageUrl" to imageUrl,
                        "imageUrlID"  to publicId
                    )
                    bioCollection.document(userId)
                        .set(data, SetOptions.merge())
                        .addOnSuccessListener {
                            Log.d("MyProfile", "Bio updated successfully!")
                        }
                        .addOnFailureListener { e ->
                            Log.e("MyProfile", "Error updating bio", e)
                        }
                }

                override fun onError(requestId: String?, error: ErrorInfo?) {
                    Log.e("Cloudinary", "Upload error: ${error?.description}")
                    Toast.makeText(applicationContext, "Upload Failed!", Toast.LENGTH_SHORT).show()
                }

                override fun onReschedule(requestId: String?, error: ErrorInfo?) {
                    Log.d("Cloudinary", "Upload rescheduled")
                }
            }).dispatch()
    }
    private fun deleteFromCloudinary(publicId: String) {
        Thread {
            try {
                val result = MediaManager.get().cloudinary.uploader().destroy(publicId, emptyMap<String, Any>())
                runOnUiThread {
                    Log.d("Cloudinary", "Deleted Successfully: $result")
                    Toast.makeText(applicationContext, "Image Deleted!", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Log.e("Cloudinary", "Delete error: ${e.message}")
                    Toast.makeText(applicationContext, "Deletion Failed!", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }

    private fun update(type: String, value: String) {
        val sharedPreferences = getSharedPreferences("Login", Context.MODE_PRIVATE)
        val userId = sharedPreferences.getString("Uid", "").toString()
        val db = FirebaseFirestore.getInstance()

        if (type == "name") {
            val userCollection = db.collection("User")
            userCollection.whereEqualTo("id", userId).get()
                .addOnSuccessListener { documents ->
                    for (document in documents) {
                        userCollection.document(document.id)
                            .update("name", value)
                            .addOnSuccessListener {
                                Log.d("MyProfile", "User name updated successfully!")
                            }
                            .addOnFailureListener { e ->
                                Log.e("MyProfile", "Error updating name", e)
                            }
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("MyProfile", "Error finding user", e)
                }
        } else if (type == "about" || type == "phone") {
            val data = mapOf(
                type to value,
                "id" to userId
            )
            val userCollection = db.collection("Bio")

            userCollection.document(userId)
                .set(data, SetOptions.merge())
                .addOnSuccessListener {
                    Log.d("MyProfile", "Bio updated successfully!")
                }
                .addOnFailureListener { e ->
                    Log.e("MyProfile", "Error updating bio", e)
                }
        }
    }
}
