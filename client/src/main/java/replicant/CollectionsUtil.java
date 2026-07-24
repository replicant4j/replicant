package replicant;

import arez.Arez;
import grim.annotations.OmitType;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.jspecify.annotations.NonNull;

@OmitType( unless = "arez.collections_properties_unmodifiable" )
final class CollectionsUtil
{
  private CollectionsUtil()
  {
  }

  @NonNull
  static <T> Collection<T> wrap( @NonNull Collection<T> collection )
  {
    return Arez.areCollectionsPropertiesUnmodifiable() ? Collections.unmodifiableCollection( collection ) : collection;
  }

  @NonNull
  static <T> Set<T> wrap( @NonNull Set<T> set )
  {
    return Arez.areCollectionsPropertiesUnmodifiable() ? Collections.unmodifiableSet( set ) : set;
  }

  @NonNull
  static <T> List<T> wrap( @NonNull List<T> list )
  {
    return Arez.areCollectionsPropertiesUnmodifiable() ? Collections.unmodifiableList( list ) : list;
  }

  @NonNull
  static <T> List<T> asList( @NonNull Stream<T> stream )
  {
    return wrap( stream.collect( Collectors.toList() ) );
  }
}
