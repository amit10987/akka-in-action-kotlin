import akka.actor.testkit.typed.javadsl.ActorTestKit
import akka.actor.typed.javadsl.Behaviors
import akka.actor.typed.receptionist.Receptionist
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import routers.Aggregator
import routers.Camera
import routers.DataEnricher
import routers.DataObfuscator
import routers.PhotoProcessor
import java.time.Duration

class GroupRoutingSpec {

    companion object {
        val testKit = ActorTestKit.create()

        @AfterAll
        @JvmStatic
        fun afterAll() {
            testKit.shutdownTestKit()
        }
    }

    @Test
    fun `a group router should send messages to one worker registered at a key`() {
        val probe1 = testKit.createTestProbe<String>()
        val behavior1 = Behaviors.monitor(String::class.java, probe1.ref, Behaviors.empty())
        testKit.system().receptionist().tell(Receptionist.register(PhotoProcessor.key, testKit.spawn(behavior1)))
        val groupRouter = testKit.spawn(Camera.create())
        groupRouter.tell(Camera.Photo("hi"))
        probe1.expectMessage("hi")
        probe1.receiveSeveralMessages(0)
    }


    @Test
    fun `send messages to all photo processors registered With no guarantee of fair distribution`() {
        val photoProcessor1 = testKit.createTestProbe<String>()
        val pp1Monitor = Behaviors.monitor(String::class.java, photoProcessor1.ref, PhotoProcessor.create())

        val photoProcessor2 = testKit.createTestProbe<String>()
        val pp2Monitor = Behaviors.monitor(String::class.java, photoProcessor2.ref, PhotoProcessor.create())

        testKit.system().receptionist().tell(
            Receptionist.register(PhotoProcessor.key, testKit.spawn(pp1Monitor))
        )
        testKit.system().receptionist().tell(
            Receptionist.register(PhotoProcessor.key, testKit.spawn(pp2Monitor))
        )

        val camera = testKit.spawn(Camera.create())
        camera.tell(Camera.Photo("A"))
        camera.tell(Camera.Photo("B"))

        photoProcessor1.expectMessage("A")
        photoProcessor2.expectMessage("B")

    }

    @Test
    fun `will send messages with same id to the same aggregator`() {
        val probe1 = testKit.createTestProbe<Aggregator.Event>()
        val probe2 = testKit.createTestProbe<Aggregator.Event>()

        testKit.spawn(Aggregator.create(emptyMap(), probe1.ref), "aggregator1")
        testKit.spawn(Aggregator.create(emptyMap(), probe2.ref), "aggregator2")

        val contentValidator = testKit.spawn(DataObfuscator.create(), "wa-1")
        val dataEnricher = testKit.spawn(DataEnricher.create(), "wb-1")

        // When a message with the same id is sent to different actors
        contentValidator.tell(DataObfuscator.Message("123", "Text"))
        dataEnricher.tell(DataEnricher.Message("123", "Text"))
        contentValidator.tell(DataObfuscator.Message("123", "Text2"))
        dataEnricher.tell(DataEnricher.Message("123", "Text2"))

        // Then one aggregator receives both while the other receives none
        Assertions.assertEquals(probe1.receiveMessage().id, probe1.receiveMessage().id)

    }
}