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
  enum TestSystem
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
    final ChannelAddress descriptor1 = new ChannelAddress( TestSystem.A );
    final ChannelAddress descriptor2 = new ChannelAddress( TestSystem.B, 1 );

    assertEquals( descriptor1.getSystem(), TestSystem.class );
    assertEquals( descriptor1.getChannelType(), TestSystem.A );
    assertEquals( descriptor1.getId(), null );
    assertEquals( descriptor1.toString(), "TestSystem.A" );
    assertEquals( descriptor1.equals( descriptor1 ), true );
    assertEquals( descriptor1.equals( descriptor2 ), false );
    assertEquals( descriptor1.compareTo( descriptor1 ), 0 );
    assertEquals( descriptor1.compareTo( descriptor2 ), -1 );

    assertEquals( descriptor2.getSystem(), TestSystem.class );
    assertEquals( descriptor2.getChannelType(), TestSystem.B );
    assertEquals( descriptor2.getId(), 1 );
    assertEquals( descriptor2.toString(), "TestSystem.B:1" );
    assertEquals( descriptor2.equals( descriptor1 ), false );
    assertEquals( descriptor2.equals( descriptor2 ), true );
    assertEquals( descriptor2.compareTo( descriptor1 ), 1 );
    assertEquals( descriptor2.compareTo( descriptor2 ), 0 );
  }
}
