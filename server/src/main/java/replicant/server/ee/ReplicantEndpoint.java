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
import replicant.server.ChannelAddress;
import replicant.server.json.JsonEncoder;
import replicant.server.transport.ChannelMetaData;
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
                    "Opening WebSocket Session " + session.getId() + " for replicant session "
                            + getReplicantSession(session).getId());
        }

        _replicantSessionAddedEventEvent.fire(new ReplicantSessionAdded(newReplicantSession.getId()));

        WebSocketUtil.sendText(session, JsonEncoder.encodeSessionCreatedMessage(newReplicantSession.getId()));
    }

    @OnMessage
    @Transactional
    public void command(@NonNull final Session session, @NonNull final String message) throws IOException {
        final ReplicantSession replicantSession;
        try {
            replicantSession = getReplicantSession(session);
        } catch (final Throwable ignored) {
            sendErrorAndClose(session, "Unable to locate associated replicant session");
            return;
        }
        if (LOG.isLoggable(Level.FINE)) {
            LOG.log(
                    Level.FINE,
                    "Message on WebSocket Session " + session.getId() + " for replicant session "
                            + getReplicantSession(session).getId() + ". Message:\n" + message);
        }
        final JsonObject command;
        final String type;
        final int requestId;
        try {
            command = Json.createReader(new StringReader(message)).readObject();
            type = command.getString(Messages.Common.TYPE);
            requestId = command.getInt(Messages.Common.REQUEST_ID);
        } catch (final Throwable ignored) {
            if (!runIfValid(replicantSession, () -> onMalformedMessage(replicantSession, message))) {
                sendErrorAndClose(session, "Replicant session not authorized");
            }
            return;
        }
        if (!runIfValid(replicantSession, () -> processCommand(replicantSession, command, type, requestId))) {
            sendErrorAndClose(session, "Replicant session not authorized");
        }
    }

    private void processCommand(
            @NonNull final ReplicantSession replicantSession,
            @NonNull final JsonObject command,
            @NonNull final String type,
            final int requestId)
            throws IOException {
        try {
            //noinspection IfCanBeSwitch
            if (Messages.C2S_Type.EXEC.equals(type)) {
                _sessionManager.execCommand(
                        replicantSession,
                        command.getString(Messages.Common.COMMAND),
                        command.getInt(Messages.Common.REQUEST_ID),
                        command.containsKey(Messages.Exec.PAYLOAD)
                                ? command.getJsonObject(Messages.Exec.PAYLOAD)
                                : null);
            } else if (Messages.C2S_Type.ETAGS.equals(type)) {
                onETags(replicantSession, command);
            } else if (Messages.C2S_Type.PING.equals(type)) {
                sendOk(replicantSession.getWebSocketSession(), requestId);
            } else if (Messages.C2S_Type.SUB.equals(type)) {
                onSubscribe(replicantSession, command);
            } else if (Messages.C2S_Type.BULK_SUB.equals(type)) {
                onBulkSubscribe(replicantSession, command);
            } else if (Messages.C2S_Type.UNSUB.equals(type)) {
                onUnsubscribe(replicantSession, command);
            } else if (Messages.C2S_Type.BULK_UNSUB.equals(type)) {
                onBulkUnsubscribe(replicantSession, command);
            } else {
                onUnknownType(replicantSession, command);
            }
            _replicantSessionUpdatedEvent.fire(new ReplicantSessionUpdated(replicantSession.getId()));
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

    private void onETags(@NonNull final ReplicantSession session, @NonNull final JsonObject command) {
        final var eTags = new HashMap<ChannelAddress, String>();
        for (final var entry : command.getJsonObject(Messages.Etags.ETAGS).entrySet()) {
            final var address = ChannelAddress.parse(entry.getKey());
            final var eTag = ((JsonString) entry.getValue()).getString();
            eTags.put(address, eTag);
        }
        _sessionManager.setETags(session, eTags);

        sendOk(session.getWebSocketSession(), command.getInt(Messages.Common.REQUEST_ID));
    }

    private void onMalformedMessage(@NonNull final ReplicantSession replicantSession, @NonNull final String message) {
        closeWithError(replicantSession, "Malformed message", JsonEncoder.encodeMalformedMessageMessage(message));
    }

    private void onUnknownType(@NonNull final ReplicantSession replicantSession, @NonNull final JsonObject command) {
        closeWithError(replicantSession, "Unknown request type", JsonEncoder.encodeUnknownRequestType(command));
    }

    private void onSubscribe(@NonNull final ReplicantSession replicantSession, @NonNull final JsonObject command)
            throws IOException {
        final var address = ChannelAddress.parse(command.getString(Messages.Common.CHANNEL));
        final var channelMetaData = getChannelMetaData(address.channelId());
        if (checkSubscribeRequest(replicantSession, channelMetaData, address)) {
            final var requestId = command.getInt(Messages.Common.REQUEST_ID);
            final var filter = extractFilter(channelMetaData, command);
            _sessionManager.subscribe(replicantSession, requestId, Collections.singletonList(address), filter);
        }
    }

    private boolean checkSubscribeRequest(
            @NonNull final ReplicantSession replicantSession,
            @NonNull final ChannelMetaData channelMetaData,
            @NonNull final ChannelAddress address)
            throws IOException {
        if (!channelMetaData.isExternal()) {
            sendErrorAndClose(replicantSession, "Attempted to subscribe to internal-only channel");
            return false;
        } else if (address.hasRootId() && channelMetaData.isTypeGraph()) {
            sendErrorAndClose(replicantSession, "Attempted to subscribe to type channel with instance data");
            return false;
        } else if (!address.hasRootId() && channelMetaData.isInstanceGraph()) {
            sendErrorAndClose(replicantSession, "Attempted to subscribe to instance channel without instance data");
            return false;
        } else {
            return validateFilterInstanceId(replicantSession, channelMetaData, address);
        }
    }

    @SuppressWarnings("DuplicatedCode")
    private void onBulkSubscribe(@NonNull final ReplicantSession session, @NonNull final JsonObject command)
            throws IOException {
        final var addresses = extractChannels(command);
        if (0 != addresses.length) {
            final var channelId = addresses[0].channelId();

            final var channelMetaData = getChannelMetaData(channelId);
            for (final var address : addresses) {
                if (!checkSubscribeRequest(session, channelMetaData, address)) {
                    return;
                }
                if (address.channelId() != channelId) {
                    sendErrorAndClose(session, "Bulk channel subscribe included addresses from multiple channels");
                    return;
                }
            }

            final var requestId = command.getInt(Messages.Common.REQUEST_ID);
            final var filter = extractFilter(channelMetaData, command);
            _sessionManager.subscribe(session, requestId, Arrays.asList(addresses), filter);
        }
    }

    @NonNull
    private ChannelAddress[] extractChannels(@NonNull final JsonObject command) {
        final var channels = command.getJsonArray(Messages.Update.CHANNELS);
        final var channelCount = channels.size();
        final var addresses = new ChannelAddress[channelCount];
        for (var i = 0; i < channelCount; i++) {
            addresses[i] = ChannelAddress.parse(channels.getString(i));
        }
        return addresses;
    }

    @Nullable
    private JsonObject extractFilter(
            @NonNull final ChannelMetaData channelMetaData, @NonNull final JsonObject command) {
        return channelMetaData.requiresFilterParameter()
                        && command.containsKey(Messages.Update.FILTER)
                        && !command.isNull(Messages.Update.FILTER)
                ? command.getJsonObject(Messages.Update.FILTER)
                : null;
    }

    private void onUnsubscribe(@NonNull final ReplicantSession replicantSession, @NonNull final JsonObject command)
            throws IOException {
        final var address = ChannelAddress.parse(command.getString(Messages.Common.CHANNEL));
        final var channelMetaData = getChannelMetaData(address.channelId());
        if (checkUnsubscribeRequest(replicantSession, channelMetaData, address)) {
            final var requestId = command.getInt(Messages.Common.REQUEST_ID);
            _sessionManager.unsubscribe(replicantSession, requestId, Collections.singletonList(address));
        }
    }

    @SuppressWarnings("DuplicatedCode")
    private void onBulkUnsubscribe(@NonNull final ReplicantSession session, @NonNull final JsonObject command)
            throws IOException {
        final var addresses = extractChannels(command);
        if (0 != addresses.length) {
            final var channelId = addresses[0].channelId();

            final var channelMetaData = getChannelMetaData(channelId);
            for (final var address : addresses) {
                if (!checkUnsubscribeRequest(session, channelMetaData, address)) {
                    return;
                } else if (address.channelId() != channelId) {
                    sendErrorAndClose(session, "Bulk channel unsubscribe included addresses from multiple channels");
                    return;
                }
            }

            final var requestId = command.getInt(Messages.Common.REQUEST_ID);
            _sessionManager.unsubscribe(session, requestId, Arrays.asList(addresses));
        }
    }

    private boolean checkUnsubscribeRequest(
            @NonNull final ReplicantSession replicantSession,
            @NonNull final ChannelMetaData channelMetaData,
            @NonNull final ChannelAddress address)
            throws IOException {
        if (!channelMetaData.isExternal()) {
            sendErrorAndClose(replicantSession, "Attempted to unsubscribe from internal-only channel");
            return false;
        } else if (address.hasRootId() && channelMetaData.isTypeGraph()) {
            sendErrorAndClose(replicantSession, "Attempted to unsubscribe from type channel with instance data");
            return false;
        } else if (!address.hasRootId() && channelMetaData.isInstanceGraph()) {
            sendErrorAndClose(replicantSession, "Attempted to unsubscribe from instance channel without instance data");
            return false;
        } else {
            return validateFilterInstanceId(replicantSession, channelMetaData, address);
        }
    }

    private boolean validateFilterInstanceId(
            @NonNull final ReplicantSession session,
            @NonNull final ChannelMetaData channelMetaData,
            @NonNull final ChannelAddress address)
            throws IOException {
        final boolean hasInstanceId = null != address.filterInstanceId();
        if (channelMetaData.requiresFilterInstanceId()) {
            if (!hasInstanceId) {
                sendErrorAndClose(session, "Attempted to use instanced channel without filter instance id");
                return false;
            } else {
                return true;
            }
        } else if (hasInstanceId) {
            sendErrorAndClose(session, "Attempted to use non-instanced channel with filter instance id");
            return false;
        } else {
            return true;
        }
    }

    @Nullable
    private ReplicantSession findReplicantSession(@NonNull final Session session) {
        try {
            return _sessionManager.getSession(session.getId());
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
                    "Unable to locate ReplicantSession for WebSocket session " + session.getId());
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
                            + " but no replicant session found. This can occur except during "
                            + "application undeploy or when the session has errored.");
        } else {
            LOG.log(
                    Level.FINE,
                    () -> "Closing WebSocket Session " + session.getId() + " for replicant session "
                            + replicantSession.getId());
            closeReplicantSession(replicantSession);
        }
    }

    @NonNull
    private ChannelMetaData getChannelMetaData(final int channelId) {
        return _sessionManager.getSchemaMetaData().getChannelMetaData(channelId);
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
        _replicantSessionRemovedEvent.fire(new ReplicantSessionRemoved(replicantSession.getId()));
        _sessionManager.invalidateSession(replicantSession);
    }
}
