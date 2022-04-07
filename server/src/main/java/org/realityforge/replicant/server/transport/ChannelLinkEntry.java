package org.realityforge.replicant.server.transport;

import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.realityforge.replicant.server.ChannelAddress;

final class ChannelLinkEntry
{
  @Nonnull
  private final ChannelAddress _source;
  @Nonnull
  private final ChannelAddress _target;
  @Nullable
  private final Object _filter;

  ChannelLinkEntry( @Nonnull final ChannelAddress source,
                    @Nonnull final ChannelAddress target,
                    @Nullable final Object filter )
  {
    _source = Objects.requireNonNull( source );
    _target = Objects.requireNonNull( target );
    _filter = filter;
  }

  @Nonnull
  ChannelAddress getSource()
  {
    return _source;
  }

  @Nonnull
  ChannelAddress getTarget()
  {
    return _target;
  }

  @Nullable
  Object getFilter()
  {
    return _filter;
  }

  @Override
  public boolean equals( final Object o )
  {
    if ( this == o )
    {
      return true;
    }
    else if ( null == o || getClass() != o.getClass() )
    {
      return false;
    }
    else
    {
      final ChannelLinkEntry that = (ChannelLinkEntry) o;
      return _target.equals( that._target ) && Objects.equals( _filter, that._filter );
    }
  }

  @Override
  public int hashCode()
  {
    return Objects.hash( _target, _filter );
  }
}
