package org.realityforge.replicant.client.transport;

import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.realityforge.replicant.client.ChannelAddress;
import org.realityforge.replicant.client.FilterUtil;

final class AreaOfInterestEntry
{
  @Nonnull
  private final String _systemKey;
  @Nonnull
  private final ChannelAddress _address;
  @Nonnull
  private final AreaOfInterestAction _action;
  @Nullable
  private final Object _filterParameter;
  private boolean _inProgress;

  AreaOfInterestEntry( @Nonnull final String systemKey,
                       @Nonnull final ChannelAddress address,
                       @Nonnull final AreaOfInterestAction action,
                       @Nullable final Object filterParameter )
  {
    _systemKey = Objects.requireNonNull( systemKey );
    _address = Objects.requireNonNull( address );
    _action = Objects.requireNonNull( action );
    _filterParameter = filterParameter;
  }

  @Nonnull
  String getSystemKey()
  {
    return _systemKey;
  }

  @Nonnull
  ChannelAddress getAddress()
  {
    return _address;
  }

  @Nonnull
  AreaOfInterestAction getAction()
  {
    return _action;
  }

  @Nonnull
  String getCacheKey()
  {
    return _systemKey + ":" + getAddress().toString();
  }

  @Nullable
  Object getFilterParameter()
  {
    return _filterParameter;
  }

  boolean isInProgress()
  {
    return _inProgress;
  }

  void markAsInProgress()
  {
    _inProgress = true;
  }

  void markAsComplete()
  {
    _inProgress = false;
  }

  boolean match( @Nonnull final AreaOfInterestAction action,
                 @Nonnull final ChannelAddress descriptor,
                 @Nullable final Object filter )
  {
    return getAction().equals( action ) &&
           getAddress().equals( descriptor ) &&
           ( AreaOfInterestAction.REMOVE == action || FilterUtil.filtersEqual( filter, getFilterParameter() ) );
  }

  @Override
  public String toString()
  {
    final ChannelAddress descriptor = getAddress();
    return "AOI[SystemKey=" + _systemKey + ",Channel=" + descriptor + ",filter=" + FilterUtil.filterToString( _filterParameter ) + "]" +
           ( _inProgress ? "(InProgress)" : "" );
  }
}
