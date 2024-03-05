package chapter04

import akka.actor.testkit.typed.Effect
import akka.actor.testkit.typed.javadsl.ActorTestKit
import akka.actor.testkit.typed.javadsl.BehaviorTestKit
import akka.actor.testkit.typed.javadsl.LoggingTestKit
import akka.actor.testkit.typed.javadsl.TestInbox
import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.javadsl.AbstractBehavior
import akka.actor.typed.javadsl.ActorContext
import akka.actor.typed.javadsl.Behaviors
import akka.actor.typed.javadsl.Receive
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.slf4j.event.Level
import scala.concurrent.duration.FiniteDuration
import java.time.Duration
import java.util.concurrent.TimeUnit


class SimplifiedWorker(context: ActorContext<String>) : AbstractBehavior<String>(context) {

    companion object {
        fun create(): Behavior<String> {
            return Behaviors.setup { context -> SimplifiedWorker(context) }
        }
    }

    override fun createReceive(): Receive<String> = newReceiveBuilder().onAnyMessage { message ->
        context.log.info("Received message: $message")
        Behaviors.ignore()
    }.build()
}

class SimplifiedManager(context: ActorContext<Command>?) : AbstractBehavior<SimplifiedManager.Command>(context) {

    companion object {
        fun create(): Behavior<Command> {
            return Behaviors.setup { context -> SimplifiedManager(context) }
        }
    }

    sealed interface Command {
        data class CreateChild(val name: String) : Command
        data class Forward(val message: String, val sendTo: ActorRef<String>) : Command
        object ScheduleLog : Command
        object Log : Command
    }

    override fun createReceive(): Receive<Command> {
        return newReceiveBuilder()
            .onMessage(Command.CreateChild::class.java) { command ->
                context.spawn(SimplifiedWorker.create(), command.name)
                Behaviors.same()
            }
            .onMessage(Command.Forward::class.java) { command ->
                command.sendTo.tell(command.message)
                Behaviors.same()
            }
            .onMessage(Command.ScheduleLog::class.java) {
                context.scheduleOnce(Duration.ofSeconds(1), context.self, Command.Log)
                Behaviors.same()
            }
            .onMessage(Command.Log::class.java) {
                context.log.info("Logging")
                Behaviors.same()
            }
            .build()
    }


}


class SyncTestingSpec {


    @Test
    fun testCreateChild() {
        val testKit = BehaviorTestKit.create(SimplifiedManager.create())
        testKit.run(SimplifiedManager.Command.CreateChild("adan"))
        Assertions.assertEquals("adan", testKit.expectEffectClass(Effect.Spawned::class.java).childName())
    }

    @Test
    fun testForward() {
        val testKit = BehaviorTestKit.create(SimplifiedManager.create())
        val probe = TestInbox.create<String>()
        testKit.run(SimplifiedManager.Command.Forward("hello", probe.ref))
        probe.expectMessage("hello")
        Assertions.assertFalse(probe.hasMessages())
    }

    @Test
    fun testLog() {
        val testKit = BehaviorTestKit.create(SimplifiedManager.create())
        testKit.run(SimplifiedManager.Command.Log)
        testKit.allLogEntries.forEach {
            Assertions.assertEquals("Logging", it.message())
        }
    }

    @Test
    fun testScheduleLog() {
        val testKit = BehaviorTestKit.create(SimplifiedManager.create())
        testKit.run(SimplifiedManager.Command.ScheduleLog)
        testKit.expectEffect(
            Effect.Scheduled(FiniteDuration(1000, TimeUnit.MILLISECONDS), testKit.ref, SimplifiedManager.Command.Log)
        )
        testKit.allLogEntries.forEach {
            Assertions.assertEquals("Logging", it.message())
        }
    }
}

class AsyncTestingSpec {
    companion object {
        val testKit = ActorTestKit.create()

        @AfterAll
        @JvmStatic
        fun afterAll() {
            testKit.shutdownTestKit()
        }
    }

    @Test
    fun testForward() {
        val probe = testKit.createTestProbe<String>()
        val manager = testKit.spawn(SimplifiedManager.create())
        manager.tell(SimplifiedManager.Command.Forward("hello", probe.ref))
        probe.expectMessage("hello")
    }

    @Test
    fun testMonitor() {
        val probe = testKit.createTestProbe<String>()
        val behaviorUnderTest = Behaviors.receiveMessage<String> { _ ->
            Behaviors.ignore()
        }
        val behaviorMonitored = Behaviors.monitor(String::class.java, probe.ref, behaviorUnderTest)

        val actor = testKit.spawn(behaviorMonitored)
        actor.tell("hello")
        probe.expectMessage("hello")
    }

    @Test
    fun testLog() {
        val manager = testKit.spawn(SimplifiedManager.create())
        LoggingTestKit.info("Logging").expect(testKit.system()) {
            manager.tell(SimplifiedManager.Command.Log)
        }
    }

    @Test
    fun testDeadLetterLog() {
        val behavior: Behavior<String> = Behaviors.stopped()
        val carl = testKit.spawn(behavior, "carl")
        LoggingTestKit.empty()
            .withLogLevel(Level.INFO)
            .withMessageRegex(
                ".*Message.*to.*carl.*was not delivered.*2.*dead letters encountered")
            .expect(testKit.system()) {
                carl.tell( "Hello")
                carl.tell( "Hello")
            }
    }
}

