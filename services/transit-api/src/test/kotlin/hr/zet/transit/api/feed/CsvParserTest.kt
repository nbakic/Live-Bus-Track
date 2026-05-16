package hr.zet.transit.api.feed

import kotlin.test.Test
import kotlin.test.assertEquals

class CsvParserTest {

    @Test
    fun parsesHeaderAndRows() {
        val csv = "route_id,route_short_name\n1,Tram 1\n109,Bus 109"
        val rows = CsvParser.parse(csv)

        assertEquals(2, rows.size)
        assertEquals("1", rows[0]["route_id"])
        assertEquals("Bus 109", rows[1]["route_short_name"])
    }

    @Test
    fun handlesQuotedFieldWithComma() {
        val csv = "stop_id,stop_name\nS1,\"Vukovarska, 12\""
        val rows = CsvParser.parse(csv)

        assertEquals("Vukovarska, 12", rows.single()["stop_name"])
    }

    @Test
    fun handlesEscapedQuotes() {
        val csv = "id,name\n1,\"He said \"\"hi\"\"\""
        assertEquals("He said \"hi\"", CsvParser.parse(csv).single()["name"])
    }

    @Test
    fun skipsBlankRows() {
        val csv = "id,name\n1,A\n\n2,B\n"
        assertEquals(2, CsvParser.parse(csv).size)
    }

    @Test
    fun emptyInput_returnsEmptyList() {
        assertEquals(0, CsvParser.parse("").size)
    }
}
