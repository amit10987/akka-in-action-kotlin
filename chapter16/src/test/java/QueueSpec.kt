import akka.actor.testkit.typed.javadsl.ActorTestKit
import akka.stream.OverflowStrategy
import akka.stream.QueueOfferResult
import akka.stream.javadsl.Keep
import akka.stream.javadsl.Sink
import akka.stream.javadsl.Source
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Test

class QueueSpec {

    companion object {
        val testKit = ActorTestKit.create()

        @AfterAll
        @JvmStatic
        fun afterAll() {
            testKit.shutdownTestKit()
        }
    }

    @Test
    fun `Source queue should allow adding elements to a stream`() {
        val bufferSize = 10
        val queue = Source.queue<Int>(bufferSize)
            .map { x -> 2 * x }
            .toMat(Sink.foreach { x -> println("processed $x") }, Keep.left())
            .run(testKit.system())
        (1..10).map { i -> queue.offer(i) }

        Thread.sleep(1000)
    }

    @Test
    fun `Source queue should allow to configure its overflow strategy`() {
        val bufferSize = 4

        val queue = Source.queue<Int>(bufferSize, OverflowStrategy.dropHead())
            .throttle(1, java.time.Duration.ofMillis(100))
            .map { x -> 2 * x }
            .toMat(Sink.foreach { x -> println("processed $x") }, Keep.left())
            .run(testKit.system())

        (1..10).map { i -> queue.offer(i) }

        Thread.sleep(1000)
    }

    @Test
    fun `Source queue should allow to configure its overflow strategy and print QueueOfferResult`() {
        val bufferSize = 4

        val queue = Source.queue<Int>(bufferSize, OverflowStrategy.dropNew())
            .throttle(1, java.time.Duration.ofMillis(100))
            .map { x -> 2 * x }
            .toMat(Sink.foreach { x -> println("processed $x") }, Keep.left())
            .run(testKit.system())

        (1..10).map { i ->
            queue.offer(i).handle { t, u ->
                when(t) {
                    QueueOfferResult.enqueued() -> println("enqueued $i")
                    QueueOfferResult.dropped() -> println("dropped $i")
                    else -> { println("error $u")}
                }
            }
        }

        Thread.sleep(1000)
    }
}