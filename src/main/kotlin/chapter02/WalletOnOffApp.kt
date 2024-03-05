package chapter02

import akka.actor.typed.ActorSystem

fun main() {
    val guardian = ActorSystem.create(WalletOnOff.receiver(), "wallet-on-off-state")

    guardian.tell(WalletOnOff.Command.Increase(1))
    guardian.tell(WalletOnOff.Command.Increase(10))
    guardian.tell(WalletOnOff.Command.Deactivate)
    guardian.tell(WalletOnOff.Command.Increase(1))
    guardian.tell(WalletOnOff.Command.Activate)
    guardian.tell(WalletOnOff.Command.Increase(1))

    println("Press ENTER to terminate")
    readlnOrNull()
    guardian.terminate()
}