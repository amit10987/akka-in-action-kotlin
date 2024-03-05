import akka.NotUsed
import akka.actor.typed.ActorSystem
import akka.actor.typed.javadsl.Behaviors
import akka.stream.javadsl.Flow
import akka.stream.javadsl.Keep
import akka.stream.javadsl.Sink
import akka.stream.javadsl.Source

fun main() {
    val system = ActorSystem.create(Behaviors.empty<Void>(), "runner")
    val fakeDB = mutableListOf<Int>()
    val storeDB: (Int) -> Unit = { value -> fakeDB.add(value) }

    val producer: Source<Int, NotUsed> = Source.from(listOf(1, 2, 3))
    val processor: Flow<Int, Int, NotUsed> = Flow.create<Int>().filter { it % 2 == 0 }
    val consumer = Sink.foreach(storeDB)

    val blueprint = producer.via(processor).toMat(consumer, Keep.right()).run(system)

    blueprint.whenComplete { success, failure ->
        println(fakeDB)
        system.terminate()
    }

}