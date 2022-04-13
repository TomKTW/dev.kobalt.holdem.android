package dev.kobalt.holdem.android.home

import android.content.ClipData
import android.content.ClipboardManager
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat.getSystemService
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import com.caverock.androidsvg.SVG
import com.caverock.androidsvg.SVGImageView
import dev.kobalt.holdem.android.base.BaseFragment
import dev.kobalt.holdem.android.databinding.HomeBinding
import dev.kobalt.holdem.android.state.HoldemCard
import dev.kobalt.holdem.android.view.PlayerView

class HomeFragment : BaseFragment<HomeBinding>() {

    private val viewModel: HomeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.connect()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewLifecycleScope.launchWhenCreated {
            viewModel.pageFlow.collect {
                viewBinding?.apply {
                    nameFormContainer.root.isVisible = it == Page.NameForm
                    roomFormContainer.root.isVisible = it == Page.RoomForm
                    tableContainer.root.isVisible = it == Page.Table
                }
            }
        }
        viewLifecycleScope.launchWhenCreated {
            viewModel.messageFlow.collect { message ->
                message?.let {
                    AlertDialog.Builder(requireContext())
                        .setMessage(it)
                        .setPositiveButton("OK", null)
                        .create().show()
                }
            }
        }
        viewLifecycleScope.launchWhenCreated {
            viewModel.stateFlow.collect {
                viewModel.pageFlow.emit(
                    when {
                        viewModel.stateFlow.replayCache.firstOrNull()?.currentRoom != null -> Page.Table
                        viewModel.stateFlow.replayCache.firstOrNull()?.player?.name != null -> Page.RoomForm
                        else -> Page.NameForm
                    }
                )
                viewBinding?.apply {
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
                        titleLabel.text = "Room ${it.currentRoom?.uid?.substring(0, 6)}..."
                        subtitleLabel.text = when {
                            it.currentTable?.phase != null -> it.currentTable.phase
                            ((it.currentRoom?.players.orEmpty().size) > 1) -> "Ready to play."
                            else -> "Waiting for players..."
                        }
                        leaveButton.isVisible = it.currentRoom?.actions?.contains("Leave") == true
                        startButton.isVisible = it.currentRoom?.actions?.contains("Start") == true
                        foldButton.isVisible = it.currentTable?.actions?.contains("Fold") == true
                        checkButton.isVisible = it.currentTable?.actions?.contains("Check") == true
                        callButton.isVisible = it.currentTable?.actions?.contains("Call") == true
                        betButton.isVisible = it.currentTable?.actions?.contains("Bet") == true
                        raiseButton.isVisible = it.currentTable?.actions?.contains("Raise") == true
                        allinButton.isVisible = it.currentTable?.actions?.contains("AllIn") == true
                        betSlider.isInvisible =
                            !(it.currentTable?.actions?.contains("Bet") == true || it.currentTable?.actions?.contains(
                                "Raise"
                            ) == true)
                        val currentPlayerUid = it.player?.uid
                        val currentPlayer =
                            it.currentTable?.players?.first { it.uid == currentPlayerUid }
                        betSlider.max = currentPlayer?.money ?: 0
                    }
                }
            }
        }
        viewBinding?.apply {
            tableContainer.copyRoomIdButton.setOnClickListener {
                viewModel.stateFlow.replayCache.firstOrNull()?.currentRoom?.uid?.let {
                    val clipboard: ClipboardManager? =
                        getSystemService(requireContext(), ClipboardManager::class.java)
                    val clip = ClipData.newPlainText("Room ID", it)
                    clipboard?.setPrimaryClip(clip)
                }
            }
            nameFormContainer.apply {
                nameInput.setText(application.preferences.name)
                nameSubmitButton.setOnClickListener {
                    application.preferences.name = nameInput.text?.toString() ?: ""
                    viewModel.submit(nameInput.text?.toString() ?: "")
                }
            }
            roomFormContainer.apply {
                joinButton.setOnClickListener {
                    joinInputContainer.isVisible = true
                    joinButton.isVisible = false
                    createButton.isVisible = false
                    roomIdInput.requestFocus()
                    inputMethodManager.showSoftInput(
                        roomFormContainer.roomIdInput,
                        InputMethodManager.SHOW_IMPLICIT
                    )
                }
                joinBackButton.setOnClickListener {
                    joinInputContainer.isVisible = false
                    joinButton.isVisible = true
                    createButton.isVisible = true
                }
                joinInputButton.setOnClickListener {
                    inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
                    viewModel.send(
                        "join ${
                            roomFormContainer.roomIdInput.text?.toString().orEmpty()
                        }"
                    )
                }
                createButton.setOnClickListener {
                    viewModel.send("create")
                }
            }
            tableContainer.startButton.setOnClickListener {
                viewModel.send("start")
            }
            tableContainer.leaveButton.setOnClickListener {
                viewModel.send("leave room")
            }
            tableContainer.foldButton.setOnClickListener {
                viewModel.send("fold")
            }
            tableContainer.checkButton.setOnClickListener {
                viewModel.send("check")
            }
            tableContainer.callButton.setOnClickListener {
                viewModel.send("call")
            }
            tableContainer.betButton.setOnClickListener {
                viewModel.send("bet ${tableContainer.betSlider.progress}")
            }
            tableContainer.raiseButton.setOnClickListener {
                viewModel.send("raise")
            }
            tableContainer.allinButton.setOnClickListener {
                viewModel.send("allin")
            }
        }
    }

    enum class Page {
        NameForm, RoomForm, Table
    }

}


