import akka.actor.testkit.typed.javadsl.ActorTestKit
import akka.stream.javadsl.Source
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Test
import java.math.BigInteger
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit

class CPUBoundedSpec {
    companion object {
        val testKit = ActorTestKit.create()

        @AfterAll
        @JvmStatic
        fun afterAll() {
            testKit.shutdownTestKit()
        }
    }

    @Test
    fun `calling to a local function sync should eventually return ignore`() {
        val future = Source.range(1, 10)
            .map { each -> parsingDoc(each.toString()) }
            .run(testKit.system())

        val res = future.toCompletableFuture().get(30, TimeUnit.SECONDS)
        println(res)
    }

    @Test
    fun `calling to a local function async should eventually return ignore`() {
        val future = Source.range(1, 10)
            .mapAsync(10) { each ->
                CompletableFuture.supplyAsync { parsingDoc(each.toString()) }
            }
            .run(testKit.system())

        val res = future.toCompletableFuture().get(30, TimeUnit.SECONDS)
        println(res)
    }


    // Internal call, CPU bounded
// 80000 takes about 1-3 seconds one call
// Load is cumulative and the more threads we run
// the more they have to share the CPU
    private fun parsingDoc(doc: String): String {
        val init = System.currentTimeMillis()
        factorial(BigInteger("80000"))
        println("${((System.currentTimeMillis() - init).toDouble() / 1000)}s")
        return doc
    }

    private fun factorial(x: BigInteger): BigInteger {
        tailrec fun loop(x: BigInteger, acc: BigInteger = BigInteger.ONE): BigInteger {
            return if (x <= BigInteger.ONE) acc
            else loop(x - BigInteger.ONE, x * acc)
        }
        return loop(x)
    }
}
