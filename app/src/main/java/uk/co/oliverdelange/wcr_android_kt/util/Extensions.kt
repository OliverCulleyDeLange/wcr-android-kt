package uk.co.oliverdelange.wcr_android_kt.util

import android.app.Activity
import android.content.res.Resources
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import com.google.android.material.bottomsheet.BottomSheetBehavior


// https://medium.com/thoughts-overflow/how-to-add-a-fragment-in-kotlin-way-73203c5a450b
inline fun FragmentManager.inTransaction(func: FragmentTransaction.() -> FragmentTransaction) {
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

fun Int.toPx(): Int = (this * Resources.getSystem().displayMetrics.density).toInt()

fun stateFromInt(it: Int?): String {
    return when (it) {
        BottomSheetBehavior.STATE_DRAGGING -> "STATE_DRAGGING"
        BottomSheetBehavior.STATE_SETTLING -> "STATE_SETTLING"
        BottomSheetBehavior.STATE_EXPANDED -> "STATE_EXPANDED"
        BottomSheetBehavior.STATE_COLLAPSED -> "STATE_COLLAPSED"
        BottomSheetBehavior.STATE_HIDDEN -> "STATE_HIDDEN"
        BottomSheetBehavior.STATE_HALF_EXPANDED -> "STATE_HALF_EXPANDED"
        else -> "UNKNOWN"
    }
}