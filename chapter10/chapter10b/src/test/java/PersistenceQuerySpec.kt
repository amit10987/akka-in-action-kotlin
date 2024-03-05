import akka.NotUsed
import akka.actor.testkit.typed.javadsl.ActorTestKit
import akka.persistence.jdbc.query.javadsl.JdbcReadJournal
import akka.persistence.query.EventEnvelope
import akka.persistence.query.Offset
import akka.persistence.query.PersistenceQuery
import akka.stream.javadsl.Keep
import akka.stream.javadsl.Source
import akka.stream.testkit.javadsl.TestSink
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Test
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

class PersistenceQuerySpec {

    companion object {
        val testKit = ActorTestKit.create()

        @AfterAll
        @JvmStatic
        fun afterAll() {
            testKit.shutdownTestKit()
        }
    }

    @Test
    fun `a persistence query should retrieve the persistenceIds from db and printing them`() {
        val readJournal: JdbcReadJournal = PersistenceQuery.get(testKit.system())
            .getReadJournalFor(JdbcReadJournal::class.java, JdbcReadJournal.Identifier())

        val source: Source<String, NotUsed> = readJournal.persistenceIds()

        source.runForeach({ println("values -> $it") }, testKit.system())

        Thread.sleep(3000)
    }

    @Test
    fun `a persistence query should retrieve the events from db and printing them`() {
        val readJournal: JdbcReadJournal = PersistenceQuery.get(testKit.system())
            .getReadJournalFor(JdbcReadJournal::class.java, JdbcReadJournal.Identifier())

        val source = readJournal.eventsByTag("container-tag-0", Offset.noOffset())
        source.runForeach({ println("event -> ${it.event()}") }, testKit.system())

        Thread.sleep(3000)
    }

    @Test
    fun `a persistence query should retrieve the data from db`() {
        val readJournal: JdbcReadJournal = PersistenceQuery.get(testKit.system())
            .getReadJournalFor(JdbcReadJournal::class.java, JdbcReadJournal.Identifier())

        val source: Source<String, NotUsed> = readJournal.persistenceIds()

        val consumer = TestSink.create<String>(testKit.system())

        val probe = source.toMat(consumer, Keep.right()).run(testKit.system())

        println("---> ${probe.requestNext()}")
        println("---> ${probe.requestNext()}")

    }

    @Test
    fun `a persistence query should retrieve the data from db by tag`() {
        val readJournal: JdbcReadJournal = PersistenceQuery.get(testKit.system())
            .getReadJournalFor(JdbcReadJournal::class.java, JdbcReadJournal.Identifier())

        val source = readJournal.eventsByTag("container-tag-0", Offset.noOffset())

        val consumer = TestSink.create<EventEnvelope>(testKit.system())

        val probe = source.toMat(consumer, Keep.right()).run(testKit.system())

        println("---> ${probe.requestNext()}")
        println("---> ${probe.requestNext()}")

    }
}