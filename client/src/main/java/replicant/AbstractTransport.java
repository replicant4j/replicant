package replicant;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import jsinterop.base.Js;
import jsinterop.base.JsPropertyMap;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import replicant.messages.AuthTokenMessage;
import replicant.messages.BulkSubscribeMessage;
import replicant.messages.BulkUnsubscribeMessage;
import replicant.messages.DatasetCacheVersionsMessage;
import replicant.messages.ExecMessage;
import replicant.messages.PingMessage;
import replicant.messages.ServerToClientMessage;
import replicant.messages.SubscribeMessage;
import replicant.messages.UnsubscribeMessage;

public abstract class AbstractTransport implements Transport {
    @Nullable
    private TransportContext _transportContext;

    @Override
    public final void unbind() {
        doDisconnect();
        _transportContext = null;
    }

    @Override
    public final void requestSynchronizationPoint() {
        assert null != _transportContext;
        final int requestId = newRequestId("SynchronizationPoint", true, null);
        sendRemoteMessage(PingMessage.create(requestId));
    }

    @Override
    public final void updateAuthToken(@Nullable final String authToken) {
        final int requestId = newRequestId("Auth", true, null);
        sendRemoteMessage(AuthTokenMessage.create(requestId, authToken));
    }

    @Override
    public final void updateDatasetCacheVersionsAndRequestSynchronizationPoint(
            @NonNull final Map<String, String> datasetAddressToDatasetCacheVersionMap) {
        final JsPropertyMap<Object> map = JsPropertyMap.of();
        datasetAddressToDatasetCacheVersionMap.forEach(map::set);
        final int requestId = newRequestId("DatasetCacheVersions", true, null);
        sendRemoteMessage(DatasetCacheVersionsMessage.create(requestId, Js.uncheckedCast(map)));
    }

    @Override
    public void requestExec(
            @NonNull final String command,
            @Nullable final Object payload,
            @Nullable final ResponseHandler responseHandler) {
        final int requestId = newRequestId("Exec-" + command, responseHandler);
        sendRemoteMessage(ExecMessage.create(requestId, command, payload));
    }

    @Override
    public final void requestSubscribe(
            @NonNull final DatasetAddress datasetAddress, @Nullable final Object filterParameter) {
        final int requestId = newRequestId(toRequestKey("Subscribe", datasetAddress), null);
        sendRemoteMessage(
                SubscribeMessage.create(requestId, datasetAddress.asDatasetAddressDescriptor(), filterParameter));
    }

    @Override
    public final void requestUnsubscribe(@NonNull final DatasetAddress datasetAddress) {
        final int requestId = newRequestId(toRequestKey("Unsubscribe", datasetAddress), null);
        sendRemoteMessage(UnsubscribeMessage.create(requestId, datasetAddress.asDatasetAddressDescriptor()));
    }

    @Override
    public final void requestBulkSubscribe(
            @NonNull final List<DatasetAddress> datasetAddresses, @Nullable final Object filterParameter) {
        final int requestId = newRequestId(toRequestKey("BulkSubscribe", datasetAddresses), null);
        final String[] datasetAddressDescriptors = datasetAddresses.stream()
                .map(DatasetAddress::asDatasetAddressDescriptor)
                .toArray(String[]::new);
        sendRemoteMessage(BulkSubscribeMessage.create(requestId, datasetAddressDescriptors, filterParameter));
    }

    @Override
    public final void requestBulkUnsubscribe(@NonNull final List<DatasetAddress> datasetAddresses) {
        final int requestId = newRequestId(toRequestKey("BulkUnsubscribe", datasetAddresses), null);
        final String[] datasetAddressDescriptors = datasetAddresses.stream()
                .map(DatasetAddress::asDatasetAddressDescriptor)
                .toArray(String[]::new);
        sendRemoteMessage(BulkUnsubscribeMessage.create(requestId, datasetAddressDescriptors));
    }

    @Override
    public final void requestConnect(@NonNull final TransportContext context) {
        _transportContext = Objects.requireNonNull(context);
        doConnect();
    }

    @Override
    public final void requestDisconnect() {
        doDisconnect();
        _transportContext = null;
    }

    protected final void onMessageReceived(@NonNull final ServerToClientMessage message) {
        // if connection has been disconnected whilst poller request was in flight then ignore response
        if (null != _transportContext) {
            _transportContext.onMessageReceived(message);
        }
    }

    protected final void onError() {
        // if connection has been disconnected whilst poller request was in flight then ignore response
        if (null != _transportContext) {
            _transportContext.onError();
        }
    }

    protected final void onDisconnect() {
        // if connection has been disconnected then ignore disconnect
        if (null != _transportContext) {
            _transportContext.onDisconnect();
        }
    }

    @Nullable
    private String toRequestKey(
            @NonNull final String requestType, @NonNull final Collection<DatasetAddress> datasetAddresses) {
        if (Replicant.areNamesEnabled()) {
            final DatasetAddress datasetAddress = datasetAddresses.iterator().next();
            return requestType + ":" + datasetAddress.getName();
        } else {
            return null;
        }
    }

    @Nullable
    private String toRequestKey(@NonNull final String requestType, @NonNull final DatasetAddress datasetAddress) {
        return Replicant.areNamesEnabled() ? requestType + ":" + datasetAddress : null;
    }

    private int newRequestId(@Nullable final String name, @Nullable final ResponseHandler responseHandler) {
        return newRequestId(name, false, responseHandler);
    }

    private int newRequestId(
            @Nullable final String name,
            final boolean synchronizationPointRequest,
            @Nullable final ResponseHandler responseHandler) {
        return Objects.requireNonNull(_transportContext)
                .newRequestId(name, synchronizationPointRequest, responseHandler);
    }

    @Nullable
    protected final TransportContext getTransportContext() {
        return _transportContext;
    }

    protected abstract void doConnect();

    protected abstract void sendRemoteMessage(@NonNull Object message);

    protected abstract void doDisconnect();
}
