package replicant.server.json;

import java.io.Serializable;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonValue;
import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonGeneratorFactory;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import replicant.server.ChangeSet;
import replicant.server.ChannelAction;
import replicant.server.ChannelAction.Action;
import replicant.server.ChannelAddress;
import replicant.shared.Messages;

/**
 * Utility class used when encoding EntityMessage into JSON payload.
 */
public final class JsonEncoder {
    // Use constant to avoid slow filesystem access when serializing a message.
    @NonNull
    private static final JsonGeneratorFactory FACTORY = Json.createGeneratorFactory(null);

    private JsonEncoder() {}

    /**
     * Encode the change set with the EntityMessages.
     *
     * @param requestId the requestId that initiated the change. Only set if packet is destined for originating session.
     * @param response  the response message if the packet is the result of a request that has a response,
     *                  and the request was initiated by the session.
     * @param etag      the associated etag.
     * @param changeSet the changeSet being encoded.
     * @return the encoded change set.
     */
    @NonNull
    public static String encodeChangeSet(
            @Nullable final Integer requestId,
            @Nullable final JsonValue response,
            @Nullable final String etag,
            @NonNull final ChangeSet changeSet) {
        final var writer = new StringWriter();
        final var generator = FACTORY.createGenerator(writer);
        final var dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.ROOT);

        generator.writeStartObject();
        generator.write(Messages.Common.TYPE, Messages.S2C_Type.UPDATE);
        if (null != requestId) {
            generator.write(Messages.Common.REQUEST_ID, requestId);
        }
        if (null != response) {
            generator.write(Messages.Update.RESPONSE, response);
        }
        if (null != etag) {
            generator.write(Messages.S2C_Common.ETAG, etag);
        }

        final var actions = changeSet.getChannelActions().stream()
                .filter(c -> null == c.filter())
                .toList();
        if (!actions.isEmpty()) {
            generator.writeStartArray(Messages.Update.CHANNEL_ACTIONS);
            actions.stream().map(JsonEncoder::toDescriptor).forEach(generator::write);
            generator.writeEnd();
        }

        final var filteredActions = changeSet.getChannelActions().stream()
                .filter(c -> null != c.filter())
                .toList();
        if (!filteredActions.isEmpty()) {
            generator.writeStartArray(Messages.Update.FILTERED_CHANNEL_ACTIONS);
            filteredActions.forEach(a -> {
                generator.writeStartObject();
                generator.write(Messages.Common.CHANNEL, toDescriptor(a));
                generator.write(Messages.Update.FILTER, a.filter());
                generator.writeEnd();
            });
            generator.writeEnd();
        }

        final var changes = changeSet.getChanges();
        if (!changes.isEmpty()) {
            generator.writeStartArray(Messages.Update.CHANGES);

            for (final var change : changes) {
                final var entityMessage = change.getEntityMessage();

                generator.writeStartObject();
                generator.write(Messages.Update.ENTITY_ID, entityMessage.getTypeId() + "." + entityMessage.getId());

                final var channels = change.getChannels();
                if (!channels.isEmpty()) {
                    generator.writeStartArray(Messages.Update.CHANNELS);
                    for (final var address : channels) {
                        assert address.concrete();
                        generator.write(address.toString());
                    }
                    generator.writeEnd();
                }

                if (entityMessage.isUpdate()) {
                    generator.writeStartObject(Messages.Update.DATA);
                    final var values = Objects.requireNonNull(entityMessage.getAttributeValues());
                    for (final var entry : values.entrySet()) {
                        writeField(generator, entry.getKey(), entry.getValue(), dateFormat);
                    }
                    generator.writeEnd();
                }
                generator.writeEnd();
            }
            generator.writeEnd();
        }
        generator.writeEnd();
        generator.close();
        return writer.toString();
    }

    @NonNull
    public static String toDescriptor(@NonNull final ChannelAction channelAction) {
        assert channelAction.address().concrete();
        final var action = channelAction.action();
        final var actionValue = Action.ADD == action
                ? Messages.Update.CHANNEL_ACTION_ADD
                : Action.REMOVE == action
                        ? Messages.Update.CHANNEL_ACTION_REMOVE
                        : Action.UPDATE == action
                                ? Messages.Update.CHANNEL_ACTION_UPDATE
                                : Messages.Update.CHANNEL_ACTION_DELETE;
        return String.valueOf(actionValue) + channelAction.address();
    }

    @SuppressWarnings("StatementWithEmptyBody")
    private static void writeField(
            @NonNull final JsonGenerator generator,
            @NonNull final String key,
            @Nullable final Serializable serializable,
            @NonNull final SimpleDateFormat dateFormat) {
        if (serializable instanceof String) {
            generator.write(key, (String) serializable);
        } else if (serializable instanceof Integer) {
            generator.write(key, (Integer) serializable);
        } else if (serializable instanceof Long) {
            generator.write(key, new BigDecimal((Long) serializable).toString());
        } else if (null == serializable) {
            // No need to write anything as the client code will treat missing field as null
        } else if (serializable instanceof Float) {
            generator.write(key, (Float) serializable);
        } else if (serializable instanceof Date) {
            generator.write(key, dateFormat.format((Date) serializable));
        } else if (serializable instanceof Boolean) {
            generator.write(key, (Boolean) serializable);
        } else {
            throw new IllegalStateException("Unable to encode: " + serializable);
        }
    }

    @NonNull
    public static String encodeUseCacheMessage(
            @NonNull final ChannelAddress address, @NonNull final String eTag, @Nullable final Integer requestId) {
        assert address.concrete();
        final var response = Json.createObjectBuilder()
                .add(Messages.Common.TYPE, Messages.S2C_Type.USE_CACHE)
                .add(Messages.Common.CHANNEL, address.toString())
                .add(Messages.S2C_Common.ETAG, eTag);
        if (null != requestId) {
            response.add(Messages.Common.REQUEST_ID, requestId);
        }
        return asString(response.build());
    }

    @NonNull
    public static String encodeSessionCreatedMessage(@NonNull final String sessionId) {
        return asString(Json.createObjectBuilder()
                .add(Messages.Common.TYPE, Messages.S2C_Type.SESSION_CREATED)
                .add(Messages.S2C_Common.SESSION_ID, sessionId)
                .build());
    }

    @NonNull
    public static String encodeOkMessage(final int requestId) {
        return asString(Json.createObjectBuilder()
                .add(Messages.Common.TYPE, Messages.S2C_Type.OK)
                .add(Messages.Common.REQUEST_ID, requestId)
                .build());
    }

    @NonNull
    public static String encodeMalformedMessageMessage(@NonNull final String message) {
        return asString(Json.createObjectBuilder()
                .add(Messages.Common.TYPE, Messages.S2C_Type.MALFORMED_MESSAGE)
                .add(Messages.S2C_Common.MESSAGE, message)
                .build());
    }

    @NonNull
    public static String encodeUnknownRequestType(@NonNull final JsonObject command) {
        return asString(Json.createObjectBuilder()
                .add(Messages.Common.TYPE, Messages.S2C_Type.UNKNOWN_REQUEST_TYPE)
                .add(Messages.Common.COMMAND, command)
                .build());
    }

    @NonNull
    public static String encodeErrorMessage(@NonNull final String message) {
        return asString(Json.createObjectBuilder()
                .add(Messages.Common.TYPE, Messages.S2C_Type.ERROR)
                .add(Messages.S2C_Common.MESSAGE, message)
                .build());
    }

    @NonNull
    private static String asString(@NonNull final JsonObject message) {
        final var writer = new StringWriter();
        final var jsonWriter = Json.createWriter(writer);
        jsonWriter.writeObject(message);
        jsonWriter.close();
        writer.flush();
        return writer.toString();
    }
}
