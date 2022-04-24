package dev.kobalt.holdem.android.play

import android.app.Dialog
import android.os.Bundle
import dev.kobalt.holdem.android.base.BaseDialogFragment
import dev.kobalt.holdem.android.databinding.PlayLeaveBinding

class PlayLeaveDialogFragment : BaseDialogFragment<PlayLeaveBinding>() {

    private val playFragment get() = parentFragment as? PlayFragment

    override fun onDialogCreated(dialog: Dialog, savedInstanceState: Bundle?) {
        super.onDialogCreated(dialog, savedInstanceState)
        viewBinding?.apply {
            cancelButton.apply {
                setOnClickListener { dismiss() }
            }
            confirmButton.apply {
                setOnClickListener { playFragment?.viewModel?.leaveRoom(); dismiss() }
            }
        }
    }

}