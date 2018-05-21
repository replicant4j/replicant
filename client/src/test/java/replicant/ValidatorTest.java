package replicant;

import javax.annotation.Nullable;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class ValidatorTest
  extends AbstractReplicantTest
{
  @Test
  public void noEntities()
  {
    Validator.create( null ).validateEntities();
  }

  @Test
  public void entitiesAllValid()
  {
    final EntityService entityService = Replicant.context().getEntityService();
    final Entity entity1 = safeAction( () -> entityService.findOrCreateEntity( "MyEntity/1", MyEntity.class, 1 ) );
    safeAction( () -> entity1.setUserObject( new MyEntity( null ) ) );
    safeAction( () -> entityService.findOrCreateEntity( "MyEntity/2", MyEntity.class, 2 ) );

    // Entities fine
    Validator.create( null ).validateEntities();
  }

  @Test
  public void invalidEntity()
  {
    final EntityService entityService = Replicant.context().getEntityService();
    final Entity entity1 = safeAction( () -> entityService.findOrCreateEntity( "MyEntity/1", MyEntity.class, 1 ) );
    final Exception error = new Exception();
    safeAction( () -> entity1.setUserObject( new MyEntity( error ) ) );

    final IllegalStateException exception =
      expectThrows( IllegalStateException.class, () -> Validator.create( null ).validateEntities() );

    assertEquals( exception.getMessage(),
                  "Replicant-0065: Entity failed to verify during validation process. Entity = MyEntity/1" );
  }

  static class MyEntity
    implements Verifiable
  {
    @Nullable
    private final Exception _exception;

    MyEntity( @Nullable final Exception exception )
    {
      _exception = exception;
    }

    @Override
    public void verify()
      throws Exception
    {
      if ( null != _exception )
      {
        throw _exception;
      }
    }
  }
}
