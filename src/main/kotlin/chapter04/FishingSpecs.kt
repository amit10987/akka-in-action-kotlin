package chapter04

import akka.actor.testkit.typed.javadsl.ActorTestKit
import akka.actor.testkit.typed.javadsl.FishingOutcomes
import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.javadsl.AbstractBehavior
import akka.actor.typed.javadsl.ActorContext
import akka.actor.typed.javadsl.Behaviors
import akka.actor.typed.javadsl.Receive
import akka.actor.typed.javadsl.TimerScheduler
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Test
import java.time.Duration
import kotlin.random.Random

class Receiver(context: ActorContext<Command>) : AbstractBehavior<Receiver.Command>(context) {

    companion object {
        fun create(): Behavior<Command> {
            return Behaviors.ignore()
        }
    }

    sealed interface Command {
        object Tock : Command
        object Cancelled : Command
    }

    override fun createReceive(): Receive<Command> {
        return newReceiveBuilder().build()
    }
}

class Sender(
    private val forwardTo: ActorRef<Receiver.Command>,
    private val timer: TimerScheduler<Command>,
    context: ActorContext<Command>
) : AbstractBehavior<Sender.Command>(context) {

    companion object {
        fun create(forwardTo: ActorRef<Receiver.Command>, timer: TimerScheduler<Command>): Behavior<Command> {
            return Behaviors.setup { context -> Sender(forwardTo, timer, context) }
        }
    }

    sealed interface Command {
        object Tick : Command
        data class Cancel(val key: String) : Command
    }

    override fun createReceive(): Receive<Command> {
        return newReceiveBuilder()
            .onMessage(Command.Tick::class.java) {
                context.log.info("Sending Tock to receiver")
                forwardTo.tell(Receiver.Command.Tock)
                Behaviors.same()
            }
            .onMessage(Command.Cancel::class.java) { command ->
                context.log.info("Cancelling key: ${command.key}")
                forwardTo.tell(Receiver.Command.Cancelled)
                timer.cancel(command.key)
                Behaviors.same()
            }
            .build()
    }
}

class FishingSpecs {

    companion object {
        val testKit = ActorTestKit.create()

        @AfterAll
        @JvmStatic
        fun afterAll() {
            testKit.shutdownTestKit()
        }
    }

    @Test
    fun testCancelTimer() {
        val probe = testKit.createTestProbe<Receiver.Command>()
        val timerKey = "key1234"

        val sender = Behaviors.withTimers { timer ->
            timer.startTimerAtFixedRate(timerKey, Sender.Command.Tick, Duration.ofMillis(100))
            Sender.create(probe.ref, timer)
        }

        val ref = testKit.spawn(sender)

        probe.expectMessage(Receiver.Command.Tock)
        probe.fishForMessage(Duration.ofSeconds(3)) { message ->
           when (message) {
               is Receiver.Command.Tock -> {
                   if(Random.nextInt(4)  == 0){
                       ref.tell(Sender.Command.Cancel(timerKey))
                   }
                   FishingOutcomes.continueAndIgnore()
               }
               is Receiver.Command.Cancelled -> FishingOutcomes.complete()
               else -> FishingOutcomes.fail("Unexpected message")
           }
        }

        probe.expectNoMessage(Duration.ofMillis(100) + Duration.ofMillis(100))
    }

    @Test
    fun testMonitor() {
        val probe = testKit.createTestProbe<String>()
        val behavior = Behaviors.receiveMessage<String> { _ ->
            Behaviors.ignore()
        }
        val behaviorMonitored = Behaviors.monitor(String::class.java, probe.ref, behavior)
        val actor = testKit.spawn(behaviorMonitored)
        actor.tell("checking")
        probe.expectMessage("checking")
    }
}