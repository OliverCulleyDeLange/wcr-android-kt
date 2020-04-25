package uk.co.oliverdelange.wcr_android_kt.service

import com.google.firebase.auth.FirebaseAuth
import javax.inject.Inject

// Simple wrapper to enable unit testing
class AuthService @Inject constructor(){
    fun currentUser() = FirebaseAuth.getInstance().currentUser
}