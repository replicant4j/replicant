package replicant;

import org.realityforge.guiceyloops.shared.ValueUtil;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class EntitySchemaTest
  extends AbstractReplicantTest
{
  @Test
  public void construct()
  {
    final EntitySchema.Creator<Object> creator = ( i, d ) -> 1;
    final EntitySchema.Updater<Object> updater = ( o, d ) -> d.notify();
    final EntitySchema entitySchema = new EntitySchema( 1, "MyObject", Object.class, creator, updater );
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
    final EntitySchema entitySchema =
      new EntitySchema( ValueUtil.randomInt(), null, Object.class, ( i, d ) -> 1, null );
    final IllegalStateException exception = expectThrows( IllegalStateException.class, entitySchema::getName );
    assertEquals( exception.getMessage(),
                  "Replicant-0050: EntitySchema.getName() invoked when Replicant.areNamesEnabled() is false" );
  }

  @Test
  public void toStringWhenNamesDisabled()
  {
    ReplicantTestUtil.disableNames();
    final EntitySchema entitySchema =
      new EntitySchema( ValueUtil.randomInt(), null, Object.class, ( i, d ) -> 1, null );
    assertEquals( entitySchema.toString(), "replicant.EntitySchema@" + Integer.toHexString( entitySchema.hashCode() ) );
  }

  @Test
  public void passNameToConstructorWhenNamesDisabled()
  {
    ReplicantTestUtil.disableNames();
    final IllegalStateException exception =
      expectThrows( IllegalStateException.class,
                    () -> new EntitySchema( ValueUtil.randomInt(), "MyEntity", Object.class, ( i, d ) -> 1, null ) );
    assertEquals( exception.getMessage(),
                  "Replicant-0049: EntitySchema passed a name 'MyEntity' but Replicant.areNamesEnabled() is false" );
  }
}
