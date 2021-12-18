package com.huster.myapplication.models

import com.google.firebase.firestore.IgnoreExtraProperties



@IgnoreExtraProperties
data class UserModel(
    var id: String? = null,
    var phoneNumber: String? = null,
    var name: String? = null,
    var password: String? = null,
    var address: String? = null,
    var extraInfo: String? = null
) {
    companion object {
        const val USERS_PATH = "users"
        const val IMAGES_PATH = "images"
    }
}

