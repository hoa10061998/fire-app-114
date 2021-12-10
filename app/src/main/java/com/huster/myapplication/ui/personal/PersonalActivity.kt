package com.huster.myapplication.ui.personal

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.huster.myapplication.databinding.ActivityPersonalBinding
import com.huster.myapplication.models.UserModel
import com.huster.myapplication.showToast

class PersonalActivity : AppCompatActivity(), OnUserChangeListener {
    companion object{
        @JvmStatic
        fun start(context: Context) {
            val starter = Intent(context, PersonalActivity::class.java)
            context.startActivity(starter)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        UserManager.removeListener(this)
    }

    private lateinit var binding: ActivityPersonalBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPersonalBinding.inflate(layoutInflater)
        setContentView(binding.root)

        UserManager.addListener(this)

        UserManager.currentUserModel?.let {
            binding.btnChangeUserName.setOnClickListener {
                changeInfo(USERNAME)
            }

            binding.btnChangeAddress.setOnClickListener {
                changeInfo(ADDRESS)
            }

            binding.btnChangePassword.setOnClickListener {
                changeInfo(PASSWORD)
            }
        }

        binding.btnLogout.setOnClickListener {
            UserManager.logout(this)
            UserManager.restart(this)
        }
    }

    private fun setUserContent(user: UserModel) {
        binding.tvPhoneNumber.text = user.phoneNumber
        binding.tvUsername.text = user.name
        binding.tvAddress.text = user.address
    }

    private fun changeInfo(type: String) {
        BottomSheetChangeInfoFragment.newInstance(type).show(supportFragmentManager, null)
    }

    override fun onChange(user: UserModel?) {
        user?.let {
            setUserContent(it)
        }
    }
}