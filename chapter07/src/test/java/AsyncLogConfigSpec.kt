import akka.actor.testkit.typed.javadsl.ActorTestKit
import akka.actor.testkit.typed.javadsl.LoggingTestKit
import akka.actor.typed.Behavior
import akka.actor.typed.javadsl.Behaviors
import com.typesafe.config.ConfigFactory
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Test

class AsyncLogConfigSpec {

    companion object {
        val config = ConfigFactory
            .parseString(
                """akka.eventsourced-entity.journal-enabled  = false"""
            )
            .withFallback(ConfigFactory.load("in-memory"))
        val testKit = ActorTestKit.create(config)

        @AfterAll
        @JvmStatic
        fun afterAll() {
            testKit.shutdownTestKit()
        }
    }

    @Test
    fun `Actor must log in debug the content when receiving message`() {
        val loggerBehavior: Behavior<String> = Behaviors.receive { context, message ->
            when (message) {
                is String -> {
                    context.log.debug("message '$message', received")
                    Behaviors.same()
                }

                else -> Behaviors.unhandled()
            }
        }

        val loggerActor = testKit.spawn(loggerBehavior)
        val message = "hi"

        LoggingTestKit.debug("message '$message', received").expect(testKit.system()) {
            loggerActor.tell(message)
        }
    }

    @Test
    fun `lift one property from conf`() {
        val inmemory = testKit.system().settings().config()
        val journalenabled =
            inmemory.getString("akka.eventsourced-entity.journal-enabled")
        val readjournal =
            inmemory.getString("akka.eventsourced-entity.read-journal")

        val loggerBehavior: Behavior<String> = Behaviors.receive { context, message ->
            when (message) {
                is String -> {
                    context.log.info("$journalenabled $readjournal")
                    Behaviors.same()
                }

                else -> Behaviors.unhandled()
            }
        }

        val loggerActor = testKit.spawn(loggerBehavior)
        val message = "anymessage"

        LoggingTestKit.info("false inmem-read-journal").expect(testKit.system()) {
            loggerActor.tell(message)
        }
    }
}