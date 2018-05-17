package replicant;

import org.realityforge.guiceyloops.shared.ValueUtil;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class SystemTypeTest
  extends AbstractReplicantTest
{
  @Test
  public void basicOperation()
  {
    final String name = ValueUtil.randomString();
    final EntityType entity1 = new EntityType( 0, ValueUtil.randomString(), Integer.class );
    final EntityType entity2 = new EntityType( 1, ValueUtil.randomString(), String.class );
    final EntityType[] entities = new EntityType[]{ entity1, entity2 };
    final ChannelType channel1 = new ChannelType( 0,
                                                  ValueUtil.randomString(),
                                                  ValueUtil.randomBoolean(),
                                                  ChannelType.FilterType.NONE,
                                                  false,
                                                  true );
    final ChannelType[] channels = { channel1 };
    final SystemType systemType = new SystemType( name, channels, entities );
    assertEquals( systemType.getName(), name );
    assertEquals( systemType.getEntityCount(), 2 );
    assertEquals( systemType.getEntity( 0 ), entity1 );
    assertEquals( systemType.getEntity( 1 ), entity2 );
    assertEquals( systemType.getChannelCount(), 1 );
    assertEquals( systemType.getChannel( 0 ), channel1 );
    assertEquals( systemType.toString(), name );
  }

  @Test
  public void getChannel_BadIndex()
  {
    final SystemType systemType = new SystemType( ValueUtil.randomString(), new ChannelType[]{}, new EntityType[]{} );
    final IllegalStateException exception =
      expectThrows( IllegalStateException.class, () -> systemType.getChannel( 2 ) );
    assertEquals( exception.getMessage(),
                  "Replicant-0058: SystemType.getChannel(id) passed an id that is out of range." );
  }

  @Test
  public void getEntity_BadIndex()
  {
    final SystemType systemType = new SystemType( ValueUtil.randomString(), new ChannelType[]{}, new EntityType[]{} );
    final IllegalStateException exception =
      expectThrows( IllegalStateException.class, () -> systemType.getEntity( 2 ) );
    assertEquals( exception.getMessage(),
                  "Replicant-0057: SystemType.getEntity(id) passed an id that is out of range." );
  }

  @Test
  public void construct_nullEntity()
  {
    final IllegalStateException exception =
      expectThrows( IllegalStateException.class,
                    () -> new SystemType( "X", new ChannelType[]{}, new EntityType[]{ null } ) );
    assertEquals( exception.getMessage(),
                  "Replicant-0053: SystemType named 'X' passed an array of entities that has a null element" );
  }

  @Test
  public void construct_nullChannel()
  {
    final IllegalStateException exception =
      expectThrows( IllegalStateException.class,
                    () -> new SystemType( "X", new ChannelType[]{ null }, new EntityType[]{} ) );
    assertEquals( exception.getMessage(),
                  "Replicant-0055: SystemType named 'X' passed an array of channels that has a null element" );
  }

  @Test
  public void construct_badEntityIndex()
  {
    final EntityType entity1 = new EntityType( 23, ValueUtil.randomString(), Integer.class );
    final IllegalStateException exception =
      expectThrows( IllegalStateException.class,
                    () -> new SystemType( "X",
                                          new ChannelType[]{},
                                          new EntityType[]{ entity1 } ) );
    assertEquals( exception.getMessage(),
                  "Replicant-0054: SystemType named 'X' passed an array of entities where entity at index 0 does not have id matching index." );
  }

  @Test
  public void construct_badChannelIndex()
  {
    final ChannelType channel1 = new ChannelType( 234,
                                                  ValueUtil.randomString(),
                                                  ValueUtil.randomBoolean(),
                                                  ChannelType.FilterType.NONE,
                                                  false,
                                                  true );
    final IllegalStateException exception =
      expectThrows( IllegalStateException.class,
                    () -> new SystemType( "X",
                                          new ChannelType[]{ channel1 },
                                          new EntityType[]{} ) );
    assertEquals( exception.getMessage(),
                  "Replicant-0056: SystemType named 'X' passed an array of channels where channel at index 0 does not have id matching index." );
  }

  @Test
  public void getNameWhenNamesDisabled()
  {
    ReplicantTestUtil.disableNames();
    final SystemType systemType = new SystemType( null, new ChannelType[ 0 ], new EntityType[ 0 ] );
    final IllegalStateException exception = expectThrows( IllegalStateException.class, systemType::getName );
    assertEquals( exception.getMessage(),
                  "Replicant-0052: SystemType.getName() invoked when Replicant.areNamesEnabled() is false" );
  }

  @Test
  public void toStringWhenNamesDisabled()
  {
    ReplicantTestUtil.disableNames();
    final SystemType systemType = new SystemType( null, new ChannelType[ 0 ], new EntityType[ 0 ] );
    assertEquals( systemType.toString(), "replicant.SystemType@" + Integer.toHexString( systemType.hashCode() ) );
  }

  @Test
  public void passNameToConstructorWhenNamesDisabled()
  {
    ReplicantTestUtil.disableNames();
    final IllegalStateException exception =
      expectThrows( IllegalStateException.class,
                    () -> new SystemType( "MySystem", new ChannelType[ 0 ], new EntityType[ 0 ] ) );
    assertEquals( exception.getMessage(),
                  "Replicant-0051: SystemType passed a name 'MySystem' but Replicant.areNamesEnabled() is false" );
  }
}
