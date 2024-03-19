import akka.actor.testkit.typed.javadsl.ActorTestKit
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Test

class CSVSpec{
    companion object {
        val testKit = ActorTestKit.create()

        @AfterAll
        @JvmStatic
        fun afterAll() {
            testKit.shutdownTestKit()
        }
    }

    @Test
    fun `A CSV Source should be readable`(){

    }
}