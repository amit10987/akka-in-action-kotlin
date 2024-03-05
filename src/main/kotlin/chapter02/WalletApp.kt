package chapter02

import chapter02.Wallet.intReceiver
import akka.actor.typed.ActorSystem

fun main(args: Array<String>) {
    val guardian = ActorSystem.create(intReceiver(), "IntReceiverSystem")
    guardian.tell(1)
    guardian.tell(10)
    println("Press ENTER to terminate")
    readlnOrNull()
    guardian.terminate()
}