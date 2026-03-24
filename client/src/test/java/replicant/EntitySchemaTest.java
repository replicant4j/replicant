package replicant;

import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class EntitySchemaTest
  extends AbstractReplicantTest
{
  @Test
  public void construct()
  {
    final var creator = (EntitySchema.Creator<Object>) ( i, d ) -> 1;
    final var updater = (EntitySchema.Updater<Object>) ( o, d ) -> d.notify();
    final var entitySchema =
      new EntitySchema( 1, "MyObject", Object.class, creator, updater, new ChannelLinkSchema[ 0 ] );
    assertEquals( entitySchema.getId(), 1 );
    assertEquals( entitySchema.getName(), "MyObject" );
    assertEquals( entitySchema.getType(), Object.class );
    assertEquals( entitySchema.getCreator(), creator );
    assertEquals( entitySchema.getUpdater(), updater );
    assertEquals( entitySchema.toString(), "MyObject" );
  }

  @Test
  public void getNameWhenNamesDisabled()
  {
    ReplicantTestUtil.disableNames();
    final var entitySchema =
      new EntitySchema( ValueUtil.randomInt(), null, Object.class, ( i, d ) -> 1, null, new ChannelLinkSchema[ 0 ] );
    final var exception = expectThrows( IllegalStateException.class, entitySchema::getName );
    assertEquals( exception.getMessage(),
                  "Replicant-0050: EntitySchema.getName() invoked when Replicant.areNamesEnabled() is false" );
  }

  @Test
  public void toStringWhenNamesDisabled()
  {
    ReplicantTestUtil.disableNames();
    final var entitySchema =
      new EntitySchema( ValueUtil.randomInt(), null, Object.class, ( i, d ) -> 1, null, new ChannelLinkSchema[ 0 ] );
    assertEquals( entitySchema.toString(), "replicant.EntitySchema@" + Integer.toHexString( entitySchema.hashCode() ) );
  }

  @Test
  public void passNameToConstructorWhenNamesDisabled()
  {
    ReplicantTestUtil.disableNames();
    final var exception =
      expectThrows( IllegalStateException.class,
                    () -> new EntitySchema( ValueUtil.randomInt(),
                                            "MyEntity",
                                            Object.class,
                                            ( i, d ) -> 1,
                                            null,
                                            new ChannelLinkSchema[ 0 ] ) );
    assertEquals( exception.getMessage(),
                  "Replicant-0049: EntitySchema passed a name 'MyEntity' but Replicant.areNamesEnabled() is false" );
  }
}
