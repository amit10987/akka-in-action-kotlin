import akka.actor.testkit.typed.javadsl.ActorTestKit
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Test
import routers.Switch

class StateBasedRouterSpec {
    companion object {
        val testKit = ActorTestKit.create()

        @AfterAll
        @JvmStatic
        fun afterAll() {
            testKit.shutdownTestKit()
        }
    }

    @Test
    fun `A State Based Router should route to forward to actor reference when on`(){
        val forwardToProbe = testKit.createTestProbe<String>()
        val alertToProbe = testKit.createTestProbe<String>()
        val switch = testKit.spawn(Switch.create(forwardToProbe.ref, alertToProbe.ref), "switch")
        switch.tell(Switch.Payload("content1", "metadata1"))
        forwardToProbe.expectMessage("content1")

    }
}