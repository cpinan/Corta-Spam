package org.carlospinan.bloqueador.app.rules

object PhoneNumberParser {
    fun parseCountryCode(number: String): String? {
        val cleaned = number.trim().removePrefix("+")
        if (cleaned.isEmpty() || !cleaned.all { it.isDigit() }) return null

        return when {
            cleaned.startsWith("1") && cleaned.length >= 11 -> "1"
            cleaned.startsWith("44") && cleaned.length >= 12 -> "44"
            cleaned.startsWith("34") && cleaned.length >= 11 -> "34"
            cleaned.startsWith("33") && cleaned.length >= 11 -> "33"
            cleaned.startsWith("49") && cleaned.length >= 12 -> "49"
            cleaned.startsWith("39") && cleaned.length >= 11 -> "39"
            cleaned.startsWith("52") && cleaned.length >= 12 -> "52"
            cleaned.startsWith("55") && cleaned.length >= 12 -> "55"
            cleaned.startsWith("81") && cleaned.length >= 12 -> "81"
            cleaned.startsWith("86") && cleaned.length >= 12 -> "86"
            cleaned.startsWith("91") && cleaned.length >= 12 -> "91"
            cleaned.startsWith("61") && cleaned.length >= 11 -> "61"
            cleaned.startsWith("7") && cleaned.length >= 11 -> "7"
            cleaned.startsWith("27") && cleaned.length >= 11 -> "27"
            cleaned.startsWith("20") && cleaned.length >= 11 -> "20"
            cleaned.startsWith("90") && cleaned.length >= 11 -> "90"
            cleaned.startsWith("48") && cleaned.length >= 11 -> "48"
            cleaned.startsWith("31") && cleaned.length >= 11 -> "31"
            cleaned.startsWith("46") && cleaned.length >= 11 -> "46"
            cleaned.startsWith("47") && cleaned.length >= 11 -> "47"
            cleaned.startsWith("45") && cleaned.length >= 11 -> "45"
            cleaned.startsWith("358") && cleaned.length >= 12 -> "358"
            cleaned.startsWith("41") && cleaned.length >= 11 -> "41"
            cleaned.startsWith("43") && cleaned.length >= 11 -> "43"
            cleaned.startsWith("32") && cleaned.length >= 11 -> "32"
            cleaned.startsWith("351") && cleaned.length >= 12 -> "351"
            cleaned.startsWith("30") && cleaned.length >= 11 -> "30"
            cleaned.startsWith("972") && cleaned.length >= 12 -> "972"
            cleaned.startsWith("971") && cleaned.length >= 12 -> "971"
            cleaned.startsWith("966") && cleaned.length >= 12 -> "966"
            cleaned.startsWith("63") && cleaned.length >= 11 -> "63"
            cleaned.startsWith("62") && cleaned.length >= 12 -> "62"
            cleaned.startsWith("66") && cleaned.length >= 11 -> "66"
            cleaned.startsWith("84") && cleaned.length >= 11 -> "84"
            cleaned.startsWith("60") && cleaned.length >= 11 -> "60"
            cleaned.startsWith("65") && cleaned.length >= 11 -> "65"
            cleaned.startsWith("82") && cleaned.length >= 12 -> "82"
            cleaned.startsWith("54") && cleaned.length >= 12 -> "54"
            cleaned.startsWith("56") && cleaned.length >= 11 -> "56"
            cleaned.startsWith("57") && cleaned.length >= 12 -> "57"
            cleaned.startsWith("51") && cleaned.length >= 11 -> "51"
            else -> null
        }
    }
}
