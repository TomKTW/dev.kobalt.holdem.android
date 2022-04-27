package dev.kobalt.holdem.android.play

import android.content.ClipData
import android.content.ClipboardManager
import android.os.Bundle
import android.view.View
import android.widget.SeekBar
import androidx.core.content.ContextCompat
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import com.caverock.androidsvg.SVG
import com.caverock.androidsvg.SVGImageView
import dev.kobalt.holdem.android.R
import dev.kobalt.holdem.android.base.BaseFragment
import dev.kobalt.holdem.android.databinding.PlayBinding
import dev.kobalt.holdem.android.state.HoldemCard
import dev.kobalt.holdem.android.view.PlayerView

class PlayFragment : BaseFragment<PlayBinding>() {

    val viewModel: PlayViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewLifecycleScope.launchWhenCreated {
            viewModel.pageFlow.collect {
                viewBinding?.apply {
                    serverContainer.root.isVisible = it == Page.Server
                    roomContainer.root.isVisible = it == Page.Room
                    tableContainer.root.isVisible = it == Page.Table
                    shareButton.isVisible = it == Page.Table
                    when (it) {
                        Page.Server -> {
                            titleLabel.text = getResourceString(R.string.play_server_title)
                            subtitleLabel.isVisible = false
                            subtitleLabel.text = null
                        }
                        Page.Room -> {
                            titleLabel.text = getResourceString(R.string.play_room_title)
                            subtitleLabel.isVisible = false
                            subtitleLabel.text = null
                        }
                        Page.Table -> {
                            titleLabel.text = getResourceString(R.string.play_table_title)
                            subtitleLabel.isVisible = true
                            subtitleLabel.text =
                                "${viewModel.stateFlow.replayCache.firstOrNull()?.currentRoom?.uid}"
                        }
                    }
                }
            }
        }
        viewLifecycleScope.launchWhenCreated {
            viewModel.messageFlow.collect { message ->
                message?.let {
                    PlayMessageDialogFragment().apply {
                        arguments = Bundle().apply { putString("message", it) }
                    }.show(childFragmentManager, "PlayMessage")
                }
            }
        }
        viewLifecycleScope.launchWhenCreated {
            viewModel.stateFlow.collect {
                when {
                    viewModel.session == null -> viewModel.pageFlow.emit(Page.Server)
                    viewModel.stateFlow.replayCache.firstOrNull()?.currentRoom != null -> viewModel.pageFlow.emit(
                        Page.Table
                    )
                    else -> viewModel.pageFlow.emit(
                        Page.Room
                    )
                }
                viewBinding?.apply {
                    backButton.apply { setOnClickListener { onBackPressed() } }
                    tableContainer.apply {
                        tableCircle.apply {
                            removeAllViews()
                            setCapacity(it.currentRoom?.players?.size ?: 0)
                            (it.currentTable?.players
                                ?: it.currentRoom?.players)?.forEach { player ->
                                addView(PlayerView(requireContext()).apply { apply(player) })
                            }
                        }
                        cardStack.apply {
                            removeAllViews()
                            it.currentTable?.hand?.forEach {
                                addView(SVGImageView(context).apply {
                                    it.src?.let { HoldemCard.valueOf(it) }?.let {
                                        setSVG(SVG.getFromAsset(context.assets, "${it.image}.svg"))
                                    }
                                }, dp(48), dp(48))
                            }
                        }
                        startButton.isVisible = it.currentRoom?.actions?.contains("Start") == true
                        foldButton.isVisible = it.currentTable?.actions?.contains("Fold") == true
                        checkButton.isVisible = it.currentTable?.actions?.contains("Check") == true
                        callButton.isVisible = it.currentTable?.actions?.contains("Call") == true
                        betButton.isVisible = it.currentTable?.actions?.contains("Bet") == true
                        raiseButton.isVisible = it.currentTable?.actions?.contains("Raise") == true
                        allinButton.isVisible = it.currentTable?.actions?.contains("AllIn") == true
                        betLabel.isInvisible =
                            !(it.currentTable?.actions?.contains("Bet") == true || it.currentTable?.actions?.contains(
                                "Raise"
                            ) == true)
                        betSlider.isInvisible =
                            !(it.currentTable?.actions?.contains("Bet") == true || it.currentTable?.actions?.contains(
                                "Raise"
                            ) == true)
                        val currentPlayerUid = it.player?.uid
                        val currentPlayer =
                            it.currentTable?.players?.firstOrNull { it.uid == currentPlayerUid }
                        betSlider.max = currentPlayer?.money ?: 0
                    }
                }
            }
        }
        viewBinding?.apply {
            serverContainer.apply {
                connectButton.setOnClickListener {
                    PlayConnectDialogFragment().show(childFragmentManager, "PlayConnect")
                }
                hostButton.setOnClickListener {
                    PlayHostDialogFragment().show(childFragmentManager, "PlayHost")
                }
            }
            roomContainer.apply {
                createButton.apply {
                    setOnClickListener {
                        viewModel.createRoom()
                    }
                }
                joinButton.apply {
                    setOnClickListener {
                        PlayJoinDialogFragment().show(childFragmentManager, "PlayJoin")
                    }
                }
            }
            shareButton.setOnLongClickListener {
                showToast("Copy Room ID to clipboard", view = shareButton); true
            }
            shareButton.setOnClickListener {
                viewModel.stateFlow.replayCache.firstOrNull()?.currentRoom?.uid?.let {
                    val clipboard: ClipboardManager? =
                        ContextCompat.getSystemService(
                            requireContext(),
                            ClipboardManager::class.java
                        )
                    val clip = ClipData.newPlainText("Room ID", it)
                    clipboard?.setPrimaryClip(clip)
                    showToast("Room ID has been copied to clipboard.")
                }
            }
            tableContainer.apply {
                startButton.setOnClickListener {
                    viewModel.startRoom()
                }
                foldButton.setOnClickListener {
                    viewModel.tableFold()
                }
                checkButton.setOnClickListener {
                    viewModel.tableCheck()
                }
                callButton.setOnClickListener {
                    viewModel.tableCall()
                }
                betButton.setOnClickListener {
                    viewModel.tableBet(betSlider.progress)
                }
                raiseButton.setOnClickListener {
                    viewModel.tableRaise(betSlider.progress)
                }
                allinButton.setOnClickListener {
                    viewModel.tableAllIn()
                }
                betSlider.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(
                        seekBar: SeekBar?,
                        progress: Int,
                        fromUser: Boolean
                    ) {
                        betLabel.text = progress.toString()
                    }

                    override fun onStartTrackingTouch(seekBar: SeekBar?) {
                    }

                    override fun onStopTrackingTouch(seekBar: SeekBar?) {
                    }

                })
            }
        }
    }

    override fun onBackPressed(): Boolean {
        return when {
            viewModel.stateFlow.replayCache.firstOrNull()?.currentRoom != null -> {
                PlayLeaveDialogFragment().show(childFragmentManager, "PlayLeave"); true
            }
            viewModel.session != null -> {
                PlayDisconnectDialogFragment().show(childFragmentManager, "PlayDisconnect"); true
            }
            else -> {
                super.onBackPressed()
            }
        }
    }

    enum class Page {
        Server, Room, Table
    }

}


