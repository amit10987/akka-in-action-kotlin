import akka.actor.typed.ActorSystem
import akka.actor.typed.javadsl.Behaviors
import akka.stream.Materializer
import akka.stream.javadsl.Flow
import akka.stream.javadsl.Keep
import akka.stream.javadsl.Sink
import akka.stream.javadsl.Source
import org.junit.jupiter.api.Test
import scala.concurrent.Await
import java.time.Duration
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

class StreamSpec {

    @Test
    fun `a producer of fixed elements 1,2,3 and a function should allow a consumer to receive only even numbers`() {
        val system = ActorSystem.create(Behaviors.empty<Void>(), "runner")

        val producer = Source.from(listOf(1, 2, 3))
        val processor = Flow.create<Int>().filter { it % 2 == 0 }
        val consumer = Sink.seq<Int>()

        val future = producer.via(processor).toMat(consumer, Keep.right()).run(system)

        val sequenceFuture = CompletableFuture<List<Int>>()

        future.thenAccept {
            sequenceFuture.complete(it)
            system.terminate()
        }

        val sequence = sequenceFuture.get(3, TimeUnit.SECONDS)
        assert(sequence == listOf(2))
    }

    @Test
    fun `a producer of fixed elements 1,2,3 and a function should allow when consumed see the side effects | simple test version`() {
        val system = ActorSystem.create(Behaviors.empty<Void>(), "runner")
        var fakeDB = listOf<Int>()
        fun storeDB(value: Int) {
            fakeDB = fakeDB + value
        }

        val producer = Source.from(listOf(1, 2, 3))
        val processor = Flow.create<Int>().filter { it % 2 == 0 }
        producer.via(processor).runForeach({
            storeDB(it)
            system.terminate()
        }, system).thenRun {
            assert(fakeDB == listOf(2))
        }
    }

    @Test
    fun `an infinite producer with a consumer creating side effect should be cancellable`() {
        val system = ActorSystem.create(Behaviors.empty<Void>(), "runner")
        val liveSource = Source.tick(Duration.ofSeconds(1), Duration.ofSeconds(1), "Hello, World")
        val masking = Flow.create<String>().map { it.replace("World", "xyz") }
        fun dbFakeInsert(value: String): Unit = println("inserting $value")
        val dbFakeSink = Sink.foreach<String> { dbFakeInsert(it) }
        val future = liveSource.via(masking).toMat(dbFakeSink, Keep.both()).run(system)
        Thread.sleep(3000)
        future.first().cancel()
        Thread.sleep(3000)
        assert(future.second().toCompletableFuture().isDone)
    }

}