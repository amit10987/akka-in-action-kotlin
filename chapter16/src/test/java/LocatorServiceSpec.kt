import akka.actor.testkit.typed.javadsl.ActorTestKit
import akka.grpc.GrpcClientSettings
import akka.stream.RestartSettings
import akka.stream.javadsl.RestartSource
import com.google.protobuf.empty.Empty
import example.locator.grpc.LocatorServiceClient
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Test
import java.time.Duration

class LocatorServiceSpec {

    companion object {
        val testKit = ActorTestKit.create()

        @AfterAll
        @JvmStatic
        fun afterAll() {
            testKit.shutdownTestKit()
        }
    }

    @Test
    fun `A Source should be able to keep consuming from a failed and restored service`() {
        val clientSettings =
            GrpcClientSettings
                .connectToServiceAt("127.0.0.1", 8080, testKit.system())
                .withTls(false)

        val client = LocatorServiceClient.create(clientSettings, testKit.system())

        val restartSettings = RestartSettings.create(
            Duration.ofSeconds(1),
            Duration.ofSeconds(3),
            0.2
        ).withMaxRestarts(30, Duration.ofMinutes(3))

        RestartSource.withBackoff(restartSettings) {
            client.follow(com.google.protobuf.Empty.getDefaultInstance())
        }.map { println(it) }.run(testKit.system())
            .toCompletableFuture().get(90, java.util.concurrent.TimeUnit.SECONDS)
    }
}