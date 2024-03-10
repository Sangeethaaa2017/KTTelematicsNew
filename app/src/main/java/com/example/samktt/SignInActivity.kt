package com.example.samktt

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import com.example.samktt.databinding.ActivitySignInBinding
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.*

class SignInActivity : AppCompatActivity() {

    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var binding: ActivitySignInBinding
    private lateinit var adapter: ArrayAdapter<String>


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignInBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseAuth = FirebaseAuth.getInstance()
        adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, mutableListOf())

        val switchUserButton: AppCompatButton = findViewById(R.id.switchUserButton)
        switchUserButton.setOnClickListener {
            switchUser()
        }

        binding.button.setOnClickListener {
            val email = binding.emailEt.text.toString()
            val password = binding.passET.text.toString()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                signInUser(email, password)
            } else {
                Toast.makeText(this, "Empty Fields Are not Allowed !!", Toast.LENGTH_SHORT).show()
            }
        }

        // Add this section to navigate to SignUpActivity
        binding.textView.setOnClickListener {
            startActivity(Intent(this, SignUpActivity::class.java))
        }
    }

    private fun signInUser(email: String, password: String) {
        firebaseAuth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // Retrieve user data from Firebase Realtime Database
                    val userId = firebaseAuth.currentUser!!.uid
                    val userEmail = firebaseAuth.currentUser?.email

                    val database = FirebaseDatabase.getInstance()
                    val userRef = database.getReference("Users").child(userId)

                    userRef.addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(dataSnapshot: DataSnapshot) {
                            val userEmail = dataSnapshot.child("email").getValue(String::class.java)
                            val userPassword = dataSnapshot.child("password").getValue(String::class.java)

                            // Display retrieved email and password
                            Toast.makeText(
                                this@SignInActivity,
                                "Email: $userEmail, Password: $userPassword",
                                Toast.LENGTH_SHORT
                            ).show()
                        }

                        override fun onCancelled(databaseError: DatabaseError) {
                            Log.e("SignInActivity", "Failed to retrieve user data: ${databaseError.message}")
                        }
                    })

                    // Store login time
                    val currentTimeMillis = System.currentTimeMillis()
                    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                    val formattedTime = dateFormat.format(Date(currentTimeMillis))
                    userRef.child("loginTime").setValue(formattedTime)
                    // Start location updates

                    // Navigate to MainActivity
                    val intent = Intent(this@SignInActivity, MainActivity::class.java)
                    startActivity(intent)
                    finish()
                } else {
                    Toast.makeText(
                        this@SignInActivity,
                        "Failed to sign in: ${task.exception?.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                    Log.d("SignInActivity", "Failed to sign in: ${task.exception?.message}")
                }
            }
    }
    private fun switchUser() {
        // Implement the logic to switch user here
        // For example, sign out the current user and navigate to the sign-in screen
        FirebaseAuth.getInstance().signOut()
        // Clear login status from SharedPreferences
       // sharedPreferences.edit().remove("isLoggedIn").apply()
        startActivity(Intent(this, SignInActivity::class.java))
        finish() // Finish SignInActivity to prevent going back to it with back button
    }




}
