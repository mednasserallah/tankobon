package eu.kanade.tachiyomi.ui.reader.textdetection

/**
 * Normalizes the ALL-CAPS lettering conventional in manga to sentence case, applied after line
 * merging and before both display and translation.
 *
 * Rules: lowercase everything, then capitalize the first letter of the string and the first letter
 * after a sentence terminator (`.`, `!`, `?`), and restore the standalone pronoun "i" to "I".
 *
 * This is a display/readability heuristic, NOT grammar correction. Known limitations:
 *  - Acronyms and initialisms are lowercased ("NASA" → "Nasa", "FBI" → "Fbi").
 *  - Proper nouns lose their capital ("Guts" → "Guts" only if it began a sentence, else "guts").
 *  - Abbreviations with internal periods ("Mr.") cause a spurious capital on the next word.
 * These are acceptable for a best-effort convenience feature; better casing would need a dictionary
 * or NLP model, which is out of scope.
 */
object SentenceCase {

    private val standaloneI = Regex("\\bi\\b")

    fun normalize(text: String): String {
        if (text.isBlank()) return text

        val lowered = text.lowercase()
        val builder = StringBuilder(lowered.length)
        var capitalizeNext = true
        for (char in lowered) {
            if (capitalizeNext && char.isLetter()) {
                builder.append(char.uppercaseChar())
                capitalizeNext = false
            } else {
                builder.append(char)
                if (char == '.' || char == '!' || char == '?') {
                    capitalizeNext = true
                }
            }
        }

        return standaloneI.replace(builder.toString(), "I")
    }
}
