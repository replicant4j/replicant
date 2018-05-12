package replicant;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;

class RateLimitedValue
{
  private static final int MILLIS_PER_SECOND = 1000;
  private static final double MAX_POSSIBLE_TOKENS = Double.MAX_VALUE / 4;

  //Target rate in messages/sec
  private double _tokensPerSecond;
  // Last time the token count was regenerated
  private long _lastRegenTime;
  // The maximum number of tokens in the bucket
  private double _maxTokenCount;
  // The number of tokens left in the bucket
  private double _tokenCount;

  RateLimitedValue( @Nonnegative final double tokensPerSecond )
  {
    this( tokensPerSecond, tokensPerSecond );
  }

  RateLimitedValue( @Nonnegative final double tokensPerSecond, @Nonnegative final double maxTokenAmount )
  {
    setTokensPerSecond( tokensPerSecond );
    setMaxTokenCount( maxTokenAmount );
    setTokenCount( maxTokenAmount );
    _lastRegenTime = currentTimeMillis();
  }

  synchronized void setTokensPerSecond( @Nonnegative final double tokensPerSecond )
  {
    assert tokensPerSecond >= 0;
    _tokensPerSecond = Math.min( tokensPerSecond, MAX_POSSIBLE_TOKENS );
  }

  double getTokensPerSecond()
  {
    return _tokensPerSecond;
  }

  double getMaxTokenCount()
  {
    return _maxTokenCount;
  }

  synchronized void fillBucket()
  {
    _tokenCount = _maxTokenCount;
  }

  synchronized double getTokenCount()
  {
    return _tokenCount;
  }

  synchronized boolean isBucketFull()
  {
    regenerateTokens();
    return _tokenCount == _maxTokenCount;
  }

  void setMaxTokenCount( @Nonnegative final double maxTokenCount )
  {
    assert maxTokenCount >= 0;
    _maxTokenCount = maxTokenCount;
    _tokenCount = Math.min( _tokenCount, _maxTokenCount );
  }

  void setTokenCount( @Nonnegative final double tokenCount )
  {
    assert tokenCount >= 0;
    _tokenCount = Math.min( tokenCount, _maxTokenCount );
  }

  synchronized boolean consume( @Nonnegative final double costInTokens )
  {
    regenerateTokens();
    if ( _tokenCount >= costInTokens )
    {
      _tokenCount -= costInTokens;
      return true;
    }
    else
    {
      return false;
    }
  }

  boolean attempt( @Nonnegative final double costInTokens, @Nonnull final Runnable action )
  {
    if ( consume( costInTokens ) )
    {
      action.run();
      return true;
    }
    else
    {
      return false;
    }
  }

  /**
   * Regenerate tokens available.
   */
  final void regenerateTokens()
  {
    final long now = currentTimeMillis();
    final long duration = now - _lastRegenTime;
    if ( duration > 0 )
    {
      final double newTokenCount = _tokenCount + ( duration * _tokensPerSecond / MILLIS_PER_SECOND );
      _tokenCount = Math.min( _maxTokenCount, newTokenCount );
      _lastRegenTime = now;
    }
  }

  long currentTimeMillis()
  {
    return System.currentTimeMillis();
  }
}
