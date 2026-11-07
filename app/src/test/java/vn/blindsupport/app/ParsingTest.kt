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
import vn.blindsupport.app.ui.Choice
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

    @Test fun confirm_phrasesHaveNoCloseSoundingDecoys() {
        assertFalse("gọi" in Confirm.YES_WORDS)
        for (w in listOf("đó", "đi", "được", "nam")) assertFalse(w, w in Confirm.PHRASES)
    }

    private val aAnh = Contact("A Anh", "0911000001")
    private val anhHung = Contact("Anh Hưng", "0911000002")
    private val chiTrang = Contact("Chị Trang", "0911000003")
    private val trangNguyen = Contact("Trang Nguyễn", "0911000004")
    private val me = Contact("Mẹ", "0900000000")
    private val book = listOf(aAnh, anhHung, chiTrang, trangNguyen, me)

    @Test fun call_byFullOrPartialName() {
        assertEquals(Command.CallContact(aAnh), CommandParser.parse("gọi a anh", book))
        assertEquals(Command.CallContact(chiTrang), CommandParser.parse("gọi chị trang", book))
        assertEquals(Command.CallContact(chiTrang), CommandParser.parse("gọi cho chị trang đi", book))
        assertEquals(Command.CallContact(anhHung), CommandParser.parse("gọi hưng", book))
        assertEquals(Command.CallContact(anhHung), CommandParser.parse("gọi điện cho anh hưng", book))
        assertEquals(Command.CallContact(me), CommandParser.parse("gọi mẹ", book))
        // Không dấu vẫn khớp.
        assertEquals(Command.CallContact(anhHung), CommandParser.parse("gọi hung", book))
        // Thứ tự chữ khác vẫn khớp.
        assertEquals(Command.CallContact(trangNguyen), CommandParser.parse("gọi nguyễn trang", book))
    }

    @Test fun call_ambiguousNameGivesChoices() {
        val anh = CommandParser.parse("gọi anh", book)
        assertTrue(anh is Command.ChooseContact)
        assertEquals(setOf(aAnh, anhHung), (anh as Command.ChooseContact).candidates.toSet())
        val trang = CommandParser.parse("gọi trang", book) as Command.ChooseContact
        assertEquals(setOf(chiTrang, trangNguyen), trang.candidates.toSet())
        // Nói "chị trang" khi danh bạ không có chữ "chị": lấy người khớp nhiều chữ nhất.
        val book2 = listOf(trangNguyen, anhHung)
        assertEquals(Command.CallContact(trangNguyen), CommandParser.parse("gọi chị trang", book2))
    }

    @Test fun call_tooManyOrNotFound() {
        val many = (1..9).map { Contact("Anh Số $it", "09000000$it") }
        assertTrue(CommandParser.parse("gọi anh", many) is Command.TooManyContacts)
        assertEquals(Command.ContactNotFound("lan"), CommandParser.parse("gọi lan", book))
        assertEquals(Command.CallWho, CommandParser.parse("gọi", book))
        assertEquals(Command.CallWho, CommandParser.parse("gọi cho", book))
    }

    @Test fun call_byNumber() {
        assertEquals(Command.CallNumber("0345678912"),
            CommandParser.parse("gọi không ba bốn năm sáu bảy tám chín một hai", book))
        assertEquals(Command.CallNumber("0345678912"),
            CommandParser.parse("gọi số không ba bốn năm sáu bảy tám chín một hai", book))
        assertEquals(Command.CallNumber("0345"), CommandParser.parse("gọi cho không ba bốn lăm", book))
        // Lẫn một chữ nghe nhầm giữa các số vẫn là gọi số.
        assertEquals(Command.CallNumber("034567"), CommandParser.parse("gọi không ba bốn mẹ năm sáu bảy", book))
    }

    @Test fun choice() {
        assertEquals(Choice.Pick(1), Choice.parse("hai", 3))
        assertEquals(Choice.Pick(1), Choice.parse("số hai", 3))
        assertEquals(Choice.Pick(2), Choice.parse("người thứ ba", 3))
        assertEquals(Choice.Pick(0), Choice.parse("thứ nhất", 3))
        assertEquals(Choice.Pick(0), Choice.parse("một", 3))
        assertEquals(Choice.Pick(3), Choice.parse("thứ tư", 4))
        assertEquals(Choice.Cancel, Choice.parse("không", 3))
        assertEquals(Choice.Cancel, Choice.parse("hủy", 3))
        assertNull(Choice.parse("năm", 3))
        assertNull(Choice.parse("hai ba", 3))
        assertNull(Choice.parse("mẹ ơi", 3))
    }

    @Test fun commands_lenientButNotLongSentences() {
        assertEquals(Command.ReadMessages, CommandParser.parse("đọc tin nhắn nhé", book))
        assertEquals(Command.ReadMessages, CommandParser.parse("đọc tin nhắn à nhé", book))
        assertEquals(Command.ReadGroupMessages, CommandParser.parse("tin nhóm", book))
        assertEquals(Command.Time, CommandParser.parse("bây giờ là mấy giờ", book))
        assertEquals(Command.CallBack, CommandParser.parse("gọi lại", book))
        for (t in listOf("anh nói to lên cho mọi người nghe", "hôm nay trời nói chậm lại", "con gọi lại cho mẹ sau",
            "ăn cơm chưa", "à ạ ơi nhé")) {
            assertNull(t, CommandParser.parse(t, book))
        }
    }

    @Test fun commands_phrasesHaveNamesDigitsButNoDecoys() {
        val phrases = CommandParser.phrases(book)
        assertTrue(phrases.containsAll(listOf("a anh", "a", "anh", "chị trang", "trang", "không", "năm")))
        for (w in listOf("nam", "mô", "ăn", "người")) assertFalse(w, w in phrases)
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
