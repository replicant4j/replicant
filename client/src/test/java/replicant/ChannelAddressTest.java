package replicant;

import arez.Arez;
import org.realityforge.replicant.client.AbstractReplicantTest;
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
    final ChannelAddress address1 = new ChannelAddress( TestSystem.A );
    final ChannelAddress address2 = new ChannelAddress( TestSystem.B, 1 );

    assertEquals( address1.getSystem(), TestSystem.class );
    assertEquals( address1.getChannelType(), TestSystem.A );
    assertEquals( address1.getId(), null );
    assertEquals( address1.toString(), "TestSystem.A" );
    assertEquals( address1.equals( address1 ), true );
    assertEquals( address1.equals( address2 ), false );
    assertEquals( address1.compareTo( address1 ), 0 );
    assertEquals( address1.compareTo( address2 ), -1 );

    assertEquals( address2.getSystem(), TestSystem.class );
    assertEquals( address2.getChannelType(), TestSystem.B );
    assertEquals( address2.getId(), 1 );
    assertEquals( address2.toString(), "TestSystem.B:1" );
    assertEquals( address2.equals( address1 ), false );
    assertEquals( address2.equals( address2 ), true );
    assertEquals( address2.compareTo( address1 ), 1 );
    assertEquals( address2.compareTo( address2 ), 0 );
  }
}
