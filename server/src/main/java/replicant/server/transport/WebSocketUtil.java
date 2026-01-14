package replicant.server.transport;

import java.io.IOException;
import javax.annotation.Nonnull;
import javax.websocket.Session;

@SuppressWarnings( "WeakerAccess" )
public final class WebSocketUtil
{
  private WebSocketUtil()
  {
  }

  public static boolean sendText( @Nonnull final Session session, @Nonnull final String message )
  {
    if ( session.isOpen() )
    {
      try
      {
        final var endpoint = session.getBasicRemote();
        endpoint.sendText( message );
        endpoint.flushBatch();
        return true;
      }
      catch ( final IOException ignored )
      {
        // This typically means that either the buffer is full or the websocket is in a bad state
        // Try to close the connection to let session be reaped.
        try
        {
          session.close();
        }
        catch ( final IOException ignore )
        {
          //Ignore as well.
        }
      }
      catch ( final NullPointerException ignored )
      {
        // The NullPointerException is due to a "bug" in either Tyrus (GlassFish websocket implementation)
        // or catalina. The bug is triggered when the remote side has reset the connection but it has yet to be
        // detected by catalina/tyrus. The bug occurs in org.apache.catalina.connector.Response.recycle() that
        // cleans blocks without locks in the incorrect order
        //
        // The stack trace looks something like.
        //java.lang.NullPointerException
        //  at org.apache.catalina.connector.OutputBuffer.isReady(OutputBuffer.java:491)
        //  at org.apache.catalina.connector.CoyoteOutputStream.isReady(CoyoteOutputStream.java:202)
        //  at org.glassfish.tyrus.servlet.TyrusServletWriter.write(TyrusServletWriter.java:132)
        //  at org.glassfish.tyrus.core.ProtocolHandler.write(ProtocolHandler.java:484)
        //  at org.glassfish.tyrus.core.ProtocolHandler.send(ProtocolHandler.java:270)
        //  at org.glassfish.tyrus.core.ProtocolHandler.send(ProtocolHandler.java:266)
        //  at org.glassfish.tyrus.core.ProtocolHandler.send(ProtocolHandler.java:315)
        //  at org.glassfish.tyrus.core.TyrusWebSocket.sendText(TyrusWebSocket.java:307)
        //  at org.glassfish.tyrus.core.TyrusRemoteEndpoint$Basic.sendText(TyrusRemoteEndpoint.java:101)
        //  at replicant.server.transport.WebSocketUtil.sendText(WebSocketUtil.java:27)

        // This thread yield attempts to ensure that the other thread progresses and the session is correctly
        // closed. Otherwise we end up in an exception in another thread that looks like:
        //java.io.IOException: Connection closed
        //	at org.glassfish.grizzly.asyncqueue.TaskQueue.onClose(TaskQueue.java:307)
        //	at org.glassfish.grizzly.nio.AbstractNIOAsyncQueueWriter.onClose(AbstractNIOAsyncQueueWriter.java:477)
        //	at org.glassfish.grizzly.nio.transport.TCPNIOTransport.closeConnection(TCPNIOTransport.java:388)
        //	at org.glassfish.grizzly.nio.NIOConnection.doClose(NIOConnection.java:643)
        //	at org.glassfish.grizzly.nio.NIOConnection$6.run(NIOConnection.java:609)
        //	at org.glassfish.grizzly.nio.DefaultSelectorHandler.execute(DefaultSelectorHandler.java:213)
        //	at org.glassfish.grizzly.nio.NIOConnection.terminate0(NIOConnection.java:603)
        //	at org.glassfish.grizzly.nio.transport.TCPNIOConnection.terminate0(TCPNIOConnection.java:267)
        //	at org.glassfish.grizzly.nio.transport.TCPNIOAsyncQueueWriter.writeCompositeRecord(TCPNIOAsyncQueueWriter.java:173)
        //	at org.glassfish.grizzly.nio.transport.TCPNIOAsyncQueueWriter.write0(TCPNIOAsyncQueueWriter.java:68)
        //	at org.glassfish.grizzly.nio.AbstractNIOAsyncQueueWriter.processAsync(AbstractNIOAsyncQueueWriter.java:320)
        //	at org.glassfish.grizzly.filterchain.DefaultFilterChain.process(DefaultFilterChain.java:84)
        //	at org.glassfish.grizzly.ProcessorExecutor.execute(ProcessorExecutor.java:53)
        //	at org.glassfish.grizzly.nio.transport.TCPNIOTransport.fireIOEvent(TCPNIOTransport.java:524)
        //	at org.glassfish.grizzly.strategies.AbstractIOStrategy.fireIOEvent(AbstractIOStrategy.java:89)
        //	at org.glassfish.grizzly.strategies.WorkerThreadIOStrategy.run0(WorkerThreadIOStrategy.java:94)
        //	at org.glassfish.grizzly.strategies.WorkerThreadIOStrategy.executeIoEvent(WorkerThreadIOStrategy.java:80)
        //	at org.glassfish.grizzly.strategies.AbstractIOStrategy.executeIoEvent(AbstractIOStrategy.java:66)
        //	at org.glassfish.grizzly.nio.SelectorRunner.iterateKeyEvents(SelectorRunner.java:391)
        //	at org.glassfish.grizzly.nio.SelectorRunner.iterateKeys(SelectorRunner.java:360)
        //	at org.glassfish.grizzly.nio.SelectorRunner.doSelect(SelectorRunner.java:324)
        //	at org.glassfish.grizzly.nio.SelectorRunner.run(SelectorRunner.java:255)
        //	at org.glassfish.grizzly.threadpool.AbstractThreadPool$Worker.doWork(AbstractThreadPool.java:569)
        //	at org.glassfish.grizzly.threadpool.AbstractThreadPool$Worker.run(AbstractThreadPool.java:549)
        //	at java.lang.Thread.run(Thread.java:748)
        //Caused by: java.io.IOException: Connection reset by peer
        //	at sun.nio.ch.FileDispatcherImpl.write0(Native Method)
        //	at sun.nio.ch.SocketDispatcher.write(SocketDispatcher.java:47)
        //	at sun.nio.ch.IOUtil.writeFromNativeBuffer(IOUtil.java:93)
        //	at sun.nio.ch.IOUtil.write(IOUtil.java:51)
        //	at sun.nio.ch.SocketChannelImpl.write(SocketChannelImpl.java:471)
        //	at org.glassfish.grizzly.nio.transport.TCPNIOUtils.flushByteBuffer(TCPNIOUtils.java:125)
        //	at org.glassfish.grizzly.nio.transport.TCPNIOAsyncQueueWriter.writeCompositeRecord(TCPNIOAsyncQueueWriter.java:165)
        //	... 16 more
        Thread.yield();
        try
        {
          session.close();
        }
        catch ( final IOException ignore )
        {
          //Ignore as well.
        }
      }
    }
    return false;
  }
}
