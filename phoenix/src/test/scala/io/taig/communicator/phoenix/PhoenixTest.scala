package io.taig.communicator.phoenix

import io.circe.Json
import io.taig.communicator.websocket.WebSocket
import io.taig.phoenix.models._

import scala.concurrent.TimeoutException
import scala.concurrent.duration._
import scala.language.postfixOps

class PhoenixTest extends Suite {
    it should "send a heartbeat" in {
        Phoenix(
            WebSocket( request ),
            heartbeat = Some( 1 second )
        ).share.collect {
                case Phoenix.Event.Available( phoenix ) ⇒ phoenix
            }.flatMap( _.stream ).collect {
                case confirmation: Response.Confirmation ⇒ confirmation
            }.firstL.timeout( 10 seconds ).runAsync.map { confirmation ⇒
                confirmation.topic shouldBe Topic.Phoenix
                confirmation.payload shouldBe Json.obj()
            }
    }

    it should "allow to disable the heartbeat" in {
        Phoenix( WebSocket( request ), heartbeat = None ).share.collect {
            case Phoenix.Event.Available( phoenix ) ⇒ phoenix
        }.flatMap( _.stream ).collect {
            case confirmation: Response.Confirmation ⇒ confirmation
        }.firstOptionL
            .timeout( 10 seconds )
            .onErrorRecover { case _: TimeoutException ⇒ None }
            .runAsync
            .map( _ shouldBe None )
    }

    it should "allow to join a Channel" in {
        val topic = Topic( "echo", "foobar" )

        val phoenix = Phoenix( WebSocket( request ) )
        val channel = Channel.join( topic )( phoenix ).share

        channel.collect {
            case Channel.Event.Available( channel ) ⇒ channel
        }.firstL.timeout( 10 seconds ).runAsync.map { channel ⇒
            channel.topic shouldBe topic
        }
    }

    it should "rejoin a Channel when reconnecting" in {
        val topic = Topic( "echo", "foobar" )

        val phoenix = Phoenix(
            WebSocket(
                request,
                failureReconnect = Some( 500 milliseconds )
            )
        ).share
        val channel = Channel.join( topic )( phoenix )

        channel.collect {
            case Channel.Event.Available( channel ) ⇒
                channel.socket.cancel()
                channel
        }.take( 2 ).toListL.timeout( 10 seconds ).runAsync.map {
            _ should have length 2
        }
    }

    it should "fail to join an invalid Channel" in {
        val topic = Topic( "foo", "bar" )

        val phoenix = Phoenix( WebSocket( request ) ).share
        val channel = Channel.join( topic )( phoenix )

        channel.collect {
            case Channel.Event.Failure( response ) ⇒ response
        }.firstL.timeout( 10 seconds ).runAsync.map {
            _.map( _.message ) shouldBe Some( "unmatched topic" )
        }
    }

    it should "return None when the server omits a response" in {
        val topic = Topic( "echo", "foobar" )

        val phoenix = Phoenix(
            WebSocket( request ),
            timeout = 1 second
        ).share
        val channel = Channel.join( topic )( phoenix )

        channel.collect {
            case Channel.Event.Available( channel ) ⇒ channel
        }.mapTask { channel ⇒
            channel.send( Event( "no_reply" ), Json.Null )
        }.firstL.timeout( 10 seconds ).runAsync.map {
            _ should not be defined
        }
    }
}