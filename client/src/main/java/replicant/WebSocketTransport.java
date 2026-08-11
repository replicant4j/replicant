package replicant;

import java.util.Objects;
import jsinterop.annotations.JsFunction;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;
import jsinterop.base.Any;
import jsinterop.base.Js;
import jsinterop.base.JsPropertyMap;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import replicant.messages.ServerToClientMessage;
import replicant.shared.Messages;

public final class WebSocketTransport extends AbstractTransport {
    private static final int BROWSER_WEBSOCKET_CONNECTING = 0;
    private static final int BROWSER_WEBSOCKET_OPEN = 1;

    @NonNull
    private final WebSocketConfig _config;

    @Nullable
    private BrowserWebSocket _webSocket;

    public WebSocketTransport(@NonNull final WebSocketConfig config) {
        _config = Objects.requireNonNull(config);
    }

    @Override
    protected void doConnect() {
        _webSocket = new BrowserWebSocket(_config.getUrl());

        _webSocket.setOnMessage(this::handleMessageEvent);
        _webSocket.setOnError(e -> onError());
        _webSocket.setOnClose(e -> onDisconnect());
    }

    private void handleMessageEvent(@NonNull final BrowserMessageEvent e) {
        final Any data = e.data();
        if (null == data) {
            ReplicantLogger.log("WebSocket message has null data", null);
            onError();
        } else {
            try {
                final ServerToClientMessage message = tryParseMessage(data);
                if (null == message) {
                    onError();
                } else {
                    final String type = message.getType();
                    if (isKnownMessageType(type)) {
                        onMessageReceived(message);
                    } else {
                        ReplicantLogger.log("Unknown WebSocket message type: " + type, null);
                        onError();
                    }
                }
            } catch (final Throwable t) {
                ReplicantLogger.log("Failed to parse WebSocket message", t);
                onError();
            }
        }
    }

    private static boolean isKnownMessageType(@NonNull final String type) {
        return Messages.S2C_Type.CHANGE_SET.equals(type)
                || Messages.S2C_Type.USE_DATASET_CACHE_ENTRY.equals(type)
                || Messages.S2C_Type.SESSION_CREATED.equals(type)
                || Messages.S2C_Type.OK.equals(type)
                || Messages.S2C_Type.MALFORMED_MESSAGE.equals(type)
                || Messages.S2C_Type.UNKNOWN_REQUEST_TYPE.equals(type)
                || Messages.S2C_Type.ERROR.equals(type);
    }

    @Nullable
    private static ServerToClientMessage tryParseMessage(@NonNull final Any data) {
        final String kind = Js.typeof(data);
        Any parsed;
        if ("string".equals(kind)) {
            parsed = BrowserJson.parse(data.asString());
        } else {
            ReplicantLogger.log("WebSocket message incorrect type: " + kind, null);
            return null;
        }

        if (null == parsed) {
            ReplicantLogger.log("WebSocket message parsed to null", null);
            return null;
        } else {
            final JsPropertyMap<?> map = Js.asPropertyMap(parsed);
            if (null == map || !map.has("type")) {
                ReplicantLogger.log("WebSocket payload missing 'type' property", null);
                return null;
            } else {
                return parsed.cast();
            }
        }
    }

    @Override
    protected void doDisconnect() {
        if (null != _webSocket) {
            final int readyState = _webSocket.readyState();
            if (BROWSER_WEBSOCKET_OPEN == readyState) {
                _webSocket.close();
            } else if (BROWSER_WEBSOCKET_CONNECTING == readyState) {
                // It is an error to invoke close() on a socket that is not open, so defer the close until the
                // socket has opened.
                final BrowserWebSocket webSocket = _webSocket;
                webSocket.setOnOpen(e -> webSocket.close());
            }
            _webSocket = null;
        }
    }

    @Override
    protected void sendRemoteMessage(@NonNull final Object message) {
        _config.remote(() -> {
            // Attempts to perform a send can occur when there is no connection.
            // This typically happens when a previous request fails.
            if (null != _webSocket && BROWSER_WEBSOCKET_OPEN == _webSocket.readyState()) {
                _webSocket.send(BrowserJson.stringify(Js.asAny(message)));
            }
        });
    }

    @JsFunction
    private interface BrowserEventHandler {
        void onEvent(Object event);
    }

    @JsFunction
    private interface BrowserMessageHandler {
        void onMessage(@NonNull BrowserMessageEvent event);
    }

    @JsType(isNative = true, name = "MessageEvent", namespace = JsPackage.GLOBAL)
    private static final class BrowserMessageEvent {
        @Nullable
        @JsProperty(name = "data")
        native Any data();
    }

    @JsType(isNative = true, name = "WebSocket", namespace = JsPackage.GLOBAL)
    private static final class BrowserWebSocket {
        BrowserWebSocket(String url) {}

        @JsProperty(name = "onmessage")
        native void setOnMessage(BrowserMessageHandler handler);

        @JsProperty(name = "onerror")
        native void setOnError(BrowserEventHandler handler);

        @JsProperty(name = "onclose")
        native void setOnClose(BrowserEventHandler handler);

        @JsProperty(name = "onopen")
        native void setOnOpen(BrowserEventHandler handler);

        @JsProperty(name = "readyState")
        native int readyState();

        native void close();

        native void send(String data);
    }
}
