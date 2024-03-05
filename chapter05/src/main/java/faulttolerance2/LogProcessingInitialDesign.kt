package faulttolerance2

import akka.actor.typed.ActorSystem

fun main() {
    val directories = listOf("file:///source1/", "file:///source2/")
    val databaseUrl = "http://mydatabase1"

    val guardian = ActorSystem.create(
        LogProcessingGuardian.create(directories, databaseUrl),
        "log-processing-app"
    )



}