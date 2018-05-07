package replicant;

import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class ChannelAddressTest
  extends AbstractReplicantTest
{
  enum TestSystem
  {
    A, B
  }

  @Test
  public void construct()
  {
    final ChannelAddress address = new ChannelAddress( TestSystem.B, 1 );

    assertEquals( address.getSystem(), TestSystem.class );
    assertEquals( address.getChannelType(), TestSystem.B );
    assertEquals( address.getId(), (Integer) 1 );
  }

  @SuppressWarnings( "EqualsWithItself" )
  @Test
  public void testEquals()
  {
    final ChannelAddress address = new ChannelAddress( TestSystem.B, 1 );
    final ChannelAddress address2 = new ChannelAddress( TestSystem.B, 2 );
    final ChannelAddress address3 = new ChannelAddress( TestSystem.A );

    assertEquals( address.equals( address ), true );
    assertEquals( address.equals( new Object() ), false );
    assertEquals( address.equals( address2 ), false );
    assertEquals( address.equals( address3 ), false );
  }

  @Test
  public void toStringTest()
  {
    final ChannelAddress address = new ChannelAddress( TestSystem.B, 1 );
    assertEquals( address.toString(), "TestSystem.B:1" );
  }

  @Test
  public void toStringTest_NamingDisabled()
  {
    ReplicantTestUtil.disableNames();
    final ChannelAddress address = new ChannelAddress( TestSystem.B, 1 );
    assertEquals( address.toString(), "replicant.ChannelAddress@" + Integer.toHexString( address.hashCode() ) );
  }

  @Test
  public void getName_NamingDisabled()
  {
    ReplicantTestUtil.disableNames();
    final ChannelAddress address = new ChannelAddress( TestSystem.B, 1 );
    final IllegalStateException exception = expectThrows( IllegalStateException.class, address::getName );
    assertEquals( exception.getMessage(),
                  "Replicant-0054: ChannelAddress.getName() invoked when Replicant.areNamesEnabled() is false" );
  }

  @SuppressWarnings( "EqualsWithItself" )
  @Test
  public void compareTo()
  {
    final ChannelAddress address1 = new ChannelAddress( TestSystem.A );
    final ChannelAddress address2 = new ChannelAddress( TestSystem.B, 1 );

    assertEquals( address1.compareTo( address1 ), 0 );
    assertEquals( address1.compareTo( address2 ), -1 );
    assertEquals( address2.compareTo( address1 ), 1 );
    assertEquals( address2.compareTo( address2 ), 0 );
  }
}
