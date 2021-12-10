package com.huster.myapplication.ui.personal

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.huster.myapplication.*
import com.huster.myapplication.databinding.ActivityEnterUserInfoBinding
import com.huster.myapplication.models.UserModel

class EnterUserInfoActivity : AppCompatActivity() {
    companion object {
        @JvmStatic
        fun start(context: Context) {
            val starter = Intent(context, EnterUserInfoActivity::class.java)
            context.startActivity(starter)
        }
    }

    private lateinit var binding: ActivityEnterUserInfoBinding

    private var user: UserModel? = null

    private val database: FirebaseDatabase by lazy {
        Firebase.database
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEnterUserInfoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        user = UserManager.currentUserModel

        if (user != null) {
            inputHandler()

            binding.btnSave.setOnClickListener {
                UserManager.save(this,
                    user?.apply {
                        password = binding.edtPassword.text.toString()
                        name = binding.edtUserName.text.toString()
                        address = binding.edtAddress.text.toString()
                    }, object : UserManager.UpdateUserListener {
                        override fun onSuccess() {
                            showToast("Thành công")
                            MainActivity.start(this@EnterUserInfoActivity)
                        }

                        override fun onFail(e: Exception?) {
                            e?.printStackTrace()
                            showErrorToast()
                        }
                    })

            }
        } else {
            finish()
            showErrorToast()
        }
    }

    private fun inputHandler() {
        binding.edtPassword.doAfterTextChanged {
            validateAll()
        }

        binding.edtRePassword.doAfterTextChanged {
            validateAll()
        }

        binding.edtUserName.doAfterTextChanged {
            validateAll()
        }

        binding.edtAddress.doAfterTextChanged {
            validateAll()
        }
    }

    private fun validatePassword(value: String): Boolean {
        return when {
            value.trim().isEmpty() -> {
                binding.password.error = "Password không được bỏ trống"
                false
            }
            value.length < 8 -> {
                binding.password.error = "Password không được ngắn hơn 8 kí tự"
                false
            }
            else -> {
                binding.password.error = null
                true
            }
        }
    }

    private fun validateRePassword(value: String): Boolean {
        return if (value != binding.edtPassword.text.toString()) {
            binding.rePassword.error = "Mật khẩu không giống"
            false
        } else {
            binding.rePassword.error = null
            true
        }
    }

    private fun validateUserName(value: String): Boolean {
        return if (value.trim().isEmpty()) {
            binding.userName.error = "Không được bỏ trống họ tên"
            false
        } else {
            binding.userName.error = null
            true
        }
    }

    private fun validateAddress(value: String): Boolean {
        return if (value.trim().isEmpty()) {
            binding.address.error = "Không được bỏ trống địa chỉ"
            false
        } else {
            binding.address.error = null
            true
        }
    }

    private fun validateAll() {
        if (
            validatePassword(binding.edtPassword.text.toString()) &&
            validateRePassword(binding.edtRePassword.text.toString()) &&
            validateAddress(binding.edtAddress.text.toString()) &&
            validateUserName(binding.edtUserName.text.toString())
        ) {
            binding.btnSave.enable()
        } else {
            binding.btnSave.disable()
        }
    }
}