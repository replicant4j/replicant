package org.realityforge.replicant.client.runtime;

import javax.annotation.Nonnull;

public final class RateLimitedValue
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

  public RateLimitedValue( final double tokensPerSecond )
  {
    this( tokensPerSecond, tokensPerSecond );
  }

  public RateLimitedValue( final double tokensPerSecond, final double maxTokenAmount )
  {
    setTokensPerSecond( tokensPerSecond );
    setMaxTokenCount( maxTokenAmount );
    setTokenCount( maxTokenAmount );
    _lastRegenTime = System.currentTimeMillis();
  }

  public synchronized void setTokensPerSecond( final double tokensPerSecond )
  {
    assert tokensPerSecond >= 0;
    _tokensPerSecond = Math.min( tokensPerSecond, MAX_POSSIBLE_TOKENS );
  }

  public double getTokensPerSecond()
  {
    return _tokensPerSecond;
  }

  public double getMaxTokenCount()
  {
    return _maxTokenCount;
  }

  public synchronized void fillBucket()
  {
    _tokenCount = _maxTokenCount;
  }

  public synchronized double getTokenCount()
  {
    return _tokenCount;
  }

  public synchronized boolean isBucketFull()
  {
    regenerateTokens();
    return _tokenCount == _maxTokenCount;
  }

  public void setMaxTokenCount( final double maxTokenCount )
  {
    assert maxTokenCount >= 0;
    _maxTokenCount = maxTokenCount;
    _tokenCount = Math.min( _tokenCount, _maxTokenCount );
  }

  public void setTokenCount( final double tokenCount )
  {
    assert tokenCount >= 0;
    _tokenCount = Math.min( tokenCount, _maxTokenCount );
  }

  public synchronized boolean consume( final double costInTokens )
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

  public boolean attempt( final double costInTokens, @Nonnull final Runnable action )
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
    final long now = System.currentTimeMillis();
    final long duration = now - _lastRegenTime;
    if ( duration > 0 )
    {
      final double newTokenCount = _tokenCount + ( duration * _tokensPerSecond / MILLIS_PER_SECOND );
      _tokenCount = Math.min( _maxTokenCount, newTokenCount );
      _lastRegenTime = now;
    }
  }
}
