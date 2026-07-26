package replicant;

import java.util.Objects;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import replicant.messages.ChannelChange;
import replicant.shared.Messages;

final class ChannelChangeDescriptor {
    enum Type {
        ADD,
        REMOVE,
        UPDATE,
        DELETE
    }

    @NonNull
    private final Type _type;

    @NonNull
    private final DatasetAddress _datasetAddress;

    @Nullable
    private final Object _filter;

    @NonNull
    static ChannelChangeDescriptor from(final int schema, @NonNull final String channelAction) {
        return from(schema, channelAction, null);
    }

    @NonNull
    static ChannelChangeDescriptor from(final int schema, @NonNull final ChannelChange channelChange) {
        return from(schema, channelChange.getChannel(), channelChange.getFilter());
    }

    @NonNull
    private static ChannelChangeDescriptor from(
            final int schema, @NonNull final String channelAction, @Nullable final Object filter) {
        try {
            final String descriptor = channelAction.substring(1);
            final DatasetAddress datasetAddress = DatasetAddress.parse(schema, descriptor);
            return new ChannelChangeDescriptor(actionToType(channelAction), datasetAddress, filter);
        } catch (final Throwable t) {
            throw new IllegalStateException("Failed to parse channel action '" + channelAction + "'", t);
        }
    }

    @NonNull
    private static Type actionToType(@NonNull final String channelAction) {
        assert !channelAction.isEmpty();
        final char commandCode = channelAction.charAt(0);
        final Type type = Messages.Update.CHANNEL_ACTION_ADD == commandCode
                ? Type.ADD
                : Messages.Update.CHANNEL_ACTION_REMOVE == commandCode
                        ? Type.REMOVE
                        : Messages.Update.CHANNEL_ACTION_UPDATE == commandCode
                                ? Type.UPDATE
                                : Messages.Update.CHANNEL_ACTION_DELETE == commandCode ? Type.DELETE : null;
        if (null == type) {
            throw new IllegalArgumentException("Unknown channel action '" + channelAction + "'");
        }
        return type;
    }

    private ChannelChangeDescriptor(
            @NonNull final Type type, @NonNull final DatasetAddress datasetAddress, @Nullable final Object filter) {
        _type = Objects.requireNonNull(type);
        _datasetAddress = Objects.requireNonNull(datasetAddress);
        _filter = filter;
    }

    @NonNull
    Type getType() {
        return _type;
    }

    @NonNull
    DatasetAddress getDatasetAddress() {
        return _datasetAddress;
    }

    @Nullable
    Object getFilter() {
        return _filter;
    }
}
