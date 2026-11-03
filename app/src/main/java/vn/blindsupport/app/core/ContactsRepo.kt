package vn.blindsupport.app.core

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.CallLog
import android.provider.ContactsContract

data class Contact(val name: String, val number: String) {
    val normalizedName: String = TextUtils.normalize(name)
}

data class MissedCall(val name: String?, val number: String?, val time: Long)

data class RecentCall(val name: String?, val number: String?, val type: Int, val time: Long)

object ContactsRepo {

    private fun canReadContacts(c: Context) =
        c.checkSelfPermission(Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED

    private fun canReadCallLog(c: Context) =
        c.checkSelfPermission(Manifest.permission.READ_CALL_LOG) == PackageManager.PERMISSION_GRANTED

    /** Tên trong danh bạ của một số điện thoại, hoặc null. */
    fun lookupName(context: Context, number: String?): String? {
        if (number.isNullOrBlank() || !canReadContacts(context)) return null
        val uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number))
        return runCatching {
            context.contentResolver.query(
                uri, arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME), null, null, null
            )?.use { c -> if (c.moveToFirst()) c.getString(0) else null }
        }.getOrNull()
    }

    /** Toàn bộ danh bạ có số điện thoại, mỗi tên lấy một số (ưu tiên số mặc định). */
    fun allContacts(context: Context): List<Contact> {
        if (!canReadContacts(context)) return emptyList()
        val result = LinkedHashMap<String, Contact>()
        runCatching {
            context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                    ContactsContract.CommonDataKinds.Phone.NUMBER,
                    ContactsContract.CommonDataKinds.Phone.IS_SUPER_PRIMARY,
                ),
                null, null,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
            )?.use { c ->
                while (c.moveToNext()) {
                    val name = c.getString(0) ?: continue
                    val number = c.getString(1) ?: continue
                    val primary = c.getInt(2) == 1
                    val contact = Contact(name.trim(), number)
                    if (contact.normalizedName.isEmpty()) continue
                    val existing = result[contact.normalizedName]
                    if (existing == null || primary) result[contact.normalizedName] = contact
                }
            }
        }
        return result.values.toList()
    }

    /** Mọi số điện thoại trong danh bạ (một người nhiều số thì có nhiều dòng), xếp theo tên. */
    fun allPhoneEntries(context: Context): List<Contact> {
        if (!canReadContacts(context)) return emptyList()
        val seen = HashSet<String>()
        val out = ArrayList<Contact>()
        runCatching {
            context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                    ContactsContract.CommonDataKinds.Phone.NUMBER,
                ),
                null, null,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " COLLATE LOCALIZED ASC"
            )?.use { c ->
                while (c.moveToNext()) {
                    val name = c.getString(0)?.trim().orEmpty()
                    val number = c.getString(1) ?: continue
                    val key = name + "|" + number.filter { it.isDigit() }
                    if (!seen.add(key)) continue
                    out += Contact(name.ifEmpty { number }, number)
                }
            }
        }
        return out
    }

    /** Nhật ký cuộc gọi gần nhất (mọi loại), mới nhất trước. */
    fun recentCalls(context: Context, limit: Int): List<RecentCall> {
        if (!canReadCallLog(context)) return emptyList()
        val list = mutableListOf<RecentCall>()
        runCatching {
            context.contentResolver.query(
                CallLog.Calls.CONTENT_URI,
                arrayOf(CallLog.Calls.CACHED_NAME, CallLog.Calls.NUMBER, CallLog.Calls.TYPE, CallLog.Calls.DATE),
                null, null,
                CallLog.Calls.DATE + " DESC"
            )?.use { c ->
                while (c.moveToNext() && list.size < limit) {
                    val number = c.getString(1)?.takeIf { it.isNotBlank() }
                    val name = c.getString(0)?.takeIf { it.isNotBlank() }
                    list += RecentCall(name, number, c.getInt(2), c.getLong(3))
                }
            }
        }
        return list
    }

    /** Cuộc gọi nhỡ trong [withinMs] gần nhất, mới nhất trước. */
    fun recentMissedCalls(context: Context, withinMs: Long = 24 * 3600_000L): List<MissedCall> {
        if (!canReadCallLog(context)) return emptyList()
        val since = System.currentTimeMillis() - withinMs
        val list = mutableListOf<MissedCall>()
        runCatching {
            context.contentResolver.query(
                CallLog.Calls.CONTENT_URI,
                arrayOf(CallLog.Calls.CACHED_NAME, CallLog.Calls.NUMBER, CallLog.Calls.DATE),
                "${CallLog.Calls.TYPE} = ? AND ${CallLog.Calls.DATE} > ?",
                arrayOf(CallLog.Calls.MISSED_TYPE.toString(), since.toString()),
                CallLog.Calls.DATE + " DESC"
            )?.use { c ->
                while (c.moveToNext() && list.size < 10) {
                    val number = c.getString(1)
                    val name = c.getString(0)?.takeIf { it.isNotBlank() } ?: lookupName(context, number)
                    list += MissedCall(name, number, c.getLong(2))
                }
            }
        }
        return list
    }

    /** Số của cuộc gọi đến/gọi nhỡ gần nhất (dùng cho lệnh "gọi lại"). */
    fun lastIncomingNumber(context: Context): Pair<String?, String>? {
        if (!canReadCallLog(context)) return null
        return runCatching {
            context.contentResolver.query(
                CallLog.Calls.CONTENT_URI,
                arrayOf(CallLog.Calls.CACHED_NAME, CallLog.Calls.NUMBER),
                "${CallLog.Calls.TYPE} IN (?, ?)",
                arrayOf(CallLog.Calls.MISSED_TYPE.toString(), CallLog.Calls.INCOMING_TYPE.toString()),
                CallLog.Calls.DATE + " DESC"
            )?.use { c ->
                if (c.moveToFirst()) {
                    val number = c.getString(1) ?: return@use null
                    val name = c.getString(0)?.takeIf { it.isNotBlank() } ?: lookupName(context, number)
                    name to number
                } else null
            }
        }.getOrNull()
    }
}
