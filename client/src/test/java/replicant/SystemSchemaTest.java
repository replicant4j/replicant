package replicant;

import java.util.Collections;
import org.realityforge.guiceyloops.shared.ValueUtil;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class SystemSchemaTest
  extends AbstractReplicantTest
{
  @Test
  public void basicOperation()
  {
    final int id = ValueUtil.randomInt();
    final String name = ValueUtil.randomString();
    final EntitySchema entity1 =
      new EntitySchema( 0, ValueUtil.randomString(), Integer.class, ( i, d ) -> 1, null, new ChannelLinkSchema[ 0 ] );
    final EntitySchema entity2 =
      new EntitySchema( 1, ValueUtil.randomString(), String.class, ( i, d ) -> "", null, new ChannelLinkSchema[ 0 ] );
    final EntitySchema[] entities = new EntitySchema[]{ entity1, entity2 };
    final ChannelSchema channel1 = new ChannelSchema( 0,
                                                      ValueUtil.randomString(),
                                                      null,
                                                      ChannelSchema.FilterType.NONE,
                                                      null,
                                                      false,
                                                      true,
                                                      Collections.emptyList() );
    final ChannelSchema[] channels = { channel1 };
    final SystemSchema systemSchema = new SystemSchema( id, name, channels, entities );
    assertEquals( systemSchema.getId(), id );
    assertEquals( systemSchema.getName(), name );
    assertEquals( systemSchema.getEntityCount(), 2 );
    assertEquals( systemSchema.getEntity( 0 ), entity1 );
    assertEquals( systemSchema.getEntity( 1 ), entity2 );
    assertEquals( systemSchema.getChannelCount(), 1 );
    assertEquals( systemSchema.getChannel( 0 ), channel1 );
    assertEquals( systemSchema.toString(), name );
  }

  @Test
  public void getChannel_BadIndex()
  {
    final SystemSchema systemSchema =
      new SystemSchema( ValueUtil.randomInt(), ValueUtil.randomString(), new ChannelSchema[]{}, new EntitySchema[]{} );
    final IllegalStateException exception =
      expectThrows( IllegalStateException.class, () -> systemSchema.getChannel( 2 ) );
    assertEquals( exception.getMessage(),
                  "Replicant-0058: SystemSchema.getChannel(id) passed an id that is out of range." );
  }

  @Test
  public void getEntity_BadIndex()
  {
    final SystemSchema systemSchema =
      new SystemSchema( ValueUtil.randomInt(), ValueUtil.randomString(), new ChannelSchema[]{}, new EntitySchema[]{} );
    final IllegalStateException exception =
      expectThrows( IllegalStateException.class, () -> systemSchema.getEntity( 2 ) );
    assertEquals( exception.getMessage(),
                  "Replicant-0057: SystemSchema.getEntity(id) passed an id that is out of range." );
  }

  @Test
  public void construct_nullEntity()
  {
    final IllegalStateException exception =
      expectThrows( IllegalStateException.class,
                    () -> new SystemSchema( ValueUtil.randomInt(),
                                            "X",
                                            new ChannelSchema[]{},
                                            new EntitySchema[]{ null } ) );
    assertEquals( exception.getMessage(),
                  "Replicant-0053: SystemSchema named 'X' passed an array of entities that has a null element" );
  }

  @Test
  public void construct_nullChannel()
  {
    final IllegalStateException exception =
      expectThrows( IllegalStateException.class,
                    () -> new SystemSchema( ValueUtil.randomInt(),
                                            "X",
                                            new ChannelSchema[]{ null },
                                            new EntitySchema[]{} ) );
    assertEquals( exception.getMessage(),
                  "Replicant-0055: SystemSchema named 'X' passed an array of channels that has a null element" );
  }

  @Test
  public void construct_badEntityIndex()
  {
    final EntitySchema entity1 =
      new EntitySchema( 23, ValueUtil.randomString(), Integer.class, ( i, d ) -> 1, null, new ChannelLinkSchema[ 0 ] );
    final IllegalStateException exception =
      expectThrows( IllegalStateException.class,
                    () -> new SystemSchema( ValueUtil.randomInt(), "X",
                                            new ChannelSchema[]{},
                                            new EntitySchema[]{ entity1 } ) );
    assertEquals( exception.getMessage(),
                  "Replicant-0054: SystemSchema named 'X' passed an array of entities where entity at index 0 does not have id matching index." );
  }

  @Test
  public void construct_badChannelIndex()
  {
    final ChannelSchema channel1 = new ChannelSchema( 234,
                                                      ValueUtil.randomString(),
                                                      null,
                                                      ChannelSchema.FilterType.NONE,
                                                      null,
                                                      false,
                                                      true,
                                                      Collections.emptyList() );
    final IllegalStateException exception =
      expectThrows( IllegalStateException.class,
                    () -> new SystemSchema( ValueUtil.randomInt(),
                                            "X",
                                            new ChannelSchema[]{ channel1 },
                                            new EntitySchema[]{} ) );
    assertEquals( exception.getMessage(),
                  "Replicant-0056: SystemSchema named 'X' passed an array of channels where channel at index 0 does not have id matching index." );
  }

  @Test
  public void getNameWhenNamesDisabled()
  {
    ReplicantTestUtil.disableNames();
    final SystemSchema systemSchema =
      new SystemSchema( ValueUtil.randomInt(), null, new ChannelSchema[ 0 ], new EntitySchema[ 0 ] );
    final IllegalStateException exception = expectThrows( IllegalStateException.class, systemSchema::getName );
    assertEquals( exception.getMessage(),
                  "Replicant-0052: SystemSchema.getName() invoked when Replicant.areNamesEnabled() is false" );
  }

  @Test
  public void toStringWhenNamesDisabled()
  {
    ReplicantTestUtil.disableNames();
    final SystemSchema systemSchema =
      new SystemSchema( ValueUtil.randomInt(), null, new ChannelSchema[ 0 ], new EntitySchema[ 0 ] );
    assertEquals( systemSchema.toString(), "replicant.SystemSchema@" + Integer.toHexString( systemSchema.hashCode() ) );
  }

  @Test
  public void passNameToConstructorWhenNamesDisabled()
  {
    ReplicantTestUtil.disableNames();
    final IllegalStateException exception =
      expectThrows( IllegalStateException.class,
                    () -> new SystemSchema( ValueUtil.randomInt(),
                                            "MySystem",
                                            new ChannelSchema[ 0 ],
                                            new EntitySchema[ 0 ] ) );
    assertEquals( exception.getMessage(),
                  "Replicant-0051: SystemSchema passed a name 'MySystem' but Replicant.areNamesEnabled() is false" );
  }
}
