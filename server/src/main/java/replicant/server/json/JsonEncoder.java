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
import replicant.server.DatasetAddress;
import replicant.server.SubscriptionChange;
import replicant.shared.Messages;

/**
 * Utility class used when encoding EntityChangeCandidate into JSON payload.
 */
public final class JsonEncoder {
    // Use constant to avoid slow filesystem access when serializing a message.
    @NonNull
    private static final JsonGeneratorFactory FACTORY = Json.createGeneratorFactory(null);

    private JsonEncoder() {}

    /**
     * Encode the change set with the EntityChangeCandidates.
     *
     * @param requestId          the requestId that initiated the change. Only set if the packet is destined for the
     *                           originating session.
     * @param response           the response message if the packet is the result of a request with a response and the
     *                           request was initiated by the session.
     * @param datasetCacheVersion the opaque Dataset Cache Version for a complete Cacheable Dataset result.
     * @param changeSet          the Change Set being encoded.
     * @return the encoded change set.
     */
    @NonNull
    public static String encodeChangeSet(
            @Nullable final Integer requestId,
            @Nullable final JsonValue response,
            @Nullable final String datasetCacheVersion,
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
        if (null != datasetCacheVersion) {
            generator.write(Messages.S2C_Common.DATASET_CACHE_VERSION, datasetCacheVersion);
        }

        final var subscriptionChanges = changeSet.getSubscriptionChanges().stream()
                .filter(c -> null == c.filterParameter())
                .toList();
        if (!subscriptionChanges.isEmpty()) {
            generator.writeStartArray(Messages.Update.SUBSCRIPTION_CHANGES);
            subscriptionChanges.stream().map(JsonEncoder::toDescriptor).forEach(generator::write);
            generator.writeEnd();
        }

        final var filterParameterSubscriptionChanges = changeSet.getSubscriptionChanges().stream()
                .filter(c -> null != c.filterParameter())
                .toList();
        if (!filterParameterSubscriptionChanges.isEmpty()) {
            generator.writeStartArray(Messages.Update.FILTER_PARAMETER_SUBSCRIPTION_CHANGES);
            filterParameterSubscriptionChanges.forEach(change -> {
                generator.writeStartObject();
                generator.write(Messages.Update.SUBSCRIPTION_CHANGE, toDescriptor(change));
                generator.write(Messages.Update.FILTER_PARAMETER, change.filterParameter());
                generator.writeEnd();
            });
            generator.writeEnd();
        }

        final var changes = changeSet.getEntityChanges();
        if (!changes.isEmpty()) {
            generator.writeStartArray(Messages.Update.CHANGES);

            for (final var change : changes) {
                final var entityChangeCandidate = change.getEntityChangeCandidate();

                generator.writeStartObject();
                generator.write(
                        Messages.Update.ENTITY_ID,
                        entityChangeCandidate.getTypeId() + "." + entityChangeCandidate.getId());

                final var datasetAddresses = change.getDatasetAddresses();
                if (!datasetAddresses.isEmpty()) {
                    generator.writeStartArray(Messages.Update.DATASET_ADDRESSES);
                    for (final var datasetAddress : datasetAddresses) {
                        generator.write(datasetAddress.toString());
                    }
                    generator.writeEnd();
                }

                if (entityChangeCandidate.isUpdate()) {
                    generator.writeStartObject(Messages.Update.DATA);
                    final var values = Objects.requireNonNull(entityChangeCandidate.getAttributeValues());
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
    public static String toDescriptor(@NonNull final SubscriptionChange subscriptionChange) {
        final var type = subscriptionChange.type();
        final var descriptorCode = SubscriptionChange.Type.SUBSCRIBE == type
                ? Messages.Update.SUBSCRIPTION_CHANGE_SUBSCRIBE
                : SubscriptionChange.Type.UNSUBSCRIBE == type
                        ? Messages.Update.SUBSCRIPTION_CHANGE_UNSUBSCRIBE
                        : SubscriptionChange.Type.UPDATE == type
                                ? Messages.Update.SUBSCRIPTION_CHANGE_UPDATE
                                : Messages.Update.SUBSCRIPTION_CHANGE_INVALIDATE_DATASET_ADDRESS;
        return String.valueOf(descriptorCode) + subscriptionChange.datasetAddress();
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
    public static String encodeUseCachedDatasetMessage(
            @NonNull final DatasetAddress datasetAddress,
            @NonNull final String datasetCacheVersion,
            @Nullable final Integer requestId) {
        final var response = Json.createObjectBuilder()
                .add(Messages.Common.TYPE, Messages.S2C_Type.USE_CACHED_DATASET)
                .add(Messages.Common.DATASET_ADDRESS, datasetAddress.toString())
                .add(Messages.S2C_Common.DATASET_CACHE_VERSION, datasetCacheVersion);
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
