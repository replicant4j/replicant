package replicant.shared;

import javax.annotation.Nonnull;

public final class Messages
{
  /**
   * Types of Server to Client messages.
   */
  public static final class S2C_Type
  {
    @Nonnull
    public static final String UPDATE = "update";
    @Nonnull
    public static final String USE_CACHE = "use-cache";
    @Nonnull
    public static final String SESSION_CREATED = "session-created";
    @Nonnull
    public static final String OK = "ok";
    @Nonnull
    public static final String MALFORMED_MESSAGE = "malformed-message";
    @Nonnull
    public static final String UNKNOWN_REQUEST_TYPE = "unknown-request-type";
    @Nonnull
    public static final String ERROR = "error";

    private S2C_Type()
    {
    }
  }

  /**
   * Types of Client to Server messages.
   */
  public static final class C2S_Type
  {
    @Nonnull
    public static final String AUTH = "auth";
    @Nonnull
    public static final String ETAGS = "etags";
    @Nonnull
    public static final String PING = "ping";
    @Nonnull
    public static final String SUB = "sub";
    @Nonnull
    public static final String UNSUB = "unsub";
    @Nonnull
    public static final String BULK_SUB = "bulk-sub";
    @Nonnull
    public static final String BULK_UNSUB = "bulk-unsub";
    @Nonnull
    public static final String EXEC = "exec";

    private C2S_Type()
    {
    }
  }

  public static final class Common
  {
    @Nonnull
    public static final String TYPE = "type";
    @Nonnull
    public static final String REQUEST_ID = "requestId";
    @Nonnull
    public static final String CHANNEL = "channel";
    @Nonnull
    public static final String COMMAND = "command";

    private Common()
    {
    }
  }

  public static final class S2C_Common
  {
    @Nonnull
    public static final String SESSION_ID = "sessionId";
    @Nonnull
    public static final String ETAG = "etag";
    @Nonnull
    public static final String MESSAGE = "message";

    private S2C_Common()
    {
    }
  }

  public static final class Update
  {
    public static final char CHANNEL_ACTION_ADD = '+';
    public static final char CHANNEL_ACTION_REMOVE = '-';
    public static final char CHANNEL_ACTION_UPDATE = '=';
    // Delete indicates the instance channel has been deleted and will never be a valid channel to subscribe to.
    public static final char CHANNEL_ACTION_DELETE = '!';
    @Nonnull
    public static final String CHANGES = "changes";
    @Nonnull
    public static final String ENTITY_ID = "id";
    @Nonnull
    public static final String DATA = "data";
    @Nonnull
    public static final String FILTERED_CHANNEL_ACTIONS = "fchannels";
    @Nonnull
    public static final String CHANNEL_ACTIONS = "channels";
    @Nonnull
    public static final String RESPONSE = "response";
    @Nonnull
    public static final String CHANNELS = "channels";
    @Nonnull
    public static final String FILTER = "filter";

    private Update()
    {
    }
  }

  public static final class Etags
  {
    @Nonnull
    public static final String ETAGS = "etags";

    private Etags()
    {
    }
  }


  public static final  class Exec
  {
    @Nonnull
    public static final String PAYLOAD = "payload";

    private Exec()
    {
    }
  }
}
