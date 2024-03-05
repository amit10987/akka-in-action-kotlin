package countwords

import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.javadsl.AbstractBehavior
import akka.actor.typed.javadsl.ActorContext
import akka.actor.typed.javadsl.Behaviors
import akka.actor.typed.javadsl.Receive
import akka.util.Timeout
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import scala.concurrent.duration.FiniteDuration
import java.time.Duration
import java.util.concurrent.TimeUnit


class Master(context: ActorContext<Event>, private val workerRouter: ActorRef<Worker.Command>) :
    AbstractBehavior<Master.Event>(context) {

    sealed interface Event {
        object Tick : Event
        data class CountedWords @JsonCreator constructor(@JsonProperty("aggregation") val aggregation: Map<String, Int>) : CborSerializable, Event
        data class FailedJob(val text: String) : Event
    }

    companion object {
        fun create(workerRouter: ActorRef<Worker.Command>): Behavior<Event> = Behaviors.setup { context ->
            Behaviors.withTimers { timers ->
                timers.startTimerWithFixedDelay(Event.Tick, Event.Tick, Duration.ofSeconds(1))
                Master(context, workerRouter)
            }
        }
    }

    override fun createReceive(): Receive<Event> = doReceive(workerRouter)

    private fun doReceive(
        workersRouter: ActorRef<Worker.Command>,
        countedWords: Map<String, Int> = emptyMap(),
        lag: List<String> = emptyList()
    ): Receive<Event> {
        val paralellism = context.system.settings().config().getInt("example.countwords.delegation-parallelism")

        return newReceiveBuilder().onMessage(Event.Tick::class.java) {
            context.log.debug("tick, current lag ${lag.size} ")
            val text = "this simulates a stream, a very simple stream"
            val allTexts = lag + text

           allTexts.map { fp ->
                context.ask(
                    Event::class.java,
                    workersRouter,
                    Duration.ofSeconds(3),
                    { x -> Worker.Command.Process(fp, x) }, { l, _ ->
                        l as? Event.CountedWords ?: Event.FailedJob(fp)
                    })
            }

            doReceive(workersRouter, countedWords, emptyList())
        }.onMessage(Event.CountedWords::class.java) {
            val merged = merge(countedWords, it.aggregation)
            context.log.debug("current count ${merged} ")
            doReceive(workersRouter, merged, lag)
        }.onMessage(Event.FailedJob::class.java) {
            context.log.debug(
                "failed, adding text to lag ${lag.size} "
            )
            doReceive(workersRouter, countedWords, lag + it.text)
        }.build()
    }

    fun merge(currentCount: Map<String, Int>, newCount2Add: Map<String, Int>): Map<String, Int> =
        (currentCount.asSequence() + newCount2Add.asSequence())
            .groupBy({ it.key }, { it.value })
            .mapValues { it.value.sum() }

}