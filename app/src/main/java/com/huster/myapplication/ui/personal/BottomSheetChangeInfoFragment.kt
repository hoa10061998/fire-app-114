package com.huster.myapplication.ui.personal

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.doAfterTextChanged
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.huster.myapplication.*
import com.huster.myapplication.databinding.FragmentBottomSheetChangeInfoBinding

private const val ARG_TYPE = "ARG_TYPE"

const val USERNAME = "user_name"
const val ADDRESS = "address"
const val PASSWORD = "pwd"

class BottomSheetChangeInfoFragment : BottomSheetDialogFragment() {
    private var type: String = ""

    private lateinit var binding: FragmentBottomSheetChangeInfoBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            type = it.getString(ARG_TYPE) ?: ""
        }
    }

    @SuppressLint("SetTextI18n")
    private fun configViews() {
        binding.btnSave.setOnClickListener {
            val user = UserManager.currentUserModel
            user?.let {
                when (type) {
                    USERNAME -> {
                        user.name = binding.edtInfo.text.toString()
                    }

                    ADDRESS -> {
                        user.address = binding.edtInfo.text.toString()
                    }

                    PASSWORD -> {
                        user.password = binding.edtInfo.text.toString()
                    }
                }
            }

            UserManager.save(requireContext(), user, object : UserManager.UpdateUserListener{
                override fun onSuccess() {
                    dismiss()
                }

                override fun onFail(e: Exception?) {
                    requireContext().showErrorToast(e?.message)
                }
            })
        }

        var title = ""

        when (type) {
            USERNAME -> {
                title = "Họ và tên"
                binding.layout2.hide()
            }

            ADDRESS -> {
                title = "Địa chỉ"
                binding.layout2.hide()
            }

            PASSWORD -> {
                title = "Mật khẩu"
                binding.layout2.show()
                binding.layout2.hint = "Nhập lại mật khẩu"
            }

            else -> {
                dismiss()
            }
        }

        binding.edtInfo.doAfterTextChanged {
            validateAll()
        }

        binding.edtInfo2.doAfterTextChanged {
            validateAll()
        }

        binding.title.text = "Cập nhật $title"
        binding.edtInfo.hint = title
    }

    private fun validateInfo(value: String): Boolean {
        if (type == PASSWORD) {
            return when {
                value.trim().isEmpty() -> {
                    binding.layout1.error = "Password không được bỏ trống"
                    false
                }
                value.length < 8 -> {
                    binding.layout1.error = "Password không được ngắn hơn 8 kí tự"
                    false
                }
                else -> {
                    binding.layout1.error = null
                    true
                }
            }
        } else {
            return if (value.trim().isEmpty()) {
                binding.layout1.error = "Không được bỏ trống"
                false
            } else {
                binding.layout1.error = null
                true
            }
        }
    }

    private fun validateInfo2(value: String): Boolean {
        if (type != PASSWORD) return true
        return if (value != binding.edtInfo.text.toString()) {
            binding.layout2.error = "Mật khẩu không giống"
            false
        } else {
            binding.layout2.error = null
            true
        }
    }

    private fun validateAll() {
        if (
            validateInfo(binding.edtInfo.text.toString()) &&
            validateInfo2(binding.edtInfo2.text.toString())
        ) {
            binding.btnSave.enable()
        } else {
            binding.btnSave.disable()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentBottomSheetChangeInfoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        configViews()
    }

    companion object {
        @JvmStatic
        fun newInstance(type: String) =
            BottomSheetChangeInfoFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_TYPE, type)
                }
            }
    }
}