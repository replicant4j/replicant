package org.realityforge.replicant.client;

import java.util.ArrayList;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * The service interface for storing entities locally on the client.
 */
public interface EntityRepository
{
  <T> void registerEntity( Class<T> type, Object id, T entity );

  @Nonnull
  <T> T deregisterEntity( Class<T> type, Object id );

  @Nonnull
  <T> T getByID( Class<T> type, Object id );

  @Nullable
  <T> T findByID( Class<T> type, Object id );

  @Nonnull
  <T> ArrayList<T> findAll( Class<T> type );
}
