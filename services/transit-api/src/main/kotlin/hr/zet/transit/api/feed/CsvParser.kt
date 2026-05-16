package hr.zet.transit.api.feed

/**
 * Minimalan RFC 4180 CSV parser za GTFS static datoteke.
 *
 * Zrcali `CsvParser` iz KMP `:shared` modula — backend i klijent dijele isti
 * GTFS format. Quoted polja mogu sadržavati zareze i nove redove; `""` je
 * escapeani navodnik.
 */
object CsvParser {

    fun parse(content: String): List<Map<String, String>> {
        val rows = splitRows(content)
        if (rows.isEmpty()) return emptyList()
        val header = rows.first()
        return rows.drop(1)
            .filter { it.isNotEmpty() && it.any(String::isNotBlank) }
            .map { row ->
                header.indices.associate { i ->
                    header[i].trim() to (row.getOrNull(i)?.trim() ?: "")
                }
            }
    }

    private fun splitRows(content: String): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        var field = StringBuilder()
        var row = mutableListOf<String>()
        var inQuotes = false
        var i = 0

        fun endField() {
            row.add(field.toString())
            field = StringBuilder()
        }
        fun endRow() {
            endField()
            rows.add(row)
            row = mutableListOf()
        }

        while (i < content.length) {
            val c = content[i]
            when {
                inQuotes && c == '"' && content.getOrNull(i + 1) == '"' -> {
                    field.append('"'); i++
                }
                c == '"' -> inQuotes = !inQuotes
                !inQuotes && c == ',' -> endField()
                !inQuotes && (c == '\n' || c == '\r') -> {
                    if (c == '\r' && content.getOrNull(i + 1) == '\n') i++
                    endRow()
                }
                else -> field.append(c)
            }
            i++
        }
        if (field.isNotEmpty() || row.isNotEmpty()) endRow()
        return rows
    }
}
