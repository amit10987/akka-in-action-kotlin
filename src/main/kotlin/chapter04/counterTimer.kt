package chapter04

import akka.actor.testkit.typed.javadsl.ActorTestKit
import akka.actor.testkit.typed.javadsl.FishingOutcomes
import akka.actor.typed.Behavior
import akka.actor.typed.javadsl.AbstractBehavior
import akka.actor.typed.javadsl.ActorContext
import akka.actor.typed.javadsl.Behaviors
import akka.actor.typed.javadsl.Receive
import akka.actor.typed.javadsl.TimerScheduler
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Test
import java.time.Duration

class CounterTimer(private val timers: TimerScheduler<Command>, context: ActorContext<Command>) :
    AbstractBehavior<CounterTimer.Command>(context) {

    companion object {
        fun create(): Behavior<Command> {
            return Behaviors.setup { context ->
                Behaviors.withTimers { timers ->
                    CounterTimer(timers, context)
                }
            }
        }
    }

    sealed interface Command {
        object Increase : Command
        data class Pause(val seconds: Int) : Command
        object Resume : Command
    }

    override fun createReceive(): Receive<Command> = resume(0)

    private fun resume(count: Int): Receive<Command> {
        return newReceiveBuilder()
            .onMessage(Command.Increase::class.java) {
                val current = count + 1
                context.log.info("increasing to $current")
                resume(current)
            }
            .onMessage(Command.Pause::class.java) {
                timers.startSingleTimer(Command.Resume, Duration.ofSeconds(it.seconds.toLong()))
                pause(count)
            }
            .build()
    }

    private fun pause(count: Int): Receive<Command> {
        return newReceiveBuilder()
            .onMessage(Command.Increase::class.java) {
                context.log.info("counter is paused. Can't increase")
                Behaviors.same()
            }
            .onMessage(Command.Pause::class.java) {
                context.log.info("counter is paused. Can't pause again")
                Behaviors.same()
            }
            .onMessage(Command.Resume::class.java) {
                context.log.info("resuming")
                resume(count)
            }.build()
    }
}

class TestCounter {

    companion object {
        val testKit = ActorTestKit.create()

        @AfterAll
        @JvmStatic
        fun afterAll() {
            testKit.shutdownTestKit()
        }
    }

    @Test
    fun testResumingCounter() {
        val probe = testKit.createTestProbe<CounterTimer.Command>()
        val counterMonitored = Behaviors.monitor(CounterTimer.Command::class.java, probe.ref, CounterTimer.create())
        val counter = testKit.spawn(counterMonitored)

        counter.tell(CounterTimer.Command.Pause(1))

        probe.fishForMessage(Duration.ofSeconds(3)) {
            when (it) {
                is CounterTimer.Command.Increase -> FishingOutcomes.continueAndIgnore()
                is CounterTimer.Command.Pause -> FishingOutcomes.continueAndIgnore()
                is CounterTimer.Command.Resume -> FishingOutcomes.complete()
            }
        }
    }
}