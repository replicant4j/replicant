package replicant;

import elemental2.core.Global;
import java.util.Objects;
import javax.annotation.Nonnull;
import jsinterop.base.Js;
import replicant.messages.ChangeSetMessage;
import replicant.messages.ServerToClientMessage;

/**
 * This is the class responsible for parsing change sets.
 * This class includes a "test" JVM implementation that will be ignored
 * when GWT compilation takes place. It is not yet handle all the varied
 * types in entities nor filters. Not suitable outside tests.
 */
final class MessageParser
{
  /**
   * The code to parse change sets. Extracted into a separate class so it can be vary by environment.
   */
  private static Parser c_parser = new JsParser();

  @Nonnull
  static ServerToClientMessage parseMessage( @Nonnull final String rawJsonData )
  {
    return c_parser.parseMessage( rawJsonData );
  }

  interface Parser
  {
    @Nonnull
    ServerToClientMessage parseMessage( @Nonnull String rawJsonData );
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
    public ServerToClientMessage parseMessage( @Nonnull final String rawJsonData )
    {
      return Js.cast( Global.JSON.parse( rawJsonData ) );
    }
  }

  @GwtIncompatible
  static final class ProxyParser
    implements Parser
  {
    private Parser _parser;

    void setParser( final Parser parser )
    {
      _parser = parser;
    }

    @SuppressWarnings( "unused" )
    @Nonnull
    public ServerToClientMessage parseMessage( @Nonnull final String rawJsonData )
    {
      if ( null == _parser )
      {
        throw new IllegalStateException( "Invoked parseMessage without setting up proxy" );
      }
      return _parser.parseMessage( rawJsonData );
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
