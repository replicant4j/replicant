package org.realityforge.replicant.shared.ee.rest;

import java.util.HashMap;
import javax.annotation.Nonnull;
import javax.json.Json;
import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonGeneratorFactory;
import javax.ws.rs.core.Response;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;

/**
 * A base class for several of the rest services within the replicant project.
 */
public abstract class AbstractReplicantRestService
{
  private DatatypeFactory _datatypeFactory;
  private JsonGeneratorFactory _factory;

  public void postConstruct()
  {
    final HashMap<String, Object> config = new HashMap<>();
    config.put( JsonGenerator.PRETTY_PRINTING, true );
    _factory = Json.createGeneratorFactory( config );

    try
    {
      _datatypeFactory = DatatypeFactory.newInstance();
    }
    catch ( final DatatypeConfigurationException dtce )
    {
      throw new IllegalStateException( "Unable to initialize DatatypeFactory", dtce );
    }
  }

  @Nonnull
  protected Response buildResponse( @Nonnull final Response.ResponseBuilder builder,
                                    @Nonnull final String content )
  {
    CacheUtil.configureNoCacheHeaders( builder );
    return builder.entity( content ).build();
  }

  @Nonnull
  protected JsonGeneratorFactory factory()
  {
    return _factory;
  }

  @Nonnull
  protected DatatypeFactory datatypeFactory()
  {
    return _datatypeFactory;
  }
}
