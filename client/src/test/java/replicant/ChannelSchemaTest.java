package replicant;

import java.util.Collections;
import org.realityforge.guiceyloops.shared.ValueUtil;
import org.testng.annotations.Test;
import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

public class ChannelSchemaTest
  extends AbstractReplicantTest
{
  @Test
  public void findEntityById()
  {
    final EntitySchema entity =
      new EntitySchema( 1, "MyObject", Object.class, ( i, d ) -> 1, ( o, d ) -> d.notify() );
    final ChannelSchema channelSchema =
      new ChannelSchema( ValueUtil.randomInt(),
                         ValueUtil.randomString(),
                         null,
                         ChannelSchema.FilterType.NONE,
                         null,
                         false,
                         false,
                         Collections.singletonList( entity ) );
    assertEquals( channelSchema.getEntities().size(), 1 );
    assertEquals( channelSchema.getEntities().size(), 1 );
    assertEquals( channelSchema.findEntityById( 1 ), entity );
    assertNull( channelSchema.findEntityById( 0 ) );
  }

  @Test
  public void typeGraph()
  {
    final EntitySchema entity =
      new EntitySchema( 1, "MyObject", Object.class, ( i, d ) -> 1, ( o, d ) -> d.notify() );
    final ChannelSchema channelSchema =
      new ChannelSchema( 1,
                         "MetaData",
                         null,
                         ChannelSchema.FilterType.NONE,
                         null,
                         false,
                         false,
                         Collections.singletonList( entity ) );
    assertEquals( channelSchema.getId(), 1 );
    assertEquals( channelSchema.getName(), "MetaData" );
    assertEquals( channelSchema.toString(), "MetaData" );
    assertTrue( channelSchema.isTypeChannel() );
    assertFalse( channelSchema.isInstanceChannel() );
    assertNull( channelSchema.getInstanceType() );
    assertEquals( channelSchema.getFilterType(), ChannelSchema.FilterType.NONE );
    assertFalse( channelSchema.isCacheable() );
    assertFalse( channelSchema.isExternal() );
    assertEquals( channelSchema.getEntities().size(), 1 );
    assertTrue( channelSchema.getEntities().contains( entity ) );
  }

  @Test
  public void instanceGraph()
  {
    final ChannelSchema channelSchema =
      new ChannelSchema( 1,
                         "MetaData",
                         String.class,
                         ChannelSchema.FilterType.NONE,
                         null,
                         false,
                         true,
                         Collections.emptyList() );
    assertEquals( channelSchema.getId(), 1 );
    assertEquals( channelSchema.getName(), "MetaData" );
    assertEquals( channelSchema.toString(), "MetaData" );
    assertFalse( channelSchema.isTypeChannel() );
    assertTrue( channelSchema.isInstanceChannel() );
    assertEquals( channelSchema.getInstanceType(), String.class );
    assertEquals( channelSchema.getFilterType(), ChannelSchema.FilterType.NONE );
    assertFalse( channelSchema.isCacheable() );
    assertTrue( channelSchema.isExternal() );
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
      new ChannelSchema( id,
                         name,
                         String.class,
                         ChannelSchema.FilterType.STATIC,
                         null,
                         cacheable,
                         external,
                         Collections.emptyList() );
    assertEquals( channelSchema.getId(), id );
    assertEquals( channelSchema.getName(), name );
    assertEquals( channelSchema.toString(), name );
    assertEquals( channelSchema.isTypeChannel(), typeChannel );
    assertTrue( channelSchema.isInstanceChannel() );
    assertEquals( channelSchema.getInstanceType(), String.class );
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
    final SubscriptionUpdateEntityFilter<?> filter = mock( SubscriptionUpdateEntityFilter.class );
    final ChannelSchema channelSchema =
      new ChannelSchema( id,
                         name,
                         null,
                         ChannelSchema.FilterType.DYNAMIC,
                         filter,
                         cacheable,
                         external,
                         Collections.emptyList() );
    assertEquals( channelSchema.getId(), id );
    assertEquals( channelSchema.getName(), name );
    assertEquals( channelSchema.toString(), name );
    assertTrue( channelSchema.isTypeChannel() );
    assertFalse( channelSchema.isInstanceChannel() );
    assertNull( channelSchema.getInstanceType() );
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
                         null,
                         ChannelSchema.FilterType.NONE,
                         null,
                         ValueUtil.randomBoolean(),
                         ValueUtil.randomBoolean(),
                         Collections.emptyList() );
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
                                             null,
                                             ChannelSchema.FilterType.NONE,
                                             null,
                                             ValueUtil.randomBoolean(),
                                             ValueUtil.randomBoolean(),
                                             Collections.emptyList() ) );
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
                                             null,
                                             ChannelSchema.FilterType.DYNAMIC,
                                             null,
                                             ValueUtil.randomBoolean(),
                                             ValueUtil.randomBoolean(),
                                             Collections.emptyList() ) );
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
                                             null,
                                             ChannelSchema.FilterType.STATIC,
                                             mock( SubscriptionUpdateEntityFilter.class ),
                                             ValueUtil.randomBoolean(),
                                             ValueUtil.randomBoolean(),
                                             Collections.emptyList() ) );
    assertEquals( exception.getMessage(),
                  "Replicant-0077: ChannelSchema 222 does not have a DYNAMIC filterType but has supplied a filter." );
  }
}
