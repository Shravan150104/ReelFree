package com.reelfree.detection.model

/**
 * The closed set of short UI labels ReelFree is allowed to remember.
 * Any label that is not one of these terms is discarded before it leaves the snapshotter.
 */
class LabelVocabulary(terms: Collection<String>) {

    private val terms: List<String> = terms.map { normalize(it) }
        .filter { it.isNotEmpty() }
        .distinct()
        .sortedByDescending { it.length }

    /**
     * Returns the vocabulary term that [raw] represents, or null.
     *
     * Matches exactly, or when the term is followed by a separator commonly used in
     * accessibility labels ("Reels, tab 3 of 5", "Reels tab", "Home.").
     */
    fun match(raw: CharSequence?): String? {
        if (raw.isNullOrEmpty() || raw.length > MAX_LABEL_LENGTH) return null
        val value = normalize(raw.toString())
        for (term in terms) {
            if (value == term) return term
            if (value.length > term.length && value.startsWith(term)) {
                val rest = value.substring(term.length)
                if (SEPARATORS.any { rest.startsWith(it) }) return term
            }
        }
        return null
    }

    companion object {
        const val MAX_LABEL_LENGTH = 40
        private val SEPARATORS = listOf(",", ".", " tab", ":", " (")

        fun normalize(value: String): String = value.trim().lowercase()
    }
}
