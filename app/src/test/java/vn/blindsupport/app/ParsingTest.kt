package vn.blindsupport.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import vn.blindsupport.app.core.CallIntent
import vn.blindsupport.app.core.Contact
import vn.blindsupport.app.core.DigitWords
import vn.blindsupport.app.core.TextUtils
import vn.blindsupport.app.ui.Command
import vn.blindsupport.app.ui.CommandParser

class ParsingTest {

    @Test fun callIntent_acceptsClearAnswers() {
        for (t in listOf("nghe", "nghe máy", "Nghe đi", "có", "vâng", "trả lời", "bắt máy")) {
            assertEquals(t, CallIntent.ANSWER, CallIntent.parse(t))
        }
    }

    @Test fun callIntent_acceptsClearDeclines() {
        for (t in listOf("không", "không nghe", "từ chối", "tắt máy", "cúp máy")) {
            assertEquals(t, CallIntent.DECLINE, CallIntent.parse(t))
        }
    }

    @Test fun callIntent_ignoresNoiseLikeResults() {
        // Từ một âm tiết dễ bị tiếng ồn tạo ra, câu lẫn từ khác, câu quá dài: không được tính.
        for (t in listOf("", "tắt", "bận", "thôi", "à nghe", "mẹ ơi", "không có không không",
            "có không", "gọi điện", "chuông", "nghe à")) {
            assertNull(t, CallIntent.parse(t))
        }
    }

    @Test fun digits() {
        assertEquals("0912345678", DigitWords.toDigits("không chín một hai ba bốn năm sáu bảy tám"))
        assertEquals("154", DigitWords.toDigits("mốt lăm tư"))
        assertEquals("07", DigitWords.toDigits("linh bẩy"))
        assertEquals("", DigitWords.toDigits("xong rồi"))
    }

    @Test fun commands() {
        val me = Contact("Mẹ", "0900000000")
        val hung = Contact("Anh Hưng", "0911111111")
        val contacts = listOf(me, hung)
        assertEquals(Command.CallNumber("091"), CommandParser.parse("gọi số không chín một", contacts))
        assertEquals(Command.CallNumber(""), CommandParser.parse("gọi số", contacts))
        assertEquals(Command.CallContact(me), CommandParser.parse("gọi mẹ", contacts))
        assertEquals(Command.CallContact(hung), CommandParser.parse("gọi cho hưng", contacts))
        assertEquals(Command.CallBack, CommandParser.parse("gọi lại", contacts))
        assertEquals(Command.Time, CommandParser.parse("mấy giờ rồi", contacts))
        assertNull(CommandParser.parse("giờ", contacts))
        assertNull(CommandParser.parse("ngày", contacts))
        assertNull(CommandParser.parse("hai", contacts))
    }

    @Test fun textUtils() {
        assertEquals("nguyen duc", TextUtils.fold("Nguyễn Đức"))
        assertEquals("0 9 1 2, 3 4 5, 6 7 8", TextUtils.speakableNumber("0912345678"))
        assertEquals("0 9 1 2, 3 4 5, 6 7 8", TextUtils.speakableNumber("+84912345678"))
    }
}
