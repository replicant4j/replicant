package org.realityforge.replicant.shared;

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
}
