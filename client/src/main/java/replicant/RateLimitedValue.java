package replicant;

import javax.annotation.Nonnull;

final class RateLimitedValue
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

  RateLimitedValue( final long now, final double tokensPerSecond, final double maxTokenAmount )
  {
    setTokensPerSecond( tokensPerSecond );
    setMaxTokenCount( maxTokenAmount );
    setTokenCount( maxTokenAmount );
    _lastRegenTime = now;
  }

  void setTokensPerSecond( final double tokensPerSecond )
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

  void fillBucket()
  {
    _tokenCount = _maxTokenCount;
  }

  double getTokenCount()
  {
    return _tokenCount;
  }

  void setMaxTokenCount( final double maxTokenCount )
  {
    assert maxTokenCount >= 0;
    _maxTokenCount = maxTokenCount;
    _tokenCount = Math.min( _tokenCount, _maxTokenCount );
  }

  void setTokenCount( final double tokenCount )
  {
    assert tokenCount >= 0;
    _tokenCount = Math.min( tokenCount, _maxTokenCount );
  }

  boolean consume( final long now, final double costInTokens )
  {
    regenerateTokens( now );
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

  boolean attempt( final long now, final double costInTokens, @Nonnull final Runnable action )
  {
    if ( consume( now, costInTokens ) )
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
  void regenerateTokens( final long now )
  {
    final long duration = now - _lastRegenTime;
    if ( duration > 0 )
    {
      final double newTokenCount = _tokenCount + ( duration * _tokensPerSecond / MILLIS_PER_SECOND );
      _tokenCount = Math.min( _maxTokenCount, newTokenCount );
      _lastRegenTime = now;
    }
  }
}
