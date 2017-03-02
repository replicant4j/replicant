package org.realityforge.replicant.client;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * The service interface for storing entities locally on the client.
 */
public interface EntityRepository
{
  <T> void registerEntity( @Nonnull Class<T> type, @Nonnull Object id, @Nonnull T entity );

  @Nonnull
  <T> T deregisterEntity( @Nonnull Class<T> type, @Nonnull Object id );

  @Nonnull
  default <T> T getByID( @Nonnull final Class<T> type, @Nonnull final Object id )
  {
    final T entity = findByID( type, id );
    if ( null == entity )
    {
      throw new NoSuchEntityException( type, id );
    }
    return entity;
  }

  @Nullable
  default <T> T findByID( @Nonnull final Class<T> type, @Nonnull final Object id )
  {
    return findByID( type, id, true );
  }

  /**
   * Lookup an entity of specified type with specified id, returning null if not present.
   * If forceLink is true then the entity will be linked prior to being returned.
   *
   * @param type      the type of the entity.
   * @param id        the id of the entity.
   * @param forceLink true if link() is to be called on entity before it is returned.
   * @param <T>       the entity type.
   * @return the entity or null if no such entity.
   */
  @Nullable
  <T> T findByID( @Nonnull Class<T> type, @Nonnull Object id, boolean forceLink );

  /**
   * Find an entity based on specified query, returning null if not present.
   *
   * @param type  the type of the entity.
   * @param query the query.
   * @param <T>   the entity type.
   * @return the entity or null if no such entity.
   */
  @Nullable
  default <T> T findByQuery( @Nonnull final Class<T> type, @Nonnull final Predicate<T> query )
  {
    return findAll( type ).stream().filter( query ).findFirst().orElse( null );
  }

  /**
   * Return an entity based on specified query, raising a NoResultException if no matching entity.
   *
   * @param type  the type of the entity.
   * @param query the query.
   * @param <T>   the entity type.
   * @return the entity or null if no such entity.
   */
  @Nonnull
  default <T> T getByQuery( @Nonnull final Class<T> type, @Nonnull final Predicate<T> query )
    throws NoResultException
  {
    final T result = findByQuery( type, query );
    if ( null == result )
    {
      throw new NoResultException();
    }
    return result;
  }

  @Nonnull
  <T> ArrayList<T> findAll( @Nonnull Class<T> type );

  @Nonnull
  default <T> ArrayList<T> findAll( @Nonnull final Class<T> type, @Nonnull final Comparator<T> comparator )
  {
    final ArrayList<T> results = findAll( type );
    results.sort( comparator );
    return results;
  }

  @Nonnull
  default <T> ArrayList<T> findAllByQuery( @Nonnull final Class<T> type, @Nonnull final Predicate<T> query )
  {
    return findAll( type ).stream().filter( query ).collect( Collectors.toCollection( ArrayList::new ) );
  }

  @Nonnull
  default <T> ArrayList<T> findAllByQuery( @Nonnull final Class<T> type,
                                           @Nonnull final Predicate<T> query,
                                           @Nonnull final Comparator<T> comparator )
  {
    final ArrayList<T> results = findAllByQuery( type, query );
    results.sort( comparator );
    return results;
  }

  /**
   * Return the list of ids for entities of a particular type.
   *
   * @param type the entity type.
   * @return the list of ids.
   */
  @Nonnull
  <T> ArrayList<Object> findAllIDs( @Nonnull Class<T> type );

  /**
   * Return the list of types registered in repository.
   *
   * @return the list of types.
   */
  @Nonnull
  ArrayList<Class> getTypes();
}
