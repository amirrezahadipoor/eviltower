package ir.hadipoor.eviltower.util

/** Persian (Eastern Arabic) digit helpers — every number in the UI is localised. */
object PersianNumbers {

    private val faDigits = charArrayOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')

    fun toPersian(value: String): String = buildString(value.length) {
        value.forEach { ch ->
            append(if (ch in '0'..'9') faDigits[ch - '0'] else ch)
        }
    }

    fun toPersian(value: Int): String = toPersian(value.toString())

    fun toPersian(value: Long): String = toPersian(value.toString())

    /** 12345 -> "۱۲٬۳۴۵" */
    fun grouped(value: Int, persian: Boolean = true): String {
        val plain = value.toString().reversed().chunked(3).joinToString(if (persian) "٬" else ",").reversed()
        return if (persian) toPersian(plain) else plain
    }

    fun format(value: Int, persian: Boolean): String = if (persian) toPersian(value) else value.toString()
}
