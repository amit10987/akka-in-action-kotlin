package chapter02

import akka.actor.typed.ActorSystem

fun main() {
    val guardian = ActorSystem.create(WalletState.receiver(0, 2), "wallet-state")
    guardian.tell(WalletState.Command.Increase(1))
    guardian.tell(WalletState.Command.Increase(1))
    guardian.tell(WalletState.Command.Increase(1))
    guardian.terminate()
}