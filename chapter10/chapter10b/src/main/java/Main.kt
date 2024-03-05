import akka.NotUsed
import akka.actor.typed.ActorSystem
import akka.actor.typed.javadsl.Behaviors
import akka.persistence.jdbc.query.javadsl.JdbcReadJournal
import akka.persistence.query.PersistenceQuery
import akka.stream.javadsl.Source

fun main() {
    val system = ActorSystem.create(Behaviors.ignore<String>(), "containers")

    val readJournal: JdbcReadJournal =
        PersistenceQuery.get(system).getReadJournalFor(JdbcReadJournal::class.java, JdbcReadJournal.Identifier())

    val source: Source<String, NotUsed> = readJournal.persistenceIds()

    source.runForeach({
        println("-----> $it")
    }, system)

}