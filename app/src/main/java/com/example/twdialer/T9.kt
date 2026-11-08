package com.example.twdialer

import android.content.Context
import android.provider.ContactsContract.CommonDataKinds.Phone
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.Normalizer

data class ContactMatch(val name: String, val number: String)

class IndexedContact(val contact: ContactMatch) {
    val numberDigits: String = contact.number.filter(Char::isDigit)
    val nameDigits: String = T9.toDigits(contact.name)
    val wordDigits: List<String> =
        contact.name.split(' ', '-', '.', '_').filter { it.isNotBlank() }.map(T9::toDigits)
}

object T9 {
    private val letterToDigit: Map<Char, Char> = buildMap {
        mapOf(
            '2' to "abc", '3' to "def", '4' to "ghi", '5' to "jkl",
            '6' to "mno", '7' to "pqrs", '8' to "tuv", '9' to "wxyz",
        ).forEach { (d, letters) -> letters.forEach { put(it, d) } }
    }

    private val combiningMarks = Regex("\\p{Mn}+")

    private fun normalize(s: String): String =
        Normalizer.normalize(s.lowercase(), Normalizer.Form.NFD)
            .replace(combiningMarks, "")
            .replace('đ', 'd')

    fun toDigits(text: String): String {
        val sb = StringBuilder()
        for (c in normalize(text)) {
            if (c.isDigit()) sb.append(c) else letterToDigit[c]?.let(sb::append)
        }
        return sb.toString()
    }

    fun filter(all: List<IndexedContact>, query: String, limit: Int = 30): List<ContactMatch> {
        val q = query.filter(Char::isDigit)
        if (q.isEmpty()) return emptyList()
        return all.mapNotNull { c ->
            val score = when {
                c.wordDigits.any { it.startsWith(q) } -> 0
                c.numberDigits.startsWith(q) -> 1
                c.nameDigits.contains(q) -> 2
                c.numberDigits.contains(q) -> 3
                else -> null
            }
            score?.let { it to c.contact }
        }
            .sortedWith(compareBy({ it.first }, { it.second.name }))
            .take(limit)
            .map { it.second }
    }
}

suspend fun loadContacts(context: Context): List<IndexedContact> = withContext(Dispatchers.IO) {
    val out = ArrayList<IndexedContact>()
    context.contentResolver.query(
        Phone.CONTENT_URI,
        arrayOf(Phone.DISPLAY_NAME, Phone.NUMBER),
        null, null,
        "${Phone.DISPLAY_NAME} COLLATE NOCASE ASC",
    )?.use { c ->
        val nameCol = c.getColumnIndexOrThrow(Phone.DISPLAY_NAME)
        val numCol = c.getColumnIndexOrThrow(Phone.NUMBER)
        while (c.moveToNext()) {
            val name = c.getString(nameCol) ?: continue
            val num = c.getString(numCol) ?: continue
            out += IndexedContact(ContactMatch(name, num))
        }
    }
    out.distinctBy { it.contact }
}
