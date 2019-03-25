package replicant;

import elemental2.core.Global;
import java.util.Objects;
import javax.annotation.Nonnull;
import jsinterop.base.Js;
import replicant.messages.ChangeSet;

/**
 * This is the class responsible for parsing change sets.
 * This class includes a "test" JVM implementation that will be ignored
 * when GWT compilation takes place. It is not yet handle all the varied
 * types in entities nor filters. Not suitable outside tests.
 */
final class ChangeSetParser
{
  /**
   * The code to parse change sets. Extracted into a separate class so it can be vary by environment.
   */
  private static Parser c_parser = ReplicantConfig.isProductionMode() ? new JsParser() : new ProxyParser();

  @Nonnull
  static ChangeSet parseChangeSet( @Nonnull final String rawJsonData )
  {
    return c_parser.parseChangeSet( rawJsonData );
  }

  interface Parser
  {
    @Nonnull
    ChangeSet parseChangeSet( @Nonnull String rawJsonData );
  }

  /**
   * This is the class responsible for parsing change sets.
   * This is split into separate class so that can be swapped during testing.
   */
  static class JsParser
    implements Parser
  {
    @SuppressWarnings( "unused" )
    @Nonnull
    public ChangeSet parseChangeSet( @Nonnull final String rawJsonData )
    {
      return Js.cast( Global.JSON.parse( rawJsonData ) );
    }
  }

  @GwtIncompatible
  static final class ProxyParser
    implements Parser
  {
    private Parser _parser;

    Parser getParser()
    {
      return _parser;
    }

    void setParser( final Parser parser )
    {
      _parser = parser;
    }

    @SuppressWarnings( "unused" )
    @Nonnull
    public ChangeSet parseChangeSet( @Nonnull final String rawJsonData )
    {
      if ( null == _parser )
      {
        throw new IllegalStateException( "Invoked parseChangeSet without setting up proxy" );
      }
      return _parser.parseChangeSet( rawJsonData );
    }
  }

  @GwtIncompatible
  static void setParser( @Nonnull final Parser parser )
  {
    c_parser = Objects.requireNonNull( parser );
  }

  @GwtIncompatible
  static Parser getParser()
  {
    return c_parser;
  }
}
