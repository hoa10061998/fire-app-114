package com.huster.myapplication.ui.otp

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.*
import com.huster.myapplication.databinding.ActivityVerifyOtpCodeBinding
import com.huster.myapplication.models.UserModel
import com.huster.myapplication.showErrorToast
import com.huster.myapplication.showToast
import com.huster.myapplication.ui.personal.EnterUserInfoActivity
import com.huster.myapplication.ui.personal.UserManager
import java.util.concurrent.TimeUnit

class VerifyOtpCodeActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "VerifyOtpCodeActivity"

        @JvmStatic
        fun start(context: Context, phoneNumber: String) {
            val starter = Intent(context, VerifyOtpCodeActivity::class.java)
                .putExtra("phoneNumber", phoneNumber)
            context.startActivity(starter)
        }
    }

    private var phoneNumber = ""
    private lateinit var binding: ActivityVerifyOtpCodeBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVerifyOtpCodeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        intent.getStringExtra("phoneNumber")?.let {
            phoneNumber = it
        }

        verifyPhoneNumber(phoneNumber)

        binding.pinView.doAfterTextChanged {
            binding.btnSubmitVerifyCode.isEnabled = it.toString().length == 6
        }

        binding.btnSubmitVerifyCode.setOnClickListener {
            verifyCode(binding.pinView.text.toString().take(6))
        }

        binding.tvResend.setOnClickListener {
            resendOtp()
        }
    }

    private var storedVerificationId = ""
    private lateinit var resendToken: PhoneAuthProvider.ForceResendingToken
    private val auth = FirebaseAuth.getInstance()

    private fun verifyPhoneNumber(phoneNumber: String) {
        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phoneNumber)       // Phone number to verify
            .setTimeout(60L, TimeUnit.SECONDS) // Timeout and unit
            .setActivity(this)                 // Activity (for callback binding)
            .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                    // This callback will be invoked in two situations:
                    // 1 - Instant verification. In some cases the phone number can be instantly
                    //     verified without needing to send or enter a verification code.
                    // 2 - Auto-retrieval. On some devices Google Play services can automatically
                    //     detect the incoming verification SMS and perform verification without
                    //     user action.
                    Log.d(TAG, "onVerificationCompleted:$credential")
                    signInWithPhoneAuthCredential(credential)
                }

                override fun onVerificationFailed(e: FirebaseException) {
                    // This callback is invoked in an invalid request for verification is made,
                    // for instance if the the phone number format is not valid.
                    Log.w(TAG, "onVerificationFailed", e)

                    if (e is FirebaseAuthInvalidCredentialsException) {
                        showToast("Bạn đã nhập sai mã PIN, vui lòng thử lại")
                    } else if (e is FirebaseTooManyRequestsException) {
                        showToast("Quá nhiều lần, vui lòng thử lại sau")
                    }
                }

                override fun onCodeSent(
                    verificationId: String,
                    token: PhoneAuthProvider.ForceResendingToken
                ) {
                    // The SMS verification code has been sent to the provided phone number, we
                    // now need to ask the user to enter the code and then construct a credential
                    // by combining the code with a verification ID.
                    Log.d(TAG, "onCodeSent:$verificationId")

                    // Save verification ID and resending token so we can use them later
                    storedVerificationId = verificationId
                    resendToken = token
                }
            })
            .build()
        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    private fun resendOtp() {
        val optionsBuilder = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phoneNumber)       // Phone number to verify
            .setTimeout(60L, TimeUnit.SECONDS) // Timeout and unit
            .setActivity(this)
            .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                    Log.d(TAG, "onVerificationCompleted:$credential")
                    signInWithPhoneAuthCredential(credential)
                }

                override fun onVerificationFailed(e: FirebaseException) {
                    // This callback is invoked in an invalid request for verification is made,
                    // for instance if the the phone number format is not valid.
                    Log.w(TAG, "onVerificationFailed", e)

                    if (e is FirebaseAuthInvalidCredentialsException) {
                        showToast("Bạn đã nhập sai mã PIN, vui lòng thử lại")
                    } else if (e is FirebaseTooManyRequestsException) {
                        showToast("Quá nhiều lần, vui lòng thử lại sau")
                    }
                }

                override fun onCodeSent(
                    verificationId: String,
                    token: PhoneAuthProvider.ForceResendingToken
                ) {
                    // The SMS verification code has been sent to the provided phone number, we
                    // now need to ask the user to enter the code and then construct a credential
                    // by combining the code with a verification ID.
                    Log.d(TAG, "onCodeSent:$verificationId")

                    // Save verification ID and resending token so we can use them later
                    storedVerificationId = verificationId
                    resendToken = token
                }
            })

        if (this::resendToken.isInitialized) {
            optionsBuilder.setForceResendingToken(resendToken)
        }

        PhoneAuthProvider.verifyPhoneNumber(optionsBuilder.build())
        showToast("Đang gửi lại ...")
    }

    private fun signInWithPhoneAuthCredential(credential: PhoneAuthCredential) {
        auth.signInWithCredential(credential)
            .addOnCompleteListener(
                this
            ) { task ->
                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
                    Log.d(TAG, "signInWithCredential:success")
                    val user: FirebaseUser? = task.result?.user
                    // Update UI
                    user?.let {
                        UserManager.save(this, UserModel(user.uid, user.phoneNumber), object : UserManager.UpdateUserListener{
                            override fun onSuccess() {
                                EnterUserInfoActivity.start(this@VerifyOtpCodeActivity)
                            }

                            override fun onFail(e: Exception?) {
                                showErrorToast(e?.message)
                            }
                        })

                    }

                } else {
                    // Sign in failed, display a message and update the UI
                    Log.w(TAG, "signInWithCredential:failure", task.exception)
                    if (task.exception is FirebaseAuthInvalidCredentialsException) {
                        showToast("Bạn đã nhập sai mã PIN, vui lòng thử lại")
                    }
                }
            }
    }

    private fun verifyCode(smsCode: String) {
        try {
            val credential = PhoneAuthProvider.getCredential(storedVerificationId, smsCode)
            signInWithPhoneAuthCredential(credential)
        } catch (e: Exception) {
            e.printStackTrace()
            showErrorToast("Vui lòng thử lại")
        }
    }
}

