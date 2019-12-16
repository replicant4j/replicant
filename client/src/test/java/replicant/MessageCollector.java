package replicant;

import java.io.File;
import javax.annotation.Nonnull;
import org.realityforge.braincheck.AbstractTestNGMessageCollector;
import org.realityforge.braincheck.GuardMessageCollector;
import static org.testng.Assert.*;

public final class MessageCollector
  extends AbstractTestNGMessageCollector
{
  @Override
  protected boolean shouldCheckDiagnosticMessages()
  {
    return System.getProperty( "replicant.check_diagnostic_messages", "true" ).equals( "true" );
  }

  @Nonnull
  @Override
  protected GuardMessageCollector createCollector()
  {
    final boolean saveIfChanged = "true".equals( System.getProperty( "replicant.output_fixture_data", "false" ) );
    final String fixtureDir = System.getProperty( "replicant.diagnostic_messages_file" );
    assertNotNull( fixtureDir,
                   "Expected System.getProperty( \"replicant.diagnostic_messages_file\" ) to return location of diagnostic messages file" );
    return new GuardMessageCollector( "Replicant", new File( fixtureDir ), saveIfChanged, true, false );
  }
}
