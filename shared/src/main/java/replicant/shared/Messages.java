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
        public static final String USE_CACHED_DATASET = "use-cached-dataset";

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
        public static final String DATASET_CACHE_VERSIONS = "dataset-cache-versions";

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
        public static final String DATASET_ADDRESS = "datasetAddress";

        @NonNull
        public static final String COMMAND = "command";

        private Common() {}
    }

    public static final class S2C_Common {
        @NonNull
        public static final String SESSION_ID = "sessionId";

        @NonNull
        public static final String DATASET_CACHE_VERSION = "datasetCacheVersion";

        @NonNull
        public static final String MESSAGE = "message";

        private S2C_Common() {}
    }

    public static final class Update {
        public static final char SUBSCRIPTION_CHANGE_SUBSCRIBE = '+';
        public static final char SUBSCRIPTION_CHANGE_UNSUBSCRIBE = '-';
        public static final char SUBSCRIPTION_CHANGE_UPDATE = '=';
        public static final char SUBSCRIPTION_CHANGE_INVALIDATE_DATASET_ADDRESS = '!';

        @NonNull
        public static final String CHANGES = "changes";

        @NonNull
        public static final String ENTITY_ID = "id";

        @NonNull
        public static final String DATA = "data";

        @NonNull
        public static final String FILTER_PARAMETER_SUBSCRIPTION_CHANGES = "filterParameterSubscriptionChanges";

        @NonNull
        public static final String SUBSCRIPTION_CHANGES = "subscriptionChanges";

        @NonNull
        public static final String RESPONSE = "response";

        @NonNull
        public static final String DATASET_ADDRESSES = "datasetAddresses";

        @NonNull
        public static final String SUBSCRIPTION_CHANGE = "subscriptionChange";

        @NonNull
        public static final String FILTER_PARAMETER = "filterParameter";

        private Update() {}
    }

    public static final class DatasetCacheVersions {
        @NonNull
        public static final String DATASET_CACHE_VERSIONS = "datasetCacheVersions";

        private DatasetCacheVersions() {}
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
