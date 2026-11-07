package vn.blindsupport.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import vn.blindsupport.app.core.CallIntent
import vn.blindsupport.app.core.Contact
import vn.blindsupport.app.core.DigitWords
import vn.blindsupport.app.core.TextUtils
import vn.blindsupport.app.ui.Command
import vn.blindsupport.app.ui.CommandParser
import vn.blindsupport.app.ui.Confirm

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

    @Test fun callIntent_ringerAudible_onlyClearAnswers() {
        // Chuông Zalo lọt vào micro hay bị nghe thành "có", "ừ": khi chuông còn kêu thì không nhận.
        for (t in listOf("có", "ừ", "vâng", "đồng ý")) {
            assertNull(t, CallIntent.parse(t, ringerAudible = true))
        }
        for (t in listOf("nghe", "nghe máy", "bắt máy", "trả lời")) {
            assertEquals(t, CallIntent.ANSWER, CallIntent.parse(t, ringerAudible = true))
        }
        assertEquals(CallIntent.DECLINE, CallIntent.parse("không", ringerAudible = true))
    }

    @Test fun confirm_acceptsShortClearAnswers() {
        for (t in listOf("đúng", "đúng rồi", "Đúng rồi", "phải", "vâng", "ừ", "có", "gọi đi", "đúng đúng rồi")) {
            assertEquals(t, Confirm.YES, Confirm.parse(t))
        }
        for (t in listOf("không", "sai", "không phải", "thôi", "hủy", "không gọi", "phải không")) {
            assertEquals(t, Confirm.NO, Confirm.parse(t))
        }
    }

    @Test fun confirm_rejectsNoiseAndLongSentences() {
        // Lỗi B1: bản cũ nhận cả câu dài có chứa "đúng/có/vâng/gọi".
        for (t in listOf("", "đúng đúng rồi đúng đúng đi đúng", "gọi", "rồi", "có ai gọi", "mẹ có nhà không",
            "ừ thì", "đúng là thế", "gọi điện", "vâng ạ con về")) {
            assertNull(t, Confirm.parse(t))
            assertFalse(t, CommandParser.isYes(t))
        }
        assertTrue(CommandParser.isYes("đúng rồi"))
        assertTrue(CommandParser.isNo("không"))
    }

    @Test fun confirm_phrasesIncludeFillers() {
        assertTrue(Confirm.PHRASES.containsAll(CallIntent.FILLER_WORDS))
        assertFalse("gọi" in Confirm.YES_WORDS)
    }

    @Test fun commands_strict() {
        val me = Contact("Mẹ", "0900000000")
        val contacts = listOf(me)
        // Từ đệm ở hai đầu được bỏ qua.
        assertEquals(Command.CallContact(me), CommandParser.parse("ừ gọi mẹ đi", contacts))
        assertEquals(Command.ReadMessages, CommandParser.parse("đọc tin nhắn nhé", contacts))
        assertEquals(Command.ReadGroupMessages, CommandParser.parse("tin nhóm", contacts))
        // Câu dài chỉ "có chứa" một lệnh (tiếng TV, người xung quanh): không nhận. Lỗi B4.
        for (t in listOf("anh nói to lên cho mọi người nghe", "hôm nay trời nói chậm lại", "con gọi lại cho mẹ sau",
            "mẹ ơi không có gì đâu", "gọi số nhà", "mấy giờ thì về", "ăn cơm chưa")) {
            assertNull(t, CommandParser.parse(t, contacts))
        }
    }

    @Test fun commands_phrasesIncludeFillers() {
        val phrases = CommandParser.phrases(emptyList())
        assertTrue(phrases.containsAll(CallIntent.FILLER_WORDS))
        assertEquals(phrases.size, phrases.distinct().size)
    }

    @Test fun help_doesNotSayVolumeDownDeclines() {
        // Lỗi B3: phím giảm âm lượng chỉ tắt chuông, không từ chối.
        assertFalse(CommandParser.HELP.contains("phím giảm âm lượng để từ chối"))
        assertTrue(CommandParser.HELP.contains("tắt chuông"))
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
