package replicant;

import arez.Disposable;
import java.util.Objects;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import replicant.messages.ServerToClientMessage;
import replicant.messages.SessionCreatedMessage;

final class TransportContextImpl implements TransportContext, Disposable {
    @NonNull
    private final Connector _connector;

    private boolean _disposed;

    TransportContextImpl(@NonNull final Connector connector) {
        _connector = Objects.requireNonNull(connector);
    }

    @Override
    public void dispose() {
        _disposed = true;
    }

    @Override
    public boolean isDisposed() {
        return _disposed;
    }

    @Override
    public int newRequestId(
            @Nullable final String name,
            final boolean synchronizationPointRequest,
            @Nullable final CommandResultHandler commandResultHandler) {
        assert isNotDisposed();
        return _connector
                .ensureConnection()
                .newRequest(name, synchronizationPointRequest, commandResultHandler)
                .getRequestId();
    }

    @Override
    public void onMessageReceived(@NonNull final ServerToClientMessage message) {
        if (isNotDisposed()) {
            if (SessionCreatedMessage.TYPE.equals(message.getType())) {
                _connector.onReplicantSessionCreated(((SessionCreatedMessage) message).getReplicantSessionId());
            } else {
                final boolean active = _connector.isSchedulerActive();
                final boolean paused = _connector.isSchedulerPaused();
                _connector.onMessageReceived(message);
                /*
                 * If the browser page is not visible then do all processing within the message handler callback
                 * to avoid suffering under the vagaries of the background timer throttling.
                 */
                if (!active && !paused && shouldProcessImmediatelyOnReceive()) {
                    //noinspection StatementWithEmptyBody
                    while (_connector.progressMessages()) {
                        // keep processing messages until done
                    }
                }
            }
        }
    }

    private static boolean shouldProcessImmediatelyOnReceive() {
        return !ReplicantConfig.shouldUseDocumentVisibility() || !"visible".equals(BrowserDocument.visibilityState());
    }

    @JsType(isNative = true, name = "globalThis.document", namespace = JsPackage.GLOBAL)
    private static final class BrowserDocument {
        @NonNull
        @JsProperty(name = "visibilityState")
        static native String visibilityState();

        private BrowserDocument() {}
    }

    @Override
    public void onError() {
        if (isNotDisposed()) {
            final ConnectorState state = _connector.getState();
            if (ConnectorState.CONNECTING == state) {
                _connector.onConnectFailure();
            } else if (ConnectorState.CONNECTED == state) {
                _connector.onMessageReadFailure();
            } else if (ConnectorState.DISCONNECTING == state) {
                _connector.onDisconnectFailure();
            }
        }
    }

    @Override
    public void onDisconnect() {
        if (isNotDisposed()) {
            _connector.onDisconnection();
        }
    }
}
