package chapter02

import akka.actor.typed.ActorSystem

fun main() {
    val guardian = ActorSystem.create(WalletTimer.receiver(), "wallet-timer")

    guardian.tell(WalletTimer.Command.Increase(1))
    guardian.tell(WalletTimer.Command.Deactivate(3))


}