import akka.actor.testkit.typed.javadsl.ActorTestKit
import akka.stream.javadsl.Source
import akka.stream.typed.javadsl.ActorFlow
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Test
import java.time.Duration

class ActorSpec {
    companion object {
        val testKit = ActorTestKit.create()

        @AfterAll
        @JvmStatic
        fun afterAll() {
            testKit.shutdownTestKit()
        }
    }

    @Test
    fun `connecting to an actor from a stream should send each element, wait and get back the answer from the actor`() {
        val ref = testKit.spawn(Cap.behaviour(0, 3))
        val askFlow = ActorFlow.ask(ref, Duration.ofSeconds(10)) { elem: Int, replyTo ->
            Cap.Increment(elem, replyTo)
        }

        Source.range(1, 10).via(askFlow).map { println(it) }.run(testKit.system()).toCompletableFuture()
            .get(1, java.util.concurrent.TimeUnit.SECONDS)

    }

    @Test
    fun `connecting to an actor async should send an element from the stream and get back an answer from the actor`() {
        val ref = testKit.spawn(Cap.behaviour(0, 1000))
        val askFlow = ActorFlow.ask(100, ref, Duration.ofSeconds(10)) { elem: Int, replyTo ->
            Cap.Increment(elem, replyTo)
        }

        Source.range(1, 1000000)
            .via(askFlow).run(testKit.system()).toCompletableFuture().get(10, java.util.concurrent.TimeUnit.SECONDS)
    }
}