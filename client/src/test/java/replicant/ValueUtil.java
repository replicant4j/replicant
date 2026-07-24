package replicant;

import java.util.Random;
import org.jspecify.annotations.NonNull;

public final class ValueUtil
{
  @NonNull
  private static final Random c_random = new Random();

  private ValueUtil()
  {
  }

  @NonNull
  public static Random getRandom()
  {
    return c_random;
  }

  public static boolean randomBoolean()
  {
    return c_random.nextBoolean();
  }

  public static int randomInt()
  {
    return c_random.nextInt();
  }

  @NonNull
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
