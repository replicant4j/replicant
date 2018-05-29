package replicant;

import arez.Arez;
import arez.annotations.ArezComponent;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.realityforge.guiceyloops.shared.ValueUtil;
import static org.mockito.Mockito.*;

@SuppressWarnings( "SameParameterValue" )
@ArezComponent
public abstract class TestConnector
  extends Connector
{
  public static TestConnector create()
  {
    return create( new SystemSchema( 1,
                                     ValueUtil.randomString(),
                                     new ChannelSchema[ 0 ],
                                     new EntitySchema[ 0 ] ) );
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
}
