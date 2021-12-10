package com.huster.myapplication.ui.personal

import android.content.Context
import androidx.fragment.app.FragmentActivity
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.google.gson.Gson
import com.huster.myapplication.PreferenceHelper
import com.huster.myapplication.PreferenceHelper.get
import com.huster.myapplication.PreferenceHelper.set
import com.huster.myapplication.models.UserModel

interface OnUserChangeListener{
    fun onChange(user: UserModel?)
}

object UserManager {
    private const val KEY = "user_manager"
    var currentUserModel: UserModel? = null
        private set

    private val listeners = mutableSetOf<OnUserChangeListener>()

    fun addListener(listener : OnUserChangeListener) {
        listeners.add(listener)
        listener.onChange(currentUserModel)
    }

    fun removeListener(listener : OnUserChangeListener) {
        listeners.remove(listener)
    }

    fun notifyUserObservers() {
        listeners.forEach {
            try {
                it.onChange(currentUserModel)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun init(context: Context) {
        val jsonValue = PreferenceHelper.defaultPrefs(context)[KEY, ""]
        currentUserModel = try {
            if (jsonValue.isEmpty()) {
                null
            } else {
                Gson().fromJson(jsonValue, UserModel::class.java)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
        notifyUserObservers()
    }

    interface UpdateUserListener {
        fun onSuccess()
        fun onFail(e: Exception? = null)
    }

    fun save(context: Context, user: UserModel?, completeListener: UpdateUserListener) {
        if (user == null) {
            return
        }
        val phoneNumber = user.phoneNumber
        if (phoneNumber == null) {
            completeListener.onFail()
            return
        }

        val database = Firebase.database
        database.getReference(UserModel.USERS_PATH).child(phoneNumber).setValue(user)
            .addOnSuccessListener {
                currentUserModel = user
                PreferenceHelper.defaultPrefs(context)[KEY] = Gson().toJson(user)
                completeListener.onSuccess()
                notifyUserObservers()
            }
            .addOnFailureListener {
                completeListener.onFail(it)
            }
    }

    fun logout(context: Context) {
        currentUserModel = null
        PreferenceHelper.defaultPrefs(context)[KEY] = ""
        notifyUserObservers()
    }

    fun restart(context: Context) {
        if (context is FragmentActivity) {
            context.finishAffinity()
        }
    }
}