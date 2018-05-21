package replicant;

import elemental2.core.Global;
import javax.annotation.Nonnull;
import jsinterop.base.Js;

/**
 * This is the class responsible for parsing change sets.
 * This is split into two classes so that gwt implementation is in base class and the JVM
 * implementation is in subclass but marked as GwtIncompatible so they are elided during compile.
 */
abstract class AbstractChangeSetParser
{
  @Nonnull
  ChangeSet parseChangeSet( @Nonnull final String rawJsonData )
  {
    return Js.cast( Global.JSON.parse( rawJsonData ) );
  }
}
