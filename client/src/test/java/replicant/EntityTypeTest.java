package replicant;

import org.realityforge.guiceyloops.shared.ValueUtil;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class EntityTypeTest
  extends AbstractReplicantTest
{
  @Test
  public void construct()
  {
    final EntityType entityType = new EntityType( 1, "MyObject", Object.class );
    assertEquals( entityType.getId(), 1 );
    assertEquals( entityType.getName(), "MyObject" );
    assertEquals( entityType.getType(), Object.class );
    assertEquals( entityType.toString(), "MyObject" );
  }

  @Test
  public void getNameWhenNamesDisabled()
  {
    ReplicantTestUtil.disableNames();
    final EntityType entityType = new EntityType( ValueUtil.randomInt(), null, Object.class );
    final IllegalStateException exception = expectThrows( IllegalStateException.class, entityType::getName );
    assertEquals( exception.getMessage(),
                  "Replicant-0050: EntityType.getName() invoked when Replicant.areNamesEnabled() is false" );
  }

  @Test
  public void toStringWhenNamesDisabled()
  {
    ReplicantTestUtil.disableNames();
    final EntityType entityType = new EntityType( ValueUtil.randomInt(), null, Object.class );
    assertEquals( entityType.toString(), "replicant.EntityType@" + Integer.toHexString( entityType.hashCode() ) );
  }

  @Test
  public void passNameToConstructorWhenNamesDisabled()
  {
    ReplicantTestUtil.disableNames();
    final IllegalStateException exception =
      expectThrows( IllegalStateException.class,
                    () -> new EntityType( ValueUtil.randomInt(), "MyEntity", Object.class ) );
    assertEquals( exception.getMessage(),
                  "Replicant-0049: EntityType passed a name 'MyEntity' but Replicant.areNamesEnabled() is false" );
  }
}
