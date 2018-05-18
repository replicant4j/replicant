package replicant;

import org.realityforge.guiceyloops.shared.ValueUtil;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class ChannelSchemaTest
  extends AbstractReplicantTest
{
  @Test
  public void typeGraph()
  {
    final ChannelSchema channelSchema =
      new ChannelSchema( 1, "MetaData", true, ChannelSchema.FilterType.NONE, false, false );
    assertEquals( channelSchema.getId(), 1 );
    assertEquals( channelSchema.getName(), "MetaData" );
    assertEquals( channelSchema.toString(), "MetaData" );
    assertEquals( channelSchema.isTypeChannel(), true );
    assertEquals( channelSchema.isInstanceChannel(), false );
    assertEquals( channelSchema.getFilterType(), ChannelSchema.FilterType.NONE );
    assertEquals( channelSchema.isCacheable(), false );
    assertEquals( channelSchema.isExternal(), false );
  }

  @Test
  public void instanceGraph()
  {
    final ChannelSchema channelSchema =
      new ChannelSchema( 1, "MetaData", false, ChannelSchema.FilterType.NONE, false, true );
    assertEquals( channelSchema.getId(), 1 );
    assertEquals( channelSchema.getName(), "MetaData" );
    assertEquals( channelSchema.toString(), "MetaData" );
    assertEquals( channelSchema.isTypeChannel(), false );
    assertEquals( channelSchema.isInstanceChannel(), true );
    assertEquals( channelSchema.getFilterType(), ChannelSchema.FilterType.NONE );
    assertEquals( channelSchema.isCacheable(), false );
    assertEquals( channelSchema.isExternal(), true );
  }

  @Test
  public void staticFilteredGraph()
  {
    final int id = ValueUtil.randomInt();
    final String name = ValueUtil.randomString();
    final boolean typeChannel = false;
    final boolean cacheable = false;
    final boolean external = true;
    final ChannelSchema channelSchema =
      new ChannelSchema( id, name, typeChannel, ChannelSchema.FilterType.STATIC, cacheable, external );
    assertEquals( channelSchema.getId(), id );
    assertEquals( channelSchema.getName(), name );
    assertEquals( channelSchema.toString(), name );
    assertEquals( channelSchema.isTypeChannel(), typeChannel );
    assertEquals( channelSchema.isInstanceChannel(), true );
    assertEquals( channelSchema.getFilterType(), ChannelSchema.FilterType.STATIC );
    assertEquals( channelSchema.isCacheable(), cacheable );
    assertEquals( channelSchema.isExternal(), external );
  }

  @Test
  public void dynamicFilteredGraph()
  {
    final int id = ValueUtil.randomInt();
    final String name = ValueUtil.randomString();
    final boolean cacheable = false;
    final boolean external = true;
    final ChannelSchema channelSchema =
      new ChannelSchema( id, name, true, ChannelSchema.FilterType.DYNAMIC, cacheable, external );
    assertEquals( channelSchema.getId(), id );
    assertEquals( channelSchema.getName(), name );
    assertEquals( channelSchema.toString(), name );
    assertEquals( channelSchema.isTypeChannel(), true );
    assertEquals( channelSchema.isInstanceChannel(), false );
    assertEquals( channelSchema.getFilterType(), ChannelSchema.FilterType.DYNAMIC );
    assertEquals( channelSchema.isCacheable(), cacheable );
    assertEquals( channelSchema.isExternal(), external );
  }

  @Test
  public void noNameSuppliedWhenNamesDisabled()
  {
    ReplicantTestUtil.disableNames();
    final ChannelSchema channelSchema =
      new ChannelSchema( ValueUtil.randomInt(),
                         null,
                         ValueUtil.randomBoolean(),
                         ChannelSchema.FilterType.NONE,
                         ValueUtil.randomBoolean(),
                         ValueUtil.randomBoolean() );
    final IllegalStateException exception = expectThrows( IllegalStateException.class, channelSchema::getName );
    assertEquals( exception.getMessage(),
                  "Replicant-0044: ChannelSchema.getName() invoked when Replicant.areNamesEnabled() is false" );
    assertEquals( channelSchema.toString(),
                  "replicant.ChannelSchema@" + Integer.toHexString( channelSchema.hashCode() ) );
  }

  @Test
  public void passNameToConstructorWhenNamesDisabled()
  {
    ReplicantTestUtil.disableNames();
    final IllegalStateException exception =
      expectThrows( IllegalStateException.class,
                    () -> new ChannelSchema( ValueUtil.randomInt(),
                                             "MyChannel",
                                             ValueUtil.randomBoolean(),
                                             ChannelSchema.FilterType.NONE,
                                             ValueUtil.randomBoolean(),
                                             ValueUtil.randomBoolean() ) );
    assertEquals( exception.getMessage(),
                  "Replicant-0045: ChannelSchema passed a name 'MyChannel' but Replicant.areNamesEnabled() is false" );
  }
}
