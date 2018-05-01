package org.realityforge.replicant.client;

import arez.Arez;
import arez.ArezTestUtil;
import arez.Disposable;
import org.testng.IHookCallBack;
import org.testng.IHookable;
import org.testng.ITestResult;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class ChannelTest
  extends AbstractReplicantTest
  implements IHookable
{
  enum TestSystem
  {
    A, B
  }

  @Override
  public void run( final IHookCallBack callBack, final ITestResult testResult )
  {
    Arez.context().safeAction( () -> callBack.runTestMethod( testResult ) );
  }

  @Test
  public void basicChannelOperation()
  {
    final ChannelAddress address = new ChannelAddress( TestSystem.A, null );

    final Channel subscription = Channel.create( address, null );

    assertEquals( subscription.getAddress(), address );
    assertEquals( Disposable.isDisposed( subscription ), false );
    assertEquals( subscription.getFilter(), null );

    // Update subscription works ....

    final Object filter = new Object();
    subscription.setFilter( filter );
    assertEquals( subscription.getFilter(), filter );

    Disposable.dispose( subscription );

    assertEquals( Disposable.isDisposed( subscription ), true );
  }

  @SuppressWarnings( "EqualsWithItself" )
  @Test
  public void comparable()
  {

    final ChannelAddress address1 = new ChannelAddress( TestSystem.A );
    final ChannelAddress address2 = new ChannelAddress( TestSystem.B );

    final Channel channel1 = Channel.create( address1 );
    final Channel channel2 = Channel.create( address2 );

    assertEquals( channel1.compareTo( channel1 ), 0 );
    assertEquals( channel1.compareTo( channel2 ), -1 );
    assertEquals( channel2.compareTo( channel1 ), 1 );
    assertEquals( channel2.compareTo( channel2 ), 0 );
  }

  @Test
  public void test_toString()
  {
    final ChannelAddress address = new ChannelAddress( TestSystem.A, null );

    final Channel channel = Channel.create( address, null );

    assertEquals( channel.toString(), "Channel[" + address + " :: Filter=null]" );

    ArezTestUtil.disableNames();

    assertTrue( channel.toString().startsWith( "org.realityforge.replicant.client.Arez_Channel@" ),
                channel.toString() );
  }
}
