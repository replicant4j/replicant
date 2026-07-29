package replicant.server.ee;

import java.io.IOException;
import java.io.StringReader;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Event;
import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonString;
import javax.transaction.Transactional;
import javax.websocket.CloseReason;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import replicant.server.DatasetAddress;
import replicant.server.json.JsonEncoder;
import replicant.server.transport.Dataset;
import replicant.server.transport.ReplicantSession;
import replicant.server.transport.ReplicantSessionManager;
import replicant.server.transport.WebSocketUtil;
import replicant.shared.Messages;
import replicant.shared.SharedConstants;

@ServerEndpoint("/api" + SharedConstants.REPLICANT_URL_FRAGMENT)
@ApplicationScoped
@Transactional
public class ReplicantEndpoint {
    @NonNull
    protected static final Logger LOG = Logger.getLogger(ReplicantEndpoint.class.getName());

    @Inject
    private ReplicantSessionManager _sessionManager;

    @Inject
    private ReplicantHandshakeAuthenticator _handshakeAuthenticator;

    @Inject
    private Event<ReplicantSessionAdded> _replicantSessionAddedEventEvent;

    @Inject
    private Event<ReplicantSessionUpdated> _replicantSessionUpdatedEvent;

    @Inject
    private Event<ReplicantSessionRemoved> _replicantSessionRemovedEvent;

    @OnOpen
    public void onOpen(@NonNull final Session session) throws IOException {
        final var authorization = _handshakeAuthenticator.authenticate(session);
        if (null == authorization) {
            session.close(new CloseReason(CloseReason.CloseCodes.VIOLATED_POLICY, "Authentication required"));
            return;
        }
        final ReplicantSession newReplicantSession;
        try {
            newReplicantSession = _sessionManager.createSession(session, authorization);
        } catch (final RuntimeException e) {
            authorization.close();
            throw e;
        }
        if (LOG.isLoggable(Level.FINE)) {
            LOG.log(
                    Level.FINE,
                    "Opening WebSocket Session " + session.getId() + " for Replicant Session ID "
                            + getReplicantSession(session).getReplicantSessionId());
        }

        _replicantSessionAddedEventEvent.fire(new ReplicantSessionAdded(newReplicantSession.getReplicantSessionId()));

        WebSocketUtil.sendText(
                session, JsonEncoder.encodeSessionCreatedMessage(newReplicantSession.getReplicantSessionId()));
    }

    @OnMessage
    @Transactional
    public void onMessage(@NonNull final Session session, @NonNull final String message) throws IOException {
        final ReplicantSession replicantSession;
        try {
            replicantSession = getReplicantSession(session);
        } catch (final Throwable ignored) {
            sendErrorAndClose(session, "Unable to locate associated Replicant Session");
            return;
        }
        if (LOG.isLoggable(Level.FINE)) {
            LOG.log(
                    Level.FINE,
                    "Message on WebSocket Session " + session.getId() + " for Replicant Session ID "
                            + getReplicantSession(session).getReplicantSessionId() + ". Message:\n" + message);
        }
        final JsonObject request;
        final String type;
        final int requestId;
        try {
            request = Json.createReader(new StringReader(message)).readObject();
            type = request.getString(Messages.Common.TYPE);
            requestId = request.getInt(Messages.Common.REQUEST_ID);
        } catch (final Throwable ignored) {
            if (!runIfValid(replicantSession, () -> onMalformedMessage(replicantSession, message))) {
                sendErrorAndClose(session, "Replicant session not authorized");
            }
            return;
        }
        if (!runIfValid(replicantSession, () -> processRequest(replicantSession, request, type, requestId))) {
            sendErrorAndClose(session, "Replicant session not authorized");
        }
    }

    private void processRequest(
            @NonNull final ReplicantSession replicantSession,
            @NonNull final JsonObject request,
            @NonNull final String type,
            final int requestId)
            throws IOException {
        try {
            //noinspection IfCanBeSwitch
            if (Messages.C2S_Type.COMMAND.equals(type)) {
                _sessionManager.executeCommand(
                        replicantSession,
                        request.getString(Messages.Command.NAME),
                        request.getInt(Messages.Common.REQUEST_ID),
                        request.containsKey(Messages.Command.PAYLOAD)
                                ? request.getJsonObject(Messages.Command.PAYLOAD)
                                : null);
            } else if (Messages.C2S_Type.DATASET_CACHE_VERSIONS.equals(type)) {
                onDatasetCacheVersions(replicantSession, request);
            } else if (Messages.C2S_Type.PING.equals(type)) {
                sendOk(replicantSession.getWebSocketSession(), requestId);
            } else if (Messages.C2S_Type.SUB.equals(type)) {
                onSubscribe(replicantSession, request);
            } else if (Messages.C2S_Type.BULK_SUB.equals(type)) {
                onBulkSubscribe(replicantSession, request);
            } else if (Messages.C2S_Type.UNSUB.equals(type)) {
                onUnsubscribe(replicantSession, request);
            } else if (Messages.C2S_Type.BULK_UNSUB.equals(type)) {
                onBulkUnsubscribe(replicantSession, request);
            } else {
                onUnknownType(replicantSession, request);
            }
            _replicantSessionUpdatedEvent.fire(new ReplicantSessionUpdated(replicantSession.getReplicantSessionId()));
        } catch (final SecurityException ignored) {
            sendErrorAndClose(replicantSession, "Security constraints violated");
        }
    }

    private void sendOk(@NonNull final Session session, final int requestId) {
        WebSocketUtil.sendText(session, JsonEncoder.encodeOkMessage(requestId));
    }

    private static boolean runIfValid(
            final ReplicantSession session,
            final replicant.server.transport.ReplicantSessionAuthorization.Action action)
            throws IOException {
        // Match outbound lock ordering: Replicant connection first, then the application authentication-session gate.
        final var lock = session.getLock();
        lock.lock();
        try {
            return session.runIfValid(action);
        } finally {
            lock.unlock();
        }
    }

    private void onDatasetCacheVersions(@NonNull final ReplicantSession session, @NonNull final JsonObject request) {
        final var datasetCacheVersions = new HashMap<DatasetAddress, String>();
        for (final var entry : request.getJsonObject(Messages.DatasetCacheVersions.DATASET_CACHE_VERSIONS)
                .entrySet()) {
            final var datasetAddress = DatasetAddress.parse(entry.getKey());
            final var datasetCacheVersion = ((JsonString) entry.getValue()).getString();
            datasetCacheVersions.put(datasetAddress, datasetCacheVersion);
        }
        _sessionManager.setDatasetCacheVersions(session, datasetCacheVersions);

        sendOk(session.getWebSocketSession(), request.getInt(Messages.Common.REQUEST_ID));
    }

    private void onMalformedMessage(@NonNull final ReplicantSession replicantSession, @NonNull final String message) {
        closeWithError(replicantSession, "Malformed message", JsonEncoder.encodeMalformedMessageMessage(message));
    }

    private void onUnknownType(@NonNull final ReplicantSession replicantSession, @NonNull final JsonObject request) {
        closeWithError(replicantSession, "Unknown request type", JsonEncoder.encodeUnknownRequestType(request));
    }

    private void onSubscribe(@NonNull final ReplicantSession replicantSession, @NonNull final JsonObject request)
            throws IOException {
        final var datasetAddress = DatasetAddress.parse(request.getString(Messages.Common.DATASET_ADDRESS));
        final var dataset = getDataset(datasetAddress.datasetId());
        if (checkSubscribeRequest(replicantSession, dataset, datasetAddress)) {
            final var requestId = request.getInt(Messages.Common.REQUEST_ID);
            final var filterParameter = extractFilterParameter(dataset, request);
            _sessionManager.subscribe(
                    replicantSession, requestId, Collections.singletonList(datasetAddress), filterParameter);
        }
    }

    private boolean checkSubscribeRequest(
            @NonNull final ReplicantSession replicantSession,
            @NonNull final Dataset dataset,
            @NonNull final DatasetAddress datasetAddress)
            throws IOException {
        if (!dataset.getVisibility().permitsAreaOfInterestOrigin()) {
            sendErrorAndClose(
                    replicantSession,
                    "Attempted to subscribe directly from an Area of Interest to a Dataset with internal visibility");
            return false;
        } else if (datasetAddress.hasDatasetRootId() && dataset.isTypeDataset()) {
            sendErrorAndClose(
                    replicantSession,
                    "Attempted to subscribe using a Dataset Address with an unexpected Dataset Root identifier");
            return false;
        } else if (!datasetAddress.hasDatasetRootId() && dataset.isInstanceDataset()) {
            sendErrorAndClose(
                    replicantSession,
                    "Attempted to subscribe using a Dataset Address without a required Dataset Root identifier");
            return false;
        } else {
            return validateDatasetKey(replicantSession, dataset, datasetAddress);
        }
    }

    @SuppressWarnings("DuplicatedCode")
    private void onBulkSubscribe(@NonNull final ReplicantSession session, @NonNull final JsonObject request)
            throws IOException {
        final var datasetAddresses = extractDatasetAddresses(request);
        if (0 != datasetAddresses.length) {
            final var datasetId = datasetAddresses[0].datasetId();

            final var dataset = getDataset(datasetId);
            for (final var datasetAddress : datasetAddresses) {
                if (!checkSubscribeRequest(session, dataset, datasetAddress)) {
                    return;
                }
                if (datasetAddress.datasetId() != datasetId) {
                    sendErrorAndClose(session, "Bulk subscribe included Dataset Addresses from multiple Datasets");
                    return;
                }
            }

            final var requestId = request.getInt(Messages.Common.REQUEST_ID);
            final var filterParameter = extractFilterParameter(dataset, request);
            _sessionManager.subscribe(session, requestId, Arrays.asList(datasetAddresses), filterParameter);
        }
    }

    @NonNull
    private DatasetAddress[] extractDatasetAddresses(@NonNull final JsonObject request) {
        final var datasetAddressDescriptors = request.getJsonArray(Messages.Common.DATASET_ADDRESSES);
        final var datasetAddressCount = datasetAddressDescriptors.size();
        final var datasetAddresses = new DatasetAddress[datasetAddressCount];
        for (var i = 0; i < datasetAddressCount; i++) {
            datasetAddresses[i] = DatasetAddress.parse(datasetAddressDescriptors.getString(i));
        }
        return datasetAddresses;
    }

    @Nullable
    private JsonObject extractFilterParameter(@NonNull final Dataset dataset, @NonNull final JsonObject request) {
        return dataset.isParameterFiltered()
                        && request.containsKey(Messages.Common.FILTER_PARAMETER)
                        && !request.isNull(Messages.Common.FILTER_PARAMETER)
                ? request.getJsonObject(Messages.Common.FILTER_PARAMETER)
                : null;
    }

    private void onUnsubscribe(@NonNull final ReplicantSession replicantSession, @NonNull final JsonObject request)
            throws IOException {
        final var datasetAddress = DatasetAddress.parse(request.getString(Messages.Common.DATASET_ADDRESS));
        final var dataset = getDataset(datasetAddress.datasetId());
        if (checkUnsubscribeRequest(replicantSession, dataset, datasetAddress)) {
            final var requestId = request.getInt(Messages.Common.REQUEST_ID);
            _sessionManager.unsubscribe(replicantSession, requestId, Collections.singletonList(datasetAddress));
        }
    }

    @SuppressWarnings("DuplicatedCode")
    private void onBulkUnsubscribe(@NonNull final ReplicantSession session, @NonNull final JsonObject request)
            throws IOException {
        final var datasetAddresses = extractDatasetAddresses(request);
        if (0 != datasetAddresses.length) {
            final var datasetId = datasetAddresses[0].datasetId();

            final var dataset = getDataset(datasetId);
            for (final var datasetAddress : datasetAddresses) {
                if (!checkUnsubscribeRequest(session, dataset, datasetAddress)) {
                    return;
                } else if (datasetAddress.datasetId() != datasetId) {
                    sendErrorAndClose(session, "Bulk unsubscribe included Dataset Addresses from multiple Datasets");
                    return;
                }
            }

            final var requestId = request.getInt(Messages.Common.REQUEST_ID);
            _sessionManager.unsubscribe(session, requestId, Arrays.asList(datasetAddresses));
        }
    }

    private boolean checkUnsubscribeRequest(
            @NonNull final ReplicantSession replicantSession,
            @NonNull final Dataset dataset,
            @NonNull final DatasetAddress datasetAddress)
            throws IOException {
        if (!dataset.getVisibility().permitsAreaOfInterestOrigin()) {
            sendErrorAndClose(
                    replicantSession,
                    "Attempted to unsubscribe directly from an Area of Interest to a Dataset with internal visibility");
            return false;
        } else if (datasetAddress.hasDatasetRootId() && dataset.isTypeDataset()) {
            sendErrorAndClose(
                    replicantSession,
                    "Attempted to unsubscribe using a Dataset Address with an unexpected Dataset Root identifier");
            return false;
        } else if (!datasetAddress.hasDatasetRootId() && dataset.isInstanceDataset()) {
            sendErrorAndClose(
                    replicantSession,
                    "Attempted to unsubscribe using a Dataset Address without a required Dataset Root identifier");
            return false;
        } else {
            return validateDatasetKey(replicantSession, dataset, datasetAddress);
        }
    }

    private boolean validateDatasetKey(
            @NonNull final ReplicantSession session,
            @NonNull final Dataset dataset,
            @NonNull final DatasetAddress datasetAddress)
            throws IOException {
        final boolean hasDatasetKey = null != datasetAddress.datasetKey();
        if (dataset.isKeyed()) {
            if (!hasDatasetKey) {
                sendErrorAndClose(session, "Attempted to use a Dataset Address without a required Dataset Key");
                return false;
            } else {
                return true;
            }
        } else if (hasDatasetKey) {
            sendErrorAndClose(session, "Attempted to use a Dataset Address with an unexpected Dataset Key");
            return false;
        } else {
            return true;
        }
    }

    @Nullable
    private ReplicantSession findReplicantSession(@NonNull final Session session) {
        try {
            final var replicantSessionId = session.getId();
            return _sessionManager.getSession(replicantSessionId);
        } catch (final Throwable ignored) {
            // This is sometimes called from onClose after the application has already been
            // un-deployed but the websockets have not completed closing. In this scenario
            // the toolkit would generate an exception. We just capture the exception and
            // return null to allow normal shutdown to occur without a log storm.
            return null;
        }
    }

    @NonNull
    private ReplicantSession getReplicantSession(@NonNull final Session session) {
        final var replicantSession = findReplicantSession(session);
        if (null != replicantSession) {
            return replicantSession;
        } else {
            throw new IllegalStateException(
                    "Unable to locate Replicant Session for WebSocket Session ID " + session.getId());
        }
    }

    @OnError
    public void onError(@NonNull final Session session, @NonNull final Throwable error) throws IOException {
        if (LOG.isLoggable(Level.INFO)) {
            LOG.log(Level.INFO, "Error on WebSocket Session " + session.getId(), error);
        }

        sendErrorAndClose(session, error.toString());
    }

    private void sendErrorAndClose(@NonNull final ReplicantSession session, @NonNull final String message)
            throws IOException {
        sendErrorAndClose(session.getWebSocketSession(), message);
    }

    private void sendErrorAndClose(@NonNull final Session session, @NonNull final String message) throws IOException {
        if (session.isOpen()) {
            WebSocketUtil.sendText(session, JsonEncoder.encodeErrorMessage(message));
            session.close(new CloseReason(CloseReason.CloseCodes.UNEXPECTED_CONDITION, "Unexpected error"));
        }
        final var replicantSession = findReplicantSession(session);
        if (null != replicantSession) {
            closeReplicantSession(replicantSession);
        }
    }

    @OnClose
    public void onClose(@NonNull final Session session) {
        final var replicantSession = findReplicantSession(session);
        if (null == replicantSession) {
            LOG.log(
                    Level.FINE,
                    () -> "Closing WebSocket Session " + session.getId()
                            + " but no Replicant Session found. This can occur except during "
                            + "application undeploy or when the session has errored.");
        } else {
            LOG.log(
                    Level.FINE,
                    () -> "Closing WebSocket Session " + session.getId() + " for Replicant Session ID "
                            + replicantSession.getReplicantSessionId());
            closeReplicantSession(replicantSession);
        }
    }

    @NonNull
    private Dataset getDataset(final int datasetId) {
        return _sessionManager.getSystemSchema().getDataset(datasetId);
    }

    private void closeWithError(
            @NonNull final ReplicantSession replicantSession,
            @NonNull final String reason,
            @NonNull final String message) {
        WebSocketUtil.sendText(replicantSession.getWebSocketSession(), message);
        replicantSession.close(new CloseReason(CloseReason.CloseCodes.UNEXPECTED_CONDITION, reason));
        closeReplicantSession(replicantSession);
    }

    private void closeReplicantSession(@NonNull final ReplicantSession replicantSession) {
        _replicantSessionRemovedEvent.fire(new ReplicantSessionRemoved(replicantSession.getReplicantSessionId()));
        _sessionManager.invalidateSession(replicantSession);
    }
}
