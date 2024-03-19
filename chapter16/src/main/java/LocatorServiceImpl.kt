import akka.NotUsed
import akka.actor.typed.ActorSystem
import akka.stream.javadsl.AsPublisher
import akka.stream.javadsl.Sink
import akka.stream.javadsl.Source
import com.google.protobuf.Empty
import example.locator.grpc.LocatorProto
import example.locator.grpc.LocatorService

class LocatorServiceImpl(val system: ActorSystem<*>, private val init: Int) : LocatorService {
    override fun follow(`in`: Empty): Source<LocatorProto.Location, NotUsed> {
        val it = (init until init + 100).map {
            LocatorProto.Location.newBuilder().setLat(it.toDouble()).setLon(-3.701101).build()
        }.iterator()

        val sou = Source.fromIterator { it }.throttle(1, java.time.Duration.ofSeconds(1))
            .runWith(Sink.asPublisher(AsPublisher.WITHOUT_FANOUT), system)

       return Source.fromPublisher(sou)
    }

}