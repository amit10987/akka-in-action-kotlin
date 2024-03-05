import akka.actor.testkit.typed.javadsl.ActorTestKit
import akka.actor.testkit.typed.javadsl.LoggingTestKit
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Test

class SupervisionExampleSpec {
    companion object {
        val testKit = ActorTestKit.create()

        @AfterAll
        @JvmStatic
        fun afterAll() {
            testKit.shutdownTestKit()
        }
    }

    @Test
    fun testPostStopException() {
        val behavior = testKit.spawn(SupervisionExample.create())
        LoggingTestKit.info("recoverable").expect(testKit.system()) {
            behavior.tell("recoverable")
        }
    }

    @Test
    fun testStopAndReceivePostStopSignal() {
        val behavior = testKit.spawn(SupervisionExample.create())
        LoggingTestKit.info("cleaning up resources").expect(testKit.system()) {
            LoggingTestKit.info("stopping").expect(testKit.system()) {
                behavior.tell("stop")
            }
        }
    }

    @Test
    fun testGrantAndLog(){
        val behavior = testKit.spawn(SupervisionExample.create())
        LoggingTestKit.info("granted").expect(testKit.system()) {
            behavior.tell("secret")
        }
    }

    @Test
    fun testStopActorSystem(){
        val behavior = testKit.spawn(SupervisionExample.create())
        behavior.tell("fatal")
    }
}