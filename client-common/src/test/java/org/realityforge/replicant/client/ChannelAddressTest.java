package org.realityforge.replicant.client;

import arez.Arez;
import org.testng.IHookCallBack;
import org.testng.IHookable;
import org.testng.ITestResult;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class ChannelAddressTest
  extends AbstractReplicantTest
  implements IHookable
{
  enum TestGraph
  {
    A, B
  }

  @Override
  public void run( final IHookCallBack callBack, final ITestResult testResult )
  {
    Arez.context().safeAction( () -> callBack.runTestMethod( testResult ) );
  }

  @SuppressWarnings( "EqualsWithItself" )
  @Test
  public void basicOperation()
  {
    final ChannelAddress descriptor1 = new ChannelAddress( TestGraph.A );
    final ChannelAddress descriptor2 = new ChannelAddress( TestGraph.B, 1 );

    assertEquals( descriptor1.getSystem(), TestGraph.class );
    assertEquals( descriptor1.getGraph(), TestGraph.A );
    assertEquals( descriptor1.getId(), null );
    assertEquals( descriptor1.toString(), "TestGraph.A" );
    assertEquals( descriptor1.equals( descriptor1 ), true );
    assertEquals( descriptor1.equals( descriptor2 ), false );

    assertEquals( descriptor2.getSystem(), TestGraph.class );
    assertEquals( descriptor2.getGraph(), TestGraph.B );
    assertEquals( descriptor2.getId(), 1 );
    assertEquals( descriptor2.toString(), "TestGraph.B:1" );
    assertEquals( descriptor2.equals( descriptor1 ), false );
    assertEquals( descriptor2.equals( descriptor2 ), true );
  }
}
