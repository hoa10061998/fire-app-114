package com.huster.myapplication

import android.content.Context
import android.view.View
import android.widget.Toast
import androidx.core.view.isVisible

fun Context.showToast(msg: String) {
    Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}

fun Context.showErrorToast(msg: String? = null) {
    showToast(msg ?: "Đã có lỗi xảy ra, xin vui lòng thử lại")
}

fun View.hide() {
    if(isVisible) {
        isVisible = false
    }
}

fun View.show() {
    if(!isVisible) {
        isVisible = true
    }
}

fun View.disable() {
    if(isEnabled) {
        isEnabled = false
    }
}

fun View.enable() {
    if(!isEnabled) {
        isEnabled = true
    }
}