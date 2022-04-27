package dev.kobalt.holdem

import dev.kobalt.holdem.android.state.StateEntity
import dev.kobalt.holdem.android.state.toStateEntity
import dev.kobalt.holdem.android.state.toStateEntityPlayer
import dev.kobalt.holdem.android.state.toStateEntityRoom
import kotlinx.serialization.json.*
import org.junit.Assert.assertEquals
import org.junit.Test

class JsonConvertUnitTest {

    private val jsonClientPlayer = buildJsonObject {
        put("uid", "80abb943c94a4a83a34fbw")
        put("name", "Player")
    }

    private val jsonRoom = buildJsonObject {
        put("uid", "6de3869a088d462398a83Q")
        put("status", "Open")
        put("playerLimit", 10)
        put("players", buildJsonArray { add(jsonClientPlayer) })
        put("actions", buildJsonArray { add("Leave") })
    }

    private val jsonState = buildJsonObject {
        put("player", jsonClientPlayer)
        put("currentRoom", jsonRoom)
        put("currentTable", JsonNull)
    }

    private val expectedPlayer = StateEntity.Player(
        "80abb943c94a4a83a34fbw",
        "Player",
        emptyList(),
        null,
        null,
        null,
        emptyList()
    )

    private val expectedRoom = StateEntity.Room(
        "6de3869a088d462398a83Q",
        "Open",
        10,
        listOf(expectedPlayer),
        listOf("Leave")
    )

    private val expectedTable = null

    private val expectedState = StateEntity(
        expectedPlayer,
        expectedRoom,
        expectedTable
    )

    @Test
    fun validateStateEntityPlayerJsonConversion() {
        assertEquals(jsonClientPlayer.toStateEntityPlayer(), expectedPlayer)
    }

    @Test
    fun validateStateEntityRoomJsonConversion() {
        assertEquals(jsonRoom.toStateEntityRoom(), expectedRoom)
    }

    @Test
    fun validateStateEntityJsonConversion() {
        assertEquals(jsonState.toStateEntity(), expectedState)
    }

}