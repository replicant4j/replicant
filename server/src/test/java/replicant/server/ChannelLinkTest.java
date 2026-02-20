package replicant.server;

import javax.annotation.Nonnull;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

public final class ChannelLinkTest
{
  @Test
  public void basicOperation()
  {
    final var link = new ChannelLink( new ChannelAddress( 22, 44 ), new ChannelAddress( 1, 2 ), null );
    assertEquals( link.source().channelId(), 22 );
    assertEquals( link.source().rootId(), (Integer) 44 );
    assertEquals( link.target().channelId(), 1 );
    assertEquals( link.target().rootId(), (Integer) 2 );
    assertEquals( link.toString(), "[22.44=>1.2]" );
  }

  @Test
  public void hashcodeAndEquals()
  {
    final var link1 = new ChannelLink( new ChannelAddress( 22, 44 ), new ChannelAddress( 1, 2 ), null );
    final var link2 = new ChannelLink( new ChannelAddress( 22, 44 ), new ChannelAddress( 1, 3 ), null );
    final var link3 = new ChannelLink( new ChannelAddress( 22, 77 ), new ChannelAddress( 1, 2 ), null );
    final var link4 = new ChannelLink( new ChannelAddress( 27 ), new ChannelAddress( 1, 2 ), null );
    final var link5 = new ChannelLink( new ChannelAddress( 27 ), new ChannelAddress( 1, 3 ), null );

    assertLinkEqual( link1, link1 );
    assertLinkEqual( link2, link2 );
    assertLinkEqual( link3, link3 );
    assertLinkEqual( link4, link4 );
    assertLinkEqual( link5, link5 );

    assertLinkNotEqual( link1, link2 );
    assertLinkNotEqual( link1, link3 );
    assertLinkNotEqual( link1, link4 );
    assertLinkNotEqual( link1, link5 );

    assertLinkNotEqual( link2, link3 );
    assertLinkNotEqual( link2, link4 );
    assertLinkNotEqual( link2, link5 );

    assertLinkNotEqual( link3, link4 );
    assertLinkNotEqual( link3, link5 );

    assertLinkNotEqual( link4, link5 );
  }

  private void assertLinkEqual( @Nonnull final ChannelLink link1, @Nonnull final ChannelLink link2 )
  {
    assertEquals( link1, link2 );
    assertEquals( link1.hashCode(), link2.hashCode() );
  }

  private void assertLinkNotEqual( @Nonnull final ChannelLink link1, @Nonnull final ChannelLink link2 )
  {
    assertNotEquals( link1, link2 );
    assertNotEquals( link1.hashCode(), link2.hashCode() );
  }
}
