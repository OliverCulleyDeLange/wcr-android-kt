package uk.co.oliverdelange.wcr_android_kt.util

import android.app.Activity
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AppCompatActivity


// Simplifies fragment transactions
inline fun androidx.fragment.app.FragmentManager.inTransaction(func: androidx.fragment.app.FragmentTransaction.() -> androidx.fragment.app.FragmentTransaction) {
    beginTransaction().func().commit()
}

fun AppCompatActivity.addFragment(fragment: androidx.fragment.app.Fragment, frameId: Int) {
    supportFragmentManager.inTransaction { add(frameId, fragment) }
}

fun AppCompatActivity.replaceFragment(fragment: androidx.fragment.app.Fragment, frameId: Int) {
    supportFragmentManager.inTransaction { replace(frameId, fragment) }
}

fun AppCompatActivity.removeFragment(fragment: androidx.fragment.app.Fragment) {
    supportFragmentManager.inTransaction {
        remove(fragment)
    }
}

fun randomAlphaNumeric(length: Int): String {
    val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
    return (1..length).map { chars.random() }.joinToString("")
}

//https://stackoverflow.com/questions/1109022/close-hide-the-android-soft-keyboard
fun hideKeyboard(activity: Activity) {
    val imm = activity.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
    //Find the currently focused view, so we can grab the correct window token from it.
    var view = activity.currentFocus
    //If no view currently has focus, create a new one, just so we can grab a window token from it
    if (view == null) {
        view = View(activity)
    }
    imm.hideSoftInputFromWindow(view.windowToken, 0)
}