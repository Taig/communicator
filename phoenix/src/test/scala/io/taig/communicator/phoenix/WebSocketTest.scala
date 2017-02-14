package io.taig.communicator.phoenix

import io.taig.communicator.OkHttpRequest
import monix.eval.Task

import scala.concurrent.duration._
import scala.language.postfixOps

class WebSocketTest extends Suite {
    override val request = new OkHttpRequest.Builder()
        .url( "wss://echo.websocket.org" )
        .build()

    it should "open a connection" in {
        WebSocket( request ).share.firstL.runAsync.map {
            _ shouldBe a[WebSocket.Event.Open]
        }
    }

    it should "receive echo messages" in {
        val observable = WebSocket( request ).share

        val receive: Task[List[String]] = observable.collect {
            case WebSocket.Event.Message( Right( value ) ) ⇒ value
        }.take( 2 ).toListL

        val send: Task[Unit] = observable.collect {
            case WebSocket.Event.Open( socket, _ ) ⇒ socket
        }.firstL.foreachL { socket ⇒
            socket.send( "foo" )
            socket.send( "bar" )
            ()
        }

        Task.mapBoth( receive, send )( ( values, _ ) ⇒ values )
            .runAsync
            .map {
                _ should contain theSameElementsAs List( "foo", "bar" )
            }
    }

    it should "reconnect after failure" in {
        var count = 0

        WebSocket(
            request,
            failureReconnect = Some( 100 milliseconds )
        ).share.collect {
                case WebSocket.Event.Open( socket, _ ) ⇒
                    socket.cancel()
                    count += 1
                    count
            }.take( 2 ).toListL.timeout( 10 seconds ).runAsync.map {
                _ should contain theSameElementsAs List( 1, 2 )
            }
    }

    it should "reconnect after complete" in {
        var count = 0

        WebSocket(
            request,
            completeReconnect = Some( 100 milliseconds )
        ).share.collect {
                case WebSocket.Event.Open( socket, _ ) ⇒
                    socket.close( 1000, null )
                    count += 1
                    count
            }.take( 2 ).toListL.timeout( 10 seconds ).runAsync.map {
                _ should contain theSameElementsAs List( 1, 2 )
            }
    }
}