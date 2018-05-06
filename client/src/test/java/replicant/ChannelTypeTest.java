package replicant;

import org.realityforge.guiceyloops.shared.ValueUtil;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class ChannelTypeTest
  extends AbstractReplicantTest
{
  @Test
  public void typeGraph()
  {
    final ChannelType channelType =
      new ChannelType( 1, "MetaData", true, ChannelType.FilterType.NONE, false, false );
    assertEquals( channelType.getId(), 1 );
    assertEquals( channelType.getName(), "MetaData" );
    assertEquals( channelType.toString(), "MetaData" );
    assertEquals( channelType.isTypeChannel(), true );
    assertEquals( channelType.isInstanceChannel(), false );
    assertEquals( channelType.getFilterType(), ChannelType.FilterType.NONE );
    assertEquals( channelType.isCacheable(), false );
    assertEquals( channelType.isExternal(), false );
  }

  @Test
  public void instanceGraph()
  {
    final ChannelType channelType =
      new ChannelType( 1, "MetaData", false, ChannelType.FilterType.NONE, false, true );
    assertEquals( channelType.getId(), 1 );
    assertEquals( channelType.getName(), "MetaData" );
    assertEquals( channelType.toString(), "MetaData" );
    assertEquals( channelType.isTypeChannel(), false );
    assertEquals( channelType.isInstanceChannel(), true );
    assertEquals( channelType.getFilterType(), ChannelType.FilterType.NONE );
    assertEquals( channelType.isCacheable(), false );
    assertEquals( channelType.isExternal(), true );
  }

  @Test
  public void staticFilteredGraph()
  {
    final int id = ValueUtil.randomInt();
    final String name = ValueUtil.randomString();
    final boolean typeChannel = false;
    final boolean cacheable = false;
    final boolean external = true;
    final ChannelType channelType =
      new ChannelType( id, name, typeChannel, ChannelType.FilterType.STATIC, cacheable, external );
    assertEquals( channelType.getId(), id );
    assertEquals( channelType.getName(), name );
    assertEquals( channelType.toString(), name );
    assertEquals( channelType.isTypeChannel(), typeChannel );
    assertEquals( channelType.isInstanceChannel(), true );
    assertEquals( channelType.getFilterType(), ChannelType.FilterType.STATIC );
    assertEquals( channelType.isCacheable(), cacheable );
    assertEquals( channelType.isExternal(), external );
  }

  @Test
  public void dynamicFilteredGraph()
  {
    final int id = ValueUtil.randomInt();
    final String name = ValueUtil.randomString();
    final boolean cacheable = false;
    final boolean external = true;
    final ChannelType channelType =
      new ChannelType( id, name, true, ChannelType.FilterType.DYNAMIC, cacheable, external );
    assertEquals( channelType.getId(), id );
    assertEquals( channelType.getName(), name );
    assertEquals( channelType.toString(), name );
    assertEquals( channelType.isTypeChannel(), true );
    assertEquals( channelType.isInstanceChannel(), false );
    assertEquals( channelType.getFilterType(), ChannelType.FilterType.DYNAMIC );
    assertEquals( channelType.isCacheable(), cacheable );
    assertEquals( channelType.isExternal(), external );
  }

  @Test
  public void noNameSuppliedWhenNamesDisabled()
  {
    ReplicantTestUtil.disableNames();
    final ChannelType channelType =
      new ChannelType( ValueUtil.randomInt(),
                       null,
                       ValueUtil.randomBoolean(),
                       ChannelType.FilterType.NONE,
                       ValueUtil.randomBoolean(),
                       ValueUtil.randomBoolean() );
    final IllegalStateException exception = expectThrows( IllegalStateException.class, channelType::getName );
    assertEquals( exception.getMessage(),
                  "Replicant-0053: ChannelType.getName() invoked when Replicant.areNamesEnabled() is false" );
    assertEquals( channelType.toString(), "replicant.ChannelType@" + Integer.toHexString( channelType.hashCode() ) );
  }

  @Test
  public void passNameToConstructorWhenNamesDisabled()
  {
    ReplicantTestUtil.disableNames();
    final IllegalStateException exception =
      expectThrows( IllegalStateException.class,
                    () -> new ChannelType( ValueUtil.randomInt(),
                                           "MyChannel",
                                           ValueUtil.randomBoolean(),
                                           ChannelType.FilterType.NONE,
                                           ValueUtil.randomBoolean(),
                                           ValueUtil.randomBoolean() ) );
    assertEquals( exception.getMessage(),
                  "Replicant-0052: ChannelType passed a name 'MyChannel' but Replicant.areNamesEnabled() is false" );
  }
}
