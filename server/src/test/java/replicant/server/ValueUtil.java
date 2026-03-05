package replicant.server;

import java.util.Random;
import javax.annotation.Nonnull;

public final class ValueUtil
{
  @Nonnull
  private static final Random c_random = new Random();

  private ValueUtil()
  {
  }

  public static int randomInt()
  {
    return c_random.nextInt();
  }

  @Nonnull
  public static String randomString()
  {
    final StringBuilder sb = new StringBuilder();
    for ( int i = 0; i < 50; i++ )
    {
      sb.append( (char) ( 'a' + c_random.nextInt( 26 ) ) );
    }
    return sb.toString();
  }
}
