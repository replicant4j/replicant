package replicant;

import javax.annotation.Nonnull;

final class ClassMetaDataCheck
{
  @Nonnull
  private static final Checker CHECKER = new Checker();

  private ClassMetaDataCheck()
  {
  }

  static boolean isClassMetadataEnabled()
  {
    return CHECKER.isClassMetadataEnabled();
  }

  private static class Checker
    extends AbstractChecker
  {
    @GwtIncompatible
    @Override
    boolean isClassMetadataEnabled()
    {
      return true;
    }
  }

  private static abstract class AbstractChecker
  {
    native boolean isClassMetadataEnabled() /*-{
      return @Class::isClassMetadataEnabled()();
    }-*/;
  }
}
