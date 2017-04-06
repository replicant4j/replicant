package org.realityforge.replicant.client.transport;

import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.realityforge.replicant.client.ChannelDescriptor;

final class AreaOfInterestEntry
{
  @Nonnull
  private final String _systemKey;
  @Nonnull
  private final ChannelDescriptor _descriptor;
  @Nonnull
  private final AreaOfInterestAction _action;
  @Nullable
  private final Object _filterParameter;
  @Nullable
  private final Runnable _userAction;
  private boolean _inProgress;

  AreaOfInterestEntry( @Nonnull final String systemKey,
                       @Nonnull final ChannelDescriptor descriptor,
                       @Nonnull final AreaOfInterestAction action,
                       @Nullable final Object filterParameter,
                       @Nullable final Runnable userAction )
  {
    _systemKey = Objects.requireNonNull( systemKey );
    _descriptor = Objects.requireNonNull( descriptor );
    _action = Objects.requireNonNull( action );
    _filterParameter = filterParameter;
    _userAction = userAction;
  }

  @Nonnull
  String getSystemKey()
  {
    return _systemKey;
  }

  @Nonnull
  ChannelDescriptor getDescriptor()
  {
    return _descriptor;
  }

  @Nonnull
  AreaOfInterestAction getAction()
  {
    return _action;
  }

  @Nonnull
  String getCacheKey()
  {
    return _systemKey + ":" + getDescriptor().toString();
  }

  @Nullable
  Object getFilterParameter()
  {
    return _filterParameter;
  }

  @Nullable
  Runnable getUserAction()
  {
    return _userAction;
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

  boolean match( @Nonnull final ChannelDescriptor descriptor, @Nonnull final AreaOfInterestAction action )
  {
    return getAction().equals( action ) && getDescriptor().equals( descriptor );
  }

  @Override
  public String toString()
  {
    final ChannelDescriptor descriptor = getDescriptor();
    return "AOI[SystemKey=" + _systemKey + ",Channel=" + descriptor + ",filter=" + _filterParameter + "]" +
           ( _inProgress ? "(InProgress)" : "" );
  }
}
