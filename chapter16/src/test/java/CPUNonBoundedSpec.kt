import akka.actor.testkit.typed.javadsl.ActorTestKit
import akka.http.javadsl.Http
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.javadsl.Source
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Test
import scalikejdbc.config.DBs
import java.util.concurrent.CompletableFuture

class CPUNonBoundedSpec {

    companion object {
        val testKit = ActorTestKit.create()

        @AfterAll
        @JvmStatic
        fun afterAll() {
            testKit.shutdownTestKit()
        }
    }

    @Test
    fun `calling to an external service should be much faster`() {
        val init = System.currentTimeMillis()

        val result =
            Source.range(1, 5000)
                .mapAsync(1000) { each -> CompletableFuture.supplyAsync { externalFilter(each) } }
                .map { each -> print1000th(each, init) }
                .run(testKit.system())

        result.toCompletableFuture().get()
    }

    @Test
    fun `calling to a real service should be handle with throttle and mapAsync`() {
        Source.range(1, 100)
            .throttle(32, java.time.Duration.ofSeconds(1))
            .mapAsync(100) {
                Http.get(testKit.system()).singleRequest(
                    akka.http.javadsl.model.HttpRequest.create("http://localhost:8080/validate?quantity=$it")
                )
            }
            .map{ println(it.toString()) }
            .run(testKit.system()).toCompletableFuture().get(5, java.util.concurrent.TimeUnit.SECONDS)
    }

    private fun externalFilter(number: Int): Int {
        return if (number % 11 == 0) 1 else number
    }

    private fun print1000th(element: Int, init: Long) {
        if (element % 1000 == 0) {
            val elapsedTime = (System.currentTimeMillis() - init) / 1000
            println("$element-th element processed. Total time elapsed: $elapsedTime s")
        }
    }

}