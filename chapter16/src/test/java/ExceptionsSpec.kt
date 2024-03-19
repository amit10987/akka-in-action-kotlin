import akka.actor.testkit.typed.javadsl.ActorTestKit
import akka.stream.ActorAttributes
import akka.stream.Supervision
import akka.stream.javadsl.Flow
import akka.stream.javadsl.Sink
import akka.stream.javadsl.Source
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Test

class ExceptionsSpec {
    companion object {
        val testKit = ActorTestKit.create()

        @AfterAll
        @JvmStatic
        fun afterAll() {
            testKit.shutdownTestKit()
        }
    }

    @Test
    fun `an Exception should stop the stream and log 'tried riskyHandle'`() {
        fun riskyHandler(elem: Int): Int = 100 / elem
        Source.range(-1, 1)
            .map { riskyHandler(it) }
            .log("tried riskyHandler")
            .map { println(it) }
            .run(testKit.system()).toCompletableFuture().get(1, java.util.concurrent.TimeUnit.SECONDS)

    }

    @Test
    fun `an Exception should be possible to overcome by a last message`() {
        fun riskyHandler(elem: Int): Int = 100 / elem
        Source.range(-1, 1)
            .map { riskyHandler(it) }
            .log("tried riskyHandler")
            .recover(ArithmeticException::class.java) { 0 }
            .map { println(it) }
            .run(testKit.system()).toCompletableFuture().get(3, java.util.concurrent.TimeUnit.SECONDS)

    }

    @Test
    fun `an Exception should be possible to overcome ArithmeticException`() {
        fun riskyHandler(elem: Int): Int {
            return if (elem > 2)
                throw IllegalArgumentException(
                    "no higher than 2 please"
                ) else
                100 / elem
        }

        val decider = Supervision.resumingDecider().apply {
            when (this) {
                is ArithmeticException -> Supervision.resume()
                else -> Supervision.stop()
            }
        }

        Source.range(-1, 3)
            .map { riskyHandler(it) }
            .log("riskyHandler")
            .map { println(it) }
            .withAttributes(
                ActorAttributes.supervisionStrategy(decider)
            )
            .run(testKit.system()).toCompletableFuture().get(1, java.util.concurrent.TimeUnit.SECONDS)

    }

    @Test
    fun `an Exception should be possible to overcome by continuing and restarting`() {
        fun riskyHandler(elem: Int): Int = 100 / elem

        val decider = Supervision.resumingDecider().apply {
            when (this) {
                is ArithmeticException -> Supervision.resume()
                else -> Supervision.stop()
            }
        }
        var state = 0

        val flow = Flow.create<Int>()
            .map { each -> state += 1; each }
            .map { riskyHandler(it) }
            .map { println(it) }

        Source.range(-1, 1)
            .via(flow)
            .withAttributes(
                ActorAttributes.supervisionStrategy(decider)
            )
            .run(testKit.system()).toCompletableFuture().get(1, java.util.concurrent.TimeUnit.SECONDS)

    }

    @Test
    fun `The invalid results should be diverted to another Sink as soon as possible`() {
        data class Result(
            val value: Int,
            val isValid: Boolean,
            val message: String = "",
        )

        fun properHandler(elem: Int): Result =
            try {
                Result(100 / elem, true)
            } catch (ex: ArithmeticException) {
                Result(
                    elem,
                    false,
                    "failed processing [$elem] with $ex."
                )
            }

        val sink1 = Sink.foreach { each: Result ->
            println("${each.value} has been diverted. Caused by: ${each.message}")
        }

        val sink2 = Sink.foreach { each: Result ->
            println("${each.value} has been successfully processed")
        }

        Source.range(-1, 1)
            .map{properHandler(it)}
            .divertTo(sink1) { !it.isValid }
            //some more processing
            .to(sink2)
            .run(testKit.system())
    }

}