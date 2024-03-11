package projection.to.kafka

import akka.Done
import akka.actor.typed.ActorSystem
import akka.kafka.javadsl.SendProducer
import akka.projection.eventsourced.EventEnvelope
import akka.projection.javadsl.Handler
import com.google.protobuf.Any
import com.google.protobuf.BytesValue
import com.google.protobuf.empty.Empty
import market.domain.Market
import org.apache.kafka.clients.producer.ProducerRecord
import org.slf4j.LoggerFactory
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage

class MarketProjectionHandler(
    val system: ActorSystem<*>,
    private val topic: String,
    val producer: SendProducer<String, ByteArray>,
) : Handler<EventEnvelope<Market.Event>>() {

    companion object {
        val log = LoggerFactory.getLogger(MarketProjectionHandler::class.java)

    }

    override fun process(envelope: EventEnvelope<Market.Event>): CompletionStage<Done> {
        log.debug("processing market event [$envelope] to topic [$topic]}")

        val event = envelope.event()
        val serializedEvent = serialize(event)
        return if (serializedEvent.isNotEmpty()) {
            val record = ProducerRecord(topic, event.marketId, serializedEvent)
            return producer.send(record).handle { res, ex ->
                log.debug("published event [$event] to topic [$topic]}")
                Done.getInstance()
            }
        } else {
            CompletableFuture.completedFuture(Done.getInstance())
        }
    }

    private fun serialize(event: Market.Event): ByteArray {
        val proto = when (event) {
            is Market.Event.Closed -> event.result!!.let {
                betting.house.projection.proto.Market.MarketClosed.newBuilder()
                    .setMarketId(event.marketId)
                    .setResult(it)
                    .build().toByteString()
            }

            is Market.Event.Opened -> betting.house.projection.proto.Market.MarketOpened.newBuilder()
                .setMarketId(event.marketId)
                .build().toByteString()

            is Market.Event.Cancelled -> betting.house.projection.proto.Market.MarketCancelled.newBuilder()
                .setMarketId(event.marketId)
                .setReason(event.reason)
                .build().toByteString()

            else -> {
                LoggerFactory.getLogger(javaClass).info("ignoring event $event in projection")
                Empty.defaultInstance().toByteString()
            }
        }
        val builder = BytesValue.newBuilder()
        builder.value = proto
        return Any.pack(builder.build(), "market-projection").toByteArray()
    }

}
