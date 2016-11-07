package org.realityforge.replicant.server;

import javax.annotation.Nonnull;
import javax.json.stream.JsonGenerator;
import org.realityforge.replicant.server.transport.ReplicantSession;
import org.realityforge.rest.field_filter.FieldFilter;

public class TestSession
  extends ReplicantSession
{
  public TestSession( @Nonnull final String sessionID )
  {
    super( sessionID );
  }

  @Override
  public void emitStatus( @Nonnull final JsonGenerator g, @Nonnull final FieldFilter filter )
  {
  }
}
