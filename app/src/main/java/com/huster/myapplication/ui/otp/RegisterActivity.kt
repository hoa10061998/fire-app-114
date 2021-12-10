package com.huster.myapplication.ui.otp

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.google.gson.Gson
import com.huster.myapplication.*
import com.huster.myapplication.PreferenceHelper.get
import com.huster.myapplication.databinding.ActivityRegisterBinding
import com.huster.myapplication.models.UserModel
import com.huster.myapplication.ui.personal.UserManager


class RegisterActivity : AppCompatActivity() {
    companion object {
        @JvmStatic
        fun start(context: Context) {
            val starter = Intent(context, RegisterActivity::class.java)
            context.startActivity(starter)
        }
    }

    private val database: FirebaseDatabase by lazy {
        Firebase.database
    }


    private lateinit var binding: ActivityRegisterBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val userFromCached = UserManager.currentUserModel
        if(userFromCached != null) {
            MainActivity.start(this)
            finish()
        }

        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)
        var currentUser: UserModel? = null

        binding.edtMobilePhone.doAfterTextChanged {
            loginToggle(false)
            if (getPhoneNumberVietNam(it.toString()).isValidPhoneNumber()) {
                binding.btnContinue.enable()
            } else {
                binding.btnContinue.disable()
            }
        }

        binding.btnContinue.setOnClickListener {
            val phoneNumber = getPhoneNumberVietNam(binding.edtMobilePhone.text.toString())
            if (phoneNumber.isValidPhoneNumber()) {
                database.getReference(UserModel.USERS_PATH).child(phoneNumber).get()
                    .addOnSuccessListener {
                        val value = it.value
                        if (value == null) {
                            showToast("Mời nhập mã OTP để xác minh số điện thoại")
                            VerifyOtpCodeActivity.start(
                                this,
                                phoneNumber
                            )
                        } else {
                            val jsonValue = Gson().toJson(value)
                            try {
                                currentUser = Gson().fromJson(jsonValue, UserModel::class.java)
                                showToast("Xin chào ${currentUser?.phoneNumber}, nhập mật khẩu để đăng nhập")
                                loginToggle(true)
                            } catch (e: Exception) {
                                e.printStackTrace()
                                showErrorToast(e.message)
                            }
                        }
                    }
                    .addOnFailureListener {
                        it.printStackTrace()
                    }
            } else {
                showErrorToast("Số điện thoại có format không hợp lệ")
            }
        }

        binding.btnLogin.setOnClickListener {
            val user = currentUser
            if(user != null) {
                if(binding.edtPwd.text.toString() == user.password) {
                    UserManager.save(this, user, object : UserManager.UpdateUserListener{
                        override fun onSuccess() {
                            showToast("Đăng nhập thành công!!")
                            MainActivity.start(this@RegisterActivity)
                            finish()
                        }

                        override fun onFail(e: Exception?) {
                            showErrorToast(e?.message)
                        }
                    })
                } else {
                    showErrorToast("Đăng nhập thất bại, kiểm tra lại tên đăng nhập và mật khẩu")
                }
            } else {
                showErrorToast()
            }
        }
    }

    private fun getPhoneNumberVietNam(phoneNumber: String): String{
        return "+84${phoneNumber}"
    }

    private fun loginToggle(turnOn: Boolean) {
        if (turnOn) {
            binding.llLogin.show()
            binding.edtPwd.text?.clear()
            binding.btnContinue.hide()
        } else {
            binding.llLogin.hide()
            binding.btnContinue.show()
        }
    }
}


fun String.isValidPhoneNumber(): Boolean {
    return "^[+][0-9]{10,13}$".toRegex().matches(this)
}