package vn.blindsupport.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import vn.blindsupport.app.notify.CallScreenText

class CallScreenTextTest {
    @Test
    fun zaloIncomingScreen_findsCaller() {
        val texts = listOf("Zalo", "Con Gái", "Zalo: Cuộc gọi thoại đến", "Từ chối", "Trả lời")
        assertEquals("Con Gái", CallScreenText.incomingCaller(texts))
    }

    @Test
    fun zaloVideoScreen_nameOnSameLine() {
        val texts = listOf("Hưng PA: Cuộc gọi video đến", "Từ chối", "Trả lời")
        assertEquals("Hưng PA", CallScreenText.incomingCaller(texts))
    }

    @Test
    fun chatScreenWithCallHistory_isNotIncoming() {
        val texts = listOf("Anh Sơn Phật Tử", "Cuộc gọi thoại đến", "Gọi lại", "Tin nhắn")
        assertNull(CallScreenText.incomingCaller(texts))
    }

    @Test
    fun missedCall_isNotIncoming() {
        assertFalse(CallScreenText.isIncomingText("Cuộc gọi thoại đến bị nhỡ"))
        assertTrue(CallScreenText.isIncomingText("Zalo: Cuộc gọi thoại đến"))
    }

    @Test
    fun notificationCaller() {
        assertEquals("Con Gái", CallScreenText.callerFromNotification("Con Gái", "Cuộc gọi thoại đến"))
        assertEquals("Con Gái", CallScreenText.callerFromNotification("Zalo", "Con Gái: Cuộc gọi thoại đến"))
        assertNull(CallScreenText.callerFromNotification("Zalo", "Cuộc gọi thoại đến"))
    }
}
