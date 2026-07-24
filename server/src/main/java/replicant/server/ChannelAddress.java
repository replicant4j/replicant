package replicant.server;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public record ChannelAddress(
        int channelId, @Nullable Integer rootId, @Nullable String filterInstanceId, boolean partial)
        implements Comparable<ChannelAddress> {
    @NonNull
    public static ChannelAddress parse(@NonNull final String name) {
        final var instanceOffset = name.indexOf('#');
        final var channelPart = -1 == instanceOffset ? name : name.substring(0, instanceOffset);
        final var filterInstanceId = -1 == instanceOffset ? null : name.substring(instanceOffset + 1);
        final var offset = channelPart.indexOf(".");
        final var channelId = Integer.parseInt(-1 == offset ? channelPart : channelPart.substring(0, offset));
        final var rootId = -1 == offset ? null : Integer.parseInt(channelPart.substring(offset + 1));
        return new ChannelAddress(channelId, rootId, filterInstanceId, false);
    }

    @NonNull
    public static ChannelAddress partial(final int channelId) {
        return new ChannelAddress(channelId, null, null, true);
    }

    @NonNull
    public static ChannelAddress partial(final int channelId, @Nullable final Integer rootId) {
        return new ChannelAddress(channelId, rootId, null, true);
    }

    @NonNull
    public static ChannelAddress of(final int channelId) {
        return new ChannelAddress(channelId, null, null, false);
    }

    @NonNull
    public static ChannelAddress of(final int channelId, @Nullable final Integer rootId) {
        return new ChannelAddress(channelId, rootId, null, false);
    }

    @NonNull
    public static ChannelAddress of(
            final int channelId, @Nullable final Integer rootId, @Nullable final String filterInstanceId) {
        return new ChannelAddress(channelId, rootId, filterInstanceId, false);
    }

    public ChannelAddress {
        assert !partial || null == filterInstanceId;
    }

    public boolean concrete() {
        return !partial;
    }

    public boolean hasRootId() {
        return null != rootId;
    }

    @Override
    public int compareTo(@NonNull final ChannelAddress other) {
        final var channelDiff = Integer.compare(channelId(), other.channelId());
        if (0 != channelDiff) {
            return channelDiff;
        } else {
            final var otherRootId = other.rootId();
            final var rootId = rootId();
            if (null != otherRootId || null != rootId) {
                if (null == otherRootId) {
                    return -1;
                } else if (null == rootId) {
                    return 1;
                } else {
                    final var rootDiff = rootId.compareTo(otherRootId);
                    if (0 != rootDiff) {
                        return rootDiff;
                    }
                }
            }
        }
        final var f1 = filterInstanceId();
        final var f2 = other.filterInstanceId();
        if (null == f1 && null == f2) {
            if (partial() == other.partial()) {
                return 0;
            } else {
                return partial() ? 1 : -1;
            }
        } else if (null == f1) {
            return -1;
        } else if (null == f2) {
            return 1;
        } else if (!f1.equals(f2)) {
            return f1.compareTo(f2);
        } else if (partial() == other.partial()) {
            return 0;
        } else {
            return partial() ? 1 : -1;
        }
    }

    @NonNull
    @Override
    public String toString() {
        final var base = channelId + (null == rootId ? "" : "." + rootId);
        return base + (null == filterInstanceId ? "" : "#" + filterInstanceId) + (partial ? "?" : "");
    }
}
