package replicant;

import arez.Arez;
import grim.annotations.OmitType;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nonnull;

@OmitType( unless = "arez.collections_properties_unmodifiable" )
final class CollectionsUtil
{
  private CollectionsUtil()
  {
  }

  @Nonnull
  static <T> Collection<T> wrap( @Nonnull Collection<T> collection )
  {
    return Arez.areCollectionsPropertiesUnmodifiable() ? Collections.unmodifiableCollection( collection ) : collection;
  }

  @Nonnull
  static <T> Set<T> wrap( @Nonnull Set<T> set )
  {
    return Arez.areCollectionsPropertiesUnmodifiable() ? Collections.unmodifiableSet( set ) : set;
  }

  @Nonnull
  static <T> List<T> wrap( @Nonnull List<T> list )
  {
    return Arez.areCollectionsPropertiesUnmodifiable() ? Collections.unmodifiableList( list ) : list;
  }

  @Nonnull
  static <T> List<T> asList( @Nonnull Stream<T> stream )
  {
    return wrap( stream.collect( Collectors.toList() ) );
  }
}
