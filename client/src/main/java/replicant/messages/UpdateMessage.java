package replicant.messages;

import java.util.HashSet;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import jsinterop.annotations.JsOverlay;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsType;
import jsinterop.base.Any;
import replicant.Replicant;
import replicant.shared.Messages;
import static org.realityforge.braincheck.Guards.*;

/**
 * The message that represents a set of changes to subscriptions and entities that should be applied atomically.
 */
@SuppressFBWarnings( "EI_EXPOSE_REP" )
@JsType( isNative = true, namespace = JsPackage.GLOBAL, name = "Object" )
public class UpdateMessage
  extends ServerToClientMessage
{
  @JsOverlay
  public static final String TYPE = Messages.S2C_Type.UPDATE;

  @Nullable
  private String etag;
  @Nullable
  private String[] channels;
  @Nullable
  private ChannelChange[] fchannels;
  @Nullable
  private EntityChange[] changes;
  @Nullable
  private Any response;

  @GwtIncompatible
  @Nonnull
  public static UpdateMessage create( @Nullable final Integer requestId,
                                      @Nullable final String eTag,
                                      @Nullable final String[] channels,
                                      @Nullable final ChannelChange[] fchannels,
                                      @Nullable final EntityChange[] entityChanges )
  {
    final UpdateMessage updateMessage = new UpdateMessage();
    updateMessage.type = TYPE;
    updateMessage.requestId = null == requestId ? null : requestId.doubleValue();
    updateMessage.etag = eTag;
    updateMessage.channels = channels;
    updateMessage.fchannels = fchannels;
    updateMessage.changes = entityChanges;
    return updateMessage;
  }

  /**
   * @return the version under which this can be cached.
   */
  @Nullable
  @JsOverlay
  public final String getETag()
  {
    return etag;
  }

  /**
   * @return the exec response associated with the message, if any
   */
  @Nullable
  @JsOverlay
  public final Any getResponse()
  {
    return response;
  }

  /**
   * Return the channel changes that are part of the message.
   * This should only be invoked if {@link #hasChannels()} return true.
   *
   * @return the channel changes.
   */
  @Nonnull
  @JsOverlay
  public final String[] getChannels()
  {
    if ( Replicant.shouldCheckApiInvariants() )
    {
      apiInvariant( () -> null != channels,
                    () -> "Replicant-0013: UpdateMessage.getChannels() invoked when no changes are present. Should guard call with UpdateMessage.hasChannels()." );
    }
    assert null != channels;
    return channels;
  }

  /**
   * Return true if this UpdateMessage contains Channels.
   *
   * @return true if this UpdateMessage contains Channels
   */
  @JsOverlay
  public final boolean hasChannels()
  {
    return null != channels;
  }

  /**
   * Return the filtered channel changes that are part of the message.
   * This should only be invoked if {@link #hasFilteredChannels()} return true.
   *
   * @return the channel changes.
   */
  @Nonnull
  @JsOverlay
  public final ChannelChange[] getFilteredChannels()
  {
    if ( Replicant.shouldCheckApiInvariants() )
    {
      apiInvariant( () -> null != fchannels,
                    () -> "Replicant-0030: UpdateMessage.getFilteredChannels() invoked when no changes are present. Should guard call with UpdateMessage.hasFilteredChannels()." );
    }
    assert null != fchannels;
    return fchannels;
  }

  /**
   * Return true if this UpdateMessage contains filtered channels.
   *
   * @return true if this UpdateMessage contains filtered channels
   */
  @JsOverlay
  public final boolean hasFilteredChannels()
  {
    return null != fchannels;
  }

  /**
   * Return the entity changes that are part of the message.
   * This should only be invoked if {@link #hasEntityChanges()} return true.
   *
   * @return the entity changes.
   */
  @Nonnull
  @JsOverlay
  public final EntityChange[] getEntityChanges()
  {
    if ( Replicant.shouldCheckApiInvariants() )
    {
      apiInvariant( () -> null != changes,
                    () -> "Replicant-0012: UpdateMessage.getEntityChanges() invoked when no changes are present. Should guard call with UpdateMessage.hasEntityChanges()." );
    }
    assert null != changes;
    return changes;
  }

  /**
   * Return true if this UpdateMessage contains EntityChanges.
   *
   * @return true if this UpdateMessage contains EntityChanges
   */
  @JsOverlay
  public final boolean hasEntityChanges()
  {
    return null != changes;
  }

  /**
   * This method will validate the UpdateMessage to make sure it is internally consistent if invariants are enabled.
   * The validation will ensure that there is not multiple EntityChange messages for the same entity and that
   * there is not multiple ChannelChange messages for the same channel.
   */
  @JsOverlay
  public final void validate()
  {
    if ( Replicant.shouldCheckApiInvariants() )
    {
      if ( null != changes )
      {
        final HashSet<String> existing = new HashSet<>();
        for ( final EntityChange change : changes )
        {
          final String id = change.getId();
          apiInvariant( () -> existing.add( id ),
                        () -> "Replicant-0014: UpdateMessage contains multiple EntityChange messages " +
                              "with the id '" + id + "'." );
        }
      }
      final HashSet<String> existingChannels = new HashSet<>();
      if ( null != channels )
      {
        for ( final String channelAction : channels )
        {
          final String key = channelAction.substring( 1 );
          apiInvariant( () -> existingChannels.add( key ),
                        () -> "Replicant-0022: UpdateMessage contains multiple ChannelChange messages " +
                              "for the channel " + channelAction.substring( 1 ) + "." );
        }
      }
      if ( null != fchannels )
      {
        for ( final ChannelChange channelChange : fchannels )
        {
          final String channelAction = channelChange.getChannel();
          final String key = channelAction.substring( 1 );
          apiInvariant( () -> existingChannels.add( key ),
                        () -> "Replicant-0028: UpdateMessage contains multiple ChannelChange messages " +
                              "for the channel " + channelAction.substring( 1 ) + "." );

        }
      }
    }
  }
}
