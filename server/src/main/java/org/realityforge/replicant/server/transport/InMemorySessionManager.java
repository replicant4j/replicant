package org.realityforge.replicant.server.transport;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * An extremely simple session manager that uses in memory session management.
 */
public abstract class InMemorySessionManager<T extends SessionInfo>
  implements SessionManager<T>
{
  private final ReadWriteLock _lock = new ReentrantReadWriteLock();
  private final Map<String, T> _sessions = new HashMap<>();
  private final Map<String, T> _roSessions = Collections.unmodifiableMap( _sessions );

  /**
   * {@inheritDoc}
   */
  @Override
  @Nonnull
  public String getSessionKey()
  {
    return "sid";
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean invalidateSession( @Nonnull final String sessionID )
  {
    return null != removeSession( sessionID );
  }

  /**
   * Remove session with specified id.
   *
   * @param sessionID the session id.
   * @return the session removed if any.
   */
  protected T removeSession( final String sessionID )
  {
    _lock.writeLock().lock();
    try
    {
      return _sessions.remove( sessionID );
    }
    finally
    {
      _lock.writeLock().unlock();
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @Nullable
  public T getSession( @Nonnull final String sessionID )
  {
    _lock.readLock().lock();
    final T sessionInfo;
    try
    {
      sessionInfo = _sessions.get( sessionID );
    }
    finally
    {
      _lock.readLock().unlock();
    }
    if ( null != sessionInfo )
    {
      sessionInfo.updateAccessTime();
    }
    return sessionInfo;
  }

  @Nonnull
  @Override
  public Set<String> getSessionIDs()
  {
    _lock.readLock().lock();
    try
    {
      return new HashSet<>( _sessions.keySet() );
    }
    finally
    {
      _lock.readLock().unlock();
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @Nonnull
  public T createSession()
  {
    final T sessionInfo = newSessionInfo();
    _lock.writeLock().lock();
    try
    {
      _sessions.put( sessionInfo.getSessionID(), sessionInfo );
    }
    finally
    {
      _lock.writeLock().unlock();
    }
    return sessionInfo;
  }

  /**
   * Return an unmodifiable map containing the set of sessions.
   * The user should also acquire a read lock via {@link #getLock()} prior to invoking
   * this method ensure it is not modified while being inspected.
   *
   * @return an unmodifiable map containing the set of sessions.
   */
  @Nonnull
  protected Map<String, T> getSessions()
  {
    return _roSessions;
  }

  /**
   * @return the lock used to guard access to sessions map.
   */
  @Nonnull
  protected ReadWriteLock getLock()
  {
    return _lock;
  }

  /**
   * Override method to create a new session.
   *
   * @return the new session.
   */
  @Nonnull
  protected abstract T newSessionInfo();

  /**
   * Remove sessions that have not been accessed for the specified idle time.
   *
   * @param maxIdleTime the max idle time for a session.
   * @return the number of sessions removed.
   */
  protected int removeIdleSessions( final long maxIdleTime )
  {
    int removedSessions = 0;
    final long now = System.currentTimeMillis();
    _lock.writeLock().lock();
    try
    {
      final Iterator<Entry<String, T>> iterator = _sessions.entrySet().iterator();
      while ( iterator.hasNext() )
      {
        final T session = iterator.next().getValue();
        if ( now - session.getLastAccessedAt() > maxIdleTime )
        {
          iterator.remove();
          removedSessions++;
        }
      }
    }
    finally
    {
      _lock.writeLock().unlock();
    }
    return removedSessions;
  }
}
