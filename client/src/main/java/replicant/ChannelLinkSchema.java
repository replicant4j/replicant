package replicant;

import java.util.Objects;
import javax.annotation.Nonnull;

/**
 * Metadata that describes the link from an entity in one channel to another channel.
 * The entity MUST be contained in the source channel. The target channel must be related to the
 * entity by a reference or a reference and specified path.
 *
 * Note: This is not used at runtime, as domgen generates the glue code but it is used in various
 * supporting infrastructure such as tests to verify changes.
 */
public final class ChannelLinkSchema
{
  /**
   * The id of the "source" graph that we are linking from.
   */
  private final int _sourceChannelId;
  /**
   * The id of the "target" graph that we are linking to.
   */
  private final int _targetChannelId;
  /**
   * Does the runtime generated by domgen automatically link from the source channel to target channel if
   * an entity in source graph is replicated.
   */
  private final boolean _auto;
  /**
   * The path of attributes from the entity in the source channel to the root entity of the target channel.
   * It is expected that the attributes are immutable and all but the first are non-null.(Unlike Domgen where
   * the path omits the first attribute, this path includes the entire path)
   */
  @Nonnull
  private final String[] _path;

  public ChannelLinkSchema( final int sourceChannelId,
                            final int targetChannelId,
                            final boolean auto,
                            @Nonnull final String[] path )
  {
    _sourceChannelId = sourceChannelId;
    _targetChannelId = targetChannelId;
    _auto = auto;
    _path = Objects.requireNonNull( path );
  }

  public int getSourceChannelId()
  {
    return _sourceChannelId;
  }

  public int getTargetChannelId()
  {
    return _targetChannelId;
  }

  public boolean isAuto()
  {
    return _auto;
  }

  @Nonnull
  public String[] getPath()
  {
    return _path;
  }
}
