import akka.actor.testkit.typed.javadsl.ActorTestKit
import akka.actor.testkit.typed.javadsl.LoggingTestKit
import akka.actor.typed.ActorRef
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Test

class ReceptionistUsageSpec {

    companion object {
        val testKit = ActorTestKit.create()

        @AfterAll
        @JvmStatic
        fun afterAll() {
            testKit.shutdownTestKit()
        }
    }

    @Test
    fun `An actor subscribed to a ServiceKey should get notified about all the actors each time an actor registers`(){
        val guest = testKit.spawn(VIPGuest.create(), "Mr.Wick")
        testKit.spawn(HotelConcierge.create())
        LoggingTestKit.info("Mr.Wick is in").expect(testKit.system()) {
            guest.tell(VIPGuest.Command.EnterHotel)
        }

        val guest2 = testKit.spawn(VIPGuest.create(), "Mr.Ious")
        LoggingTestKit.info("Mr.Wick is in").expect(testKit.system()) {
            LoggingTestKit.info("Mr.Ious is in").expect(testKit.system()) {
                guest2.tell(VIPGuest.Command.EnterHotel)
            }
        }
    }

    fun <T> getActorRefClass(ref: ActorRef<T>): Class<out ActorRef<T>> {
        @Suppress("UNCHECKED_CAST")
        return ref::class.java
    }
    @Test
    fun `find that the actor is registered, with basic Find usage`(){
        val guest = testKit.spawn(VIPGuest.create(), "Mr.Wick")
        guest.tell(VIPGuest.Command.EnterHotel)
        val testProbe = testKit.createTestProbe<ActorRef<VIPGuest.Command>>()
        val finder = testKit.spawn(GuestSearch.create("Mr.Wick", testProbe.ref), "searcher1")
        finder.tell(GuestSearch.Command.Find)
        testProbe.expectMessageClass(ActorRef::class.java as Class<ActorRef<VIPGuest.Command>>)
    }

    @Test
    fun `find that the actor is registered, with search param in Find`(){
        val guest = testKit.spawn(VIPGuest.create(), "Mr.Wick")
        guest.tell(VIPGuest.Command.EnterHotel)
        val testProbe = testKit.createTestProbe<ActorRef<VIPGuest.Command>>()
        val finder = testKit.spawn(GuestFinder.create(), "searcher1")
        finder.tell(GuestFinder.Command.Find("Mr.Wick", testProbe.ref))
        testProbe.expectMessageClass(ActorRef::class.java as Class<ActorRef<VIPGuest.Command>>)
    }
}