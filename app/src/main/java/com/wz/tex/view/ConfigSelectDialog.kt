package com.wz.tex.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.children
import com.google.android.material.chip.Chip
import com.wz.base.NetUtil
import com.wz.tex.R
import com.wz.tex.base.BaseBottomSheetDialogFragment
import com.wz.tex.databinding.DialogConfigItemBinding
import com.wz.tex.view.ConfigViewActivity.Companion.FROM_CLIENT
import com.wz.tex.view.ConfigViewActivity.Companion.FROM_CONTROL

class ConfigSelectDialog : BaseBottomSheetDialogFragment<DialogConfigItemBinding>() {

    private lateinit var callback: (fileName: String, isEdit: Boolean) -> Unit
    private lateinit var currentFileName: String
    private var from = FROM_CONTROL

    override fun newViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ) = DialogConfigItemBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val files = when (from) {
            FROM_CONTROL -> {
                NetUtil.getSConfigFileList()
            }
            FROM_CLIENT -> {
                NetUtil.getCConfigFileList()
            }
            else -> arrayOf()
        }.toMutableList()
        if (files.size < 5) {
            for (i in (files.size + 1)..5) {
                files.add("layout${i}.txt")
            }
        }
        files.forEach(::addChoices)
        binding.ok.setOnClickListener {
            clickOK()
        }
    }

    private fun addChoices(entry: String) {
        val inflate = layoutInflater.inflate(R.layout.chip_item_config, binding.chipGroup, false) as Chip
        if (currentFileName == entry) inflate.isChecked = true
        inflate.text = entry
        inflate.setOnCloseIconClickListener {
            callback.invoke(inflate.text.toString(), true)
        }
        binding.chipGroup.addView(inflate)
    }

    private fun clickOK() {
        if (binding.chipGroup.childCount == 0) {
            dismiss()
        }
        binding.chipGroup.children.forEach {
            if (it is Chip && it.isChecked) {
                callback.invoke(it.text.toString(), false)
            }
            dismiss()
        }
        Toast.makeText(requireContext(), "未选中", Toast.LENGTH_SHORT).show()
    }

    companion object {
        fun newInstance(from: Int, currentFileName: String, callback: (fileName: String, isEdit: Boolean) -> Unit): ConfigSelectDialog {
            return  ConfigSelectDialog().apply {
                this.from = from
                this.currentFileName = currentFileName
                this.callback = callback
            }
        }
    }
}