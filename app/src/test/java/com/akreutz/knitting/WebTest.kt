package com.akreutz.knitting

import com.akreutz.knitting.data.PatternType
import com.akreutz.knitting.data.Project
import com.akreutz.knitting.data.ProjectStatus
import com.akreutz.knitting.data.RowPattern
import com.akreutz.knitting.data.Step
import com.akreutz.knitting.data.StepType
import com.akreutz.knitting.data.activeProject
import com.akreutz.knitting.web.CounterWebServer
import com.akreutz.knitting.web.WebProtocol
import com.akreutz.knitting.web.WebStep
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.HttpURLConnection
import java.net.URL

class WebTest {
    private val project = Project(id = 1, name = "Scarf \"One\"", description = null, status = ProjectStatus.InProgress)

    private fun rows(id: Long, progress: Int, projectId: Long = 1) = Step(
        id = id, projectId = projectId, name = "Rows $id", type = StepType.PlainRows, targetRows = 10, progress = progress,
    )

    @Test
    fun activeProjectListsOnlyItsCountedSteps() {
        val uncounted = Step(id = 5, projectId = 1, name = "Cast on", type = StepType.CastOn)
        val steps = listOf(rows(1, 10), uncounted, rows(2, 3), rows(3, 0, projectId = 2))
        val active = activeProject(listOf(project), steps)
        assertEquals(listOf(1L, 2L), active?.steps?.map { it.id })
        assertEquals(2L, active?.current?.id)
    }

    @Test
    fun noActiveProjectWithoutOneInProgress() {
        assertNull(activeProject(listOf(project.copy(status = ProjectStatus.Created)), listOf(rows(1, 0))))
    }

    @Test
    fun encodesStateAsJsonWithEscapedNames() {
        val json = WebProtocol.encode(activeProject(listOf(project), listOf(rows(1, 4))))
        assertEquals(
            """{"project":"Scarf \"One\"","description":null,"progress":0.4000,"steps":[{"id":1,"name":"Rows 1","type":"PlainRows","prog":4,"max":10,""" +
                """"inc":1,"dec":1,"current":true,"done":false}]}""",
            json,
        )
        assertEquals("""{"project":null}""", WebProtocol.encode(null))
    }


    @Test
    fun encodesGridColorAndDetails() {
        val step = Step(
            id = 7, projectId = 1, name = "Cables", type = StepType.Pattern, patternType = PatternType.Cables,
            patternRows = 2, patternColumns = 2, patternCells = "0123", patternRepeats = 2, color = "Forest green",
        )
        val cast = Step(
            id = 8, projectId = 1, name = "Cast on", type = StepType.CastOn, stitchCount = 20, method = "Long-tail",
            needleSize = "4.0 mm", color = "Sunrise", rowPattern = RowPattern.Ribbing1x1,
        )
        val json = WebProtocol.encode(activeProject(listOf(project), listOf(step, cast)))
        assertTrue(json.contains(""""row":0,"rows":2,"cols":2,"ptype":"Cables","cells":"0123","color":"Forest green","shade":"#3f5a3a""""))
        assertTrue(json.contains(""""color":"Sunrise","rowPattern":"Ribbing1x1","details":[{"k":"method","v":"Long-tail"},{"k":"needle","v":"4.0 mm"}]"""))
        assertFalse(json.substringAfter(""""id":8""").contains("shade"))
    }
    @Test
    fun parsesStepTaps() {
        assertEquals(WebStep(12, 1), WebProtocol.parseStep("step=12&delta=1"))
        assertEquals(WebStep(12, -1), WebProtocol.parseStep("delta=-1&step=12"))
        assertNull(WebProtocol.parseStep("step=12&delta=5"))
        assertNull(WebProtocol.parseStep("step=x&delta=1"))
        assertNull(WebProtocol.parseStep(""))
    }

    private fun request(port: Int, path: String, method: String = "GET", pin: String? = null, body: String? = null): Pair<Int, String> {
        val connection = URL("http://127.0.0.1:$port$path").openConnection() as HttpURLConnection
        connection.requestMethod = method
        if (pin != null) connection.setRequestProperty("X-Pin", pin)
        if (body != null) {
            connection.doOutput = true
            connection.outputStream.use { it.write(body.toByteArray()) }
        }
        val code = connection.responseCode
        val text = (if (code < 400) connection.inputStream else connection.errorStream).use { it.readBytes().decodeToString() }
        return code to text
    }

    @Test
    fun serverChecksThePinAndRelaysTaps() = runBlocking {
        val taps = mutableListOf<WebStep>()
        val server = CounterWebServer(
            port = 0,
            pin = { "123456" },
            page = { "<html>".toByteArray() },
            state = { """{"project":null}""" },
            step = { taps += it },
        )
        val job = launch { server.run() }
        try {
            val port = withTimeout(5_000) { server.boundPort.await() }
            assertEquals(200 to "<html>", request(port, "/"))
            assertEquals(401, request(port, "/api/state").first)
            assertEquals(401, request(port, "/api/state", pin = "000000").first)
            assertEquals(200 to """{"project":null}""", request(port, "/api/state", pin = "123456"))
            assertEquals(400, request(port, "/api/step", "POST", "123456", "step=1&delta=7").first)
            assertEquals(200, request(port, "/api/step", "POST", "123456", "step=3&delta=-1").first)
            assertEquals(405, request(port, "/api/step", "GET", "123456").first)
            assertEquals(404, request(port, "/nope").first)
            assertEquals(listOf(WebStep(3, -1)), taps)
        } finally {
            job.cancel()
        }
    }
}
