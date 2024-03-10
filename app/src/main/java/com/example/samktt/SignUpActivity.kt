package com.example.samktt

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.samktt.databinding.ActivitySignUpBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import java.util.*


class SignUpActivity : AppCompatActivity() {

    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var binding: ActivitySignUpBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignUpBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseAuth = FirebaseAuth.getInstance()

        binding.textView.setOnClickListener {
            finish()
        }

        binding.button.setOnClickListener {
            val email = binding.emailEt.text.toString()
            val password = binding.passET.text.toString()
            val confirmPassword = binding.confirmPassEt.text.toString()

            if (email.isNotEmpty() && password.isNotEmpty() && confirmPassword.isNotEmpty()) {
                if (password == confirmPassword) {
                    firebaseAuth.createUserWithEmailAndPassword(email, password)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                // Store user data in the Firebase Realtime Database
                                val userId = firebaseAuth.currentUser?.uid
                                val currentTime = Calendar.getInstance().time.toString()

                                userId?.let { uid ->
                                    val database = FirebaseDatabase.getInstance()
                                    val userRef = database.getReference("Users").child(uid)
                                    userRef.child("email").setValue(email)
                                    userRef.child("password").setValue(password)
                                    userRef.child("loginTime").setValue(currentTime)

                                    // Saving additional fields like email and password
                                    val userData = HashMap<String, Any>()
                                    userData["email"] = email
                                    userData["password"] = password
                                    userData["loginTime"] = currentTime
                                    userRef.updateChildren(userData)
                                }

                                // Navigate to SignInActivity
                                val intent = Intent(this, SignInActivity::class.java)
                                startActivity(intent)
                                finish()
                            } else {
                                Toast.makeText(
                                    this,
                                    "Failed to register user: ${task.exception?.message}",
                                    Toast.LENGTH_SHORT
                                ).show()
                                Log.d("SignUpActivity", "Failed to register user: ${task.exception?.message}")
                            }
                        }
                } else {
                    Toast.makeText(this, "Passwords do not match.", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Please fill in all fields.", Toast.LENGTH_SHORT).show()
            }
        }
    }





    private fun signUpUser(user: User) {
        // Your sign up logic here

        // If sign up is successful, start SignInActivity
        startActivity(Intent(this, SignInActivity::class.java))
        finish() // Finish SignUpActivity to prevent going back to it with back button
    }
}
