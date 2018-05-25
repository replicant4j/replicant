package replicant;

import arez.Arez;
import arez.annotations.ArezComponent;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import static org.mockito.Mockito.*;

@SuppressWarnings( "SameParameterValue" )
@ArezComponent
public abstract class TestConnector
  extends Connector
{
  private final ChangeMapper _changeMapper = mock( ChangeMapper.class );

  public static TestConnector create()
  {
    return create( TestData.ROSE_SYSTEM );
  }

  public static TestConnector create( @Nonnull final SystemSchema schema )
  {
    return create( Replicant.areZonesEnabled() ? Replicant.context() : null, schema );
  }

  static TestConnector create( @Nullable final ReplicantContext context, @Nonnull final SystemSchema schema )
  {
    return Arez.context().safeAction( () -> new Arez_TestConnector( context, schema ) );
  }

  TestConnector( @Nullable final ReplicantContext context, @Nonnull final SystemSchema schema )
  {
    super( context, schema, mock( Transport.class ) );
  }

  @Nonnull
  @Override
  protected ChangeMapper getChangeMapper()
  {
    return _changeMapper;
  }
}
