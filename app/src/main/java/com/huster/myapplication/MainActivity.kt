package com.huster.myapplication

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.huster.myapplication.databinding.ActivityMainBinding
import com.huster.myapplication.models.UserModel
import com.huster.myapplication.ui.personal.OnUserChangeListener
import com.huster.myapplication.ui.personal.PersonalActivity
import com.huster.myapplication.ui.personal.UserManager

class MainActivity : AppCompatActivity(), OnUserChangeListener {
    companion object {
        @JvmStatic
        fun start(context: Context) {
            val starter = Intent(context, MainActivity::class.java)
            context.startActivity(starter)
        }
    }

    private lateinit var binding: ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        UserManager.addListener(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        UserManager.removeListener(this)
    }

    override fun onChange(user: UserModel?) {
        user?.let {
            binding.tvPhoneNumber.text = it.phoneNumber
            binding.tvUserName.text = it.name

            binding.personal.setOnClickListener {
                PersonalActivity.start(this)
            }

            binding.tvAddress.text = it.address
        }
    }
}