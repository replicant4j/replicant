package replicant.shared;

import org.jspecify.annotations.NonNull;

public final class Messages {
    private Messages() {}

    /**
     * Types of Server to Client messages.
     */
    public static final class S2C_Type {
        @NonNull
        public static final String UPDATE = "update";

        @NonNull
        public static final String USE_CACHE = "use-cache";

        @NonNull
        public static final String SESSION_CREATED = "session-created";

        @NonNull
        public static final String OK = "ok";

        @NonNull
        public static final String MALFORMED_MESSAGE = "malformed-message";

        @NonNull
        public static final String UNKNOWN_REQUEST_TYPE = "unknown-request-type";

        @NonNull
        public static final String ERROR = "error";

        private S2C_Type() {}
    }

    /**
     * Types of Client to Server messages.
     */
    public static final class C2S_Type {
        @NonNull
        public static final String AUTH = "auth";

        @NonNull
        public static final String ETAGS = "etags";

        @NonNull
        public static final String PING = "ping";

        @NonNull
        public static final String SUB = "sub";

        @NonNull
        public static final String UNSUB = "unsub";

        @NonNull
        public static final String BULK_SUB = "bulk-sub";

        @NonNull
        public static final String BULK_UNSUB = "bulk-unsub";

        @NonNull
        public static final String EXEC = "exec";

        private C2S_Type() {}
    }

    public static final class Common {
        @NonNull
        public static final String TYPE = "type";

        @NonNull
        public static final String REQUEST_ID = "requestId";

        @NonNull
        public static final String CHANNEL = "channel";

        @NonNull
        public static final String COMMAND = "command";

        private Common() {}
    }

    public static final class S2C_Common {
        @NonNull
        public static final String SESSION_ID = "sessionId";

        @NonNull
        public static final String ETAG = "etag";

        @NonNull
        public static final String MESSAGE = "message";

        private S2C_Common() {}
    }

    public static final class Update {
        public static final char CHANNEL_ACTION_ADD = '+';
        public static final char CHANNEL_ACTION_REMOVE = '-';
        public static final char CHANNEL_ACTION_UPDATE = '=';
        // Delete indicates the instance channel has been deleted and will never be a valid channel to subscribe to.
        public static final char CHANNEL_ACTION_DELETE = '!';

        @NonNull
        public static final String CHANGES = "changes";

        @NonNull
        public static final String ENTITY_ID = "id";

        @NonNull
        public static final String DATA = "data";

        @NonNull
        public static final String FILTERED_CHANNEL_ACTIONS = "fchannels";

        @NonNull
        public static final String CHANNEL_ACTIONS = "channels";

        @NonNull
        public static final String RESPONSE = "response";

        @NonNull
        public static final String CHANNELS = "channels";

        @NonNull
        public static final String FILTER = "filter";

        private Update() {}
    }

    public static final class Etags {
        @NonNull
        public static final String ETAGS = "etags";

        private Etags() {}
    }

    public static final class Auth {
        @NonNull
        public static final String TOKEN = "token";

        private Auth() {}
    }

    public static final class Exec {
        @NonNull
        public static final String PAYLOAD = "payload";

        private Exec() {}
    }
}
