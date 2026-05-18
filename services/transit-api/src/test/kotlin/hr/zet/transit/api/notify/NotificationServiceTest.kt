package hr.zet.transit.api.notify

import java.nio.file.Files
import kotlin.io.path.deleteIfExists
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertTrue

class NotificationServiceTest {

    private val storePath = Files.createTempDirectory("fcm-test").resolve("tokens.txt")

    @AfterTest
    fun cleanup() {
        storePath.deleteIfExists()
        storePath.resolveSibling("tokens.txt.tmp").deleteIfExists()
    }

    @Test
    fun registeredToken_persistsAcrossRestart() {
        // Prva instanca registrira token.
        NotificationService(storePath).registerToken("token-abc")

        // Nova instanca (simulira restart) mora učitati token s diska.
        val reloaded = NotificationService(storePath)
        reloaded.registerToken("token-xyz")

        val lines = Files.readAllLines(storePath)
        assertTrue("token-abc" in lines, "token prije restarta mora preživjeti")
        assertTrue("token-xyz" in lines, "token nakon restarta mora biti dodan")
    }

    @Test
    fun blankToken_isIgnored() {
        val service = NotificationService(storePath)
        service.registerToken("   ")
        assertTrue(!Files.exists(storePath) || Files.readAllLines(storePath).isEmpty())
    }
}
