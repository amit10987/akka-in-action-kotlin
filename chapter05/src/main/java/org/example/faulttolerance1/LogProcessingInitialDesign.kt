package org.example.faulttolerance1

import akka.actor.typed.ActorSystem

fun main() {
    val sources = listOf("file:///source1/", "file:///source2/")
    val databaseUrl = "http://mydatabase1"

    val guardian = ActorSystem.create(
        LogProcessingGuardian.create(sources, databaseUrl),
        "log-processing-app"
    )



}