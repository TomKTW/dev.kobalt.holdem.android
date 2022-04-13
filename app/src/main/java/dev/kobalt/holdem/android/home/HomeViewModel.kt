package dev.kobalt.holdem.android.home

import androidx.lifecycle.viewModelScope
import dev.kobalt.holdem.android.base.BaseViewModel
import dev.kobalt.holdem.android.state.StateEntity
import dev.kobalt.holdem.android.state.toStateEntity
import io.ktor.client.features.websocket.*
import io.ktor.http.cio.websocket.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.*

class HomeViewModel : BaseViewModel() {

    val pageFlow = MutableSharedFlow<HomeFragment.Page>(replay = 1).apply {
        viewModelScope.launch { emit(HomeFragment.Page.NameForm) }
    }

    val messageFlow = MutableSharedFlow<String?>(replay = 1).apply {
        viewModelScope.launch { emit(null) }
    }

    val stateFlow = MutableSharedFlow<StateEntity>(replay = 1).apply {
        viewModelScope.launch { emit(StateEntity(null, null, null)) }
    }

    var session: WebSocketSession? = null

    fun submit(name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            session?.send(Frame.Text("name $name"))
        }
    }

    fun send(data: String) {
        viewModelScope.launch(Dispatchers.IO) {
            session?.send(Frame.Text(data))
        }
    }

    fun connect() {
        viewModelScope.launch(Dispatchers.IO) {
            application.httpClient.webSocket("wss://tom.kobalt.dev/holdem/server/") {
                session = this
                launch {
                    while (true) {
                        send(Frame.Text("ping"))
                        delay(1000)
                    }
                }
                incoming.consumeEach { frame ->
                    when (frame) {
                        is Frame.Text -> frame.readText().also { data ->
                            when (data) {
                                "pong" -> {}
                                else -> Json.parseToJsonElement(data).jsonObject.let {
                                    if (it["message"] is JsonPrimitive) {
                                        messageFlow.emit((it["message"] as? JsonPrimitive)?.contentOrNull)
                                    }
                                    if (it["player"] is JsonObject) {
                                        stateFlow.emit(it.toStateEntity())
                                    }
                                }
                            }
                            println(data)
                        }
                        else -> return@consumeEach
                    }
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch(Dispatchers.IO) {
            session?.send(Frame.Text("leave"))
            session?.close()
        }
    }

}

