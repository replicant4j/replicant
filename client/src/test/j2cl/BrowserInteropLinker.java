package replicant;

import jsinterop.annotations.JsType;
import jsinterop.base.Js;
import replicant.messages.DatasetCacheVersionsData;
import replicant.messages.ServerToClientMessage;
import replicant.spy.tools.ConsoleSpyEventProcessor;

@JsType(namespace = "replicant.j2cl")
public final class BrowserInteropLinker {
    public static void link(final Object value) {
        FilterParameterUtil.filterParameterToString(value);
        Js.<DatasetCacheVersionsData>uncheckedCast(value).datasetAddresses();

        final WebSocketTransport transport = new WebSocketTransport(WebSocketConfig.create(""));
        transport.doConnect();
        transport.doDisconnect();
        transport.sendRemoteMessage(value);

        WebStorageDatasetCacheService.install();
        final WebStorageDatasetCacheService cacheService = Js.uncheckedCast(value);
        cacheService.getDatasetAddresses(0);

        final TransportContextImpl transportContext = new TransportContextImpl(Js.uncheckedCast(value));
        transportContext.onMessageReceived(Js.<ServerToClientMessage>uncheckedCast(value));

        BrowserConsoleLinker.log(value);
    }

    private static final class BrowserConsoleLinker extends ConsoleSpyEventProcessor {
        private static void log(final Object value) {
            new BrowserConsoleLinker().handleUnhandledEvent(value);
        }
    }

    private BrowserInteropLinker() {}
}
