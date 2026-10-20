package com.example.cs2lineuptrainer.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LineupRequestsTest {

    @Test
    fun startsEmpty() {
        val requests = LineupRequests()
        assertNull(requests.openInExplorer.value)
        assertNull(requests.showInStats.value)
    }

    @Test
    fun requestIsStoredUntilHandled() {
        val requests = LineupRequests()
        requests.requestOpenInExplorer("a")
        requests.requestShowInStats("b")
        assertEquals("a", requests.openInExplorer.value)
        assertEquals("b", requests.showInStats.value)

        requests.explorerHandled()
        assertNull(requests.openInExplorer.value)
        assertEquals("b", requests.showInStats.value)

        requests.statsHandled()
        assertNull(requests.showInStats.value)
    }
}
