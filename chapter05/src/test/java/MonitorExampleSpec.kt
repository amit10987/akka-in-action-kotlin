import akka.actor.testkit.typed.javadsl.ActorTestKit
import akka.actor.testkit.typed.javadsl.LoggingTestKit
import akka.actor.testkit.typed.javadsl.TestProbe
import akka.actor.typed.ActorSystem
import akka.actor.typed.SupervisorStrategy
import akka.actor.typed.javadsl.Behaviors
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

class MonitorExampleSpec {

    companion object {
        val testKit = ActorTestKit.create()

        @AfterAll
        @JvmStatic
        fun afterAll() {
            testKit.shutdownTestKit()
        }
    }

    @Test
    fun testStoppedSignal() {
        // #monitor-example
        val watcher = testKit.spawn(SimplifiedFileWatcher.create())
        val logprocessor =
            testKit.spawn(Behaviors.receiveMessage { message: String ->
                when (message) {
                    "stop" -> Behaviors.stopped()
                    else -> Behaviors.same()
                }
            })

        watcher.tell(SimplifiedFileWatcher.Command.Watch(logprocessor))

        LoggingTestKit.info("terminated").expect(testKit.system()) {
            logprocessor.tell("stop")
        }
    }

    @Test
    fun testChildFailed(){
        val probe = testKit.createTestProbe<String>()
        val watcher = testKit.spawn(ParentWatcher.create(probe.ref))
        watcher.tell(ParentWatcher.Spawn(ParentWatcher.childBehaviour()))
        watcher.tell(ParentWatcher.FailChildren)
        probe.expectMessage("childFailed")
    }

    @Test
    fun testChildStopped(){
        val probe = testKit.createTestProbe<String>()
        val watcher = testKit.spawn(ParentWatcher.create(probe.ref))
        watcher.tell(ParentWatcher.Spawn(ParentWatcher.childBehaviour()))
        watcher.tell(ParentWatcher.StopChildren)
        probe.expectMessage("terminated")
    }

    @Test
    fun `is not being notified if the watched child throws an Non-Fatal Exception while having a restart strategy`(){
        val restartingChildBehavior = Behaviors
            .supervise(ParentWatcher.childBehaviour())
            .onFailure(SupervisorStrategy.restart())

        val probe = testKit.createTestProbe<String>()
        val watcher = testKit.spawn(ParentWatcher.create(probe.ref))
        watcher.tell(ParentWatcher.Spawn(restartingChildBehavior))
        watcher.tell(ParentWatcher.FailChildren)
        probe.expectNoMessage()
    }


}