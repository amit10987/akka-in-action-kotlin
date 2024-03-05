import akka.actor.testkit.typed.javadsl.ActorTestKit
import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.SupervisorStrategy
import akka.actor.typed.javadsl.Behaviors
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Test

class CheckingMessagesSpec {

    companion object {
        val testKit = ActorTestKit.create()

        @AfterAll
        @JvmStatic
        fun afterAll() {
            testKit.shutdownTestKit()
        }
    }

    @Test
    fun `an actor that restart should not process the message`() {

        fun beh(monitor: ActorRef<Int>): Behavior<Int> {
            val rec: Behavior<Int> = Behaviors.receive { _, msg ->
                when (msg) {
                    2 -> {
                        monitor.tell(2)
                        throw IllegalStateException()
                    }

                    else -> Behaviors.same()
                }
            }
            return Behaviors.supervise(rec).onFailure(SupervisorStrategy.restart())
        }

        val probe = testKit.createTestProbe<Int>()
        val actor = testKit.spawn(beh(probe.ref))

        for (i in 1..10) {
            actor.tell(i)
        }

        probe.expectMessage(2)
        probe.expectNoMessage()
    }
}