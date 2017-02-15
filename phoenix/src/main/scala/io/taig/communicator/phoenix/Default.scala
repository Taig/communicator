package io.taig.communicator.phoenix

import scala.concurrent.duration._
import scala.language.postfixOps

object Default {
    val completeReconnect: Option[FiniteDuration] = None

    val failureReconnect: Option[FiniteDuration] = None

    val heartbeat: Option[FiniteDuration] = Some( 10 seconds )

    val timeout: FiniteDuration = 10 seconds
}