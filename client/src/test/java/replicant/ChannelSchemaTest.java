package replicant;

import org.realityforge.guiceyloops.shared.ValueUtil;
import org.testng.annotations.Test;
import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

public class ChannelSchemaTest
  extends AbstractReplicantTest
{
  @Test
  public void typeGraph()
  {
    final ChannelSchema channelSchema =
      new ChannelSchema( 1, "MetaData", true, ChannelSchema.FilterType.NONE, null, false, false );
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
      new ChannelSchema( 1, "MetaData", false, ChannelSchema.FilterType.NONE, null, false, true );
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
      new ChannelSchema( id, name, typeChannel, ChannelSchema.FilterType.STATIC, null, cacheable, external );
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
    final SubscriptionUpdateEntityFilter filter = mock( SubscriptionUpdateEntityFilter.class );
    final ChannelSchema channelSchema =
      new ChannelSchema( id, name, true, ChannelSchema.FilterType.DYNAMIC, filter, cacheable, external );
    assertEquals( channelSchema.getId(), id );
    assertEquals( channelSchema.getName(), name );
    assertEquals( channelSchema.toString(), name );
    assertEquals( channelSchema.isTypeChannel(), true );
    assertEquals( channelSchema.isInstanceChannel(), false );
    assertEquals( channelSchema.getFilterType(), ChannelSchema.FilterType.DYNAMIC );
    assertEquals( channelSchema.getFilter(), filter );
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
                         null,
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
                                             null,
                                             ValueUtil.randomBoolean(),
                                             ValueUtil.randomBoolean() ) );
    assertEquals( exception.getMessage(),
                  "Replicant-0045: ChannelSchema passed a name 'MyChannel' but Replicant.areNamesEnabled() is false" );
  }

  @Test
  public void constructorPassedNoFilterWhenExpected()
  {
    final IllegalStateException exception =
      expectThrows( IllegalStateException.class,
                    () -> new ChannelSchema( 222,
                                             "MyChannel",
                                             ValueUtil.randomBoolean(),
                                             ChannelSchema.FilterType.DYNAMIC,
                                             null,
                                             ValueUtil.randomBoolean(),
                                             ValueUtil.randomBoolean() ) );
    assertEquals( exception.getMessage(),
                  "Replicant-0076: ChannelSchema 222 has a DYNAMIC filterType but has supplied no filter." );
  }

  @Test
  public void constructorPassedFilterWhenNotExpected()
  {
    final IllegalStateException exception =
      expectThrows( IllegalStateException.class,
                    () -> new ChannelSchema( 222,
                                             "MyChannel",
                                             ValueUtil.randomBoolean(),
                                             ChannelSchema.FilterType.STATIC,
                                             mock( SubscriptionUpdateEntityFilter.class ),
                                             ValueUtil.randomBoolean(),
                                             ValueUtil.randomBoolean() ) );
    assertEquals( exception.getMessage(),
                  "Replicant-0077: ChannelSchema 222 does not have a DYNAMIC filterType but has supplied a filter." );
  }
}
