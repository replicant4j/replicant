package org.realityforge.replicant.client.transport;

import java.util.LinkedList;
import javax.annotation.Nonnull;

/**
 * Abstract representation of client session.
 */
public abstract class ClientSession
{
  private final String _sessionID;
  private final RequestManager _requestManager;

  /**
   * The set of data load actions that still need to have the json parsed.
   */
  private final LinkedList<DataLoadAction> _pendingActions = new LinkedList<>();
  /**
   * The set of data load actions that have their json parsed. They are inserted into
   * this list according to their sequence.
   */
  private final LinkedList<DataLoadAction> _parsedActions = new LinkedList<>();
  /**
   * Sometimes a data load action occurs that is not initiated by the server. These do not
   * typically need to be sequenced and are prioritized above other actions.
   */
  private final LinkedList<DataLoadAction> _oobActions = new LinkedList<>();

  private int _lastRxSequence;

  public ClientSession( @Nonnull final String sessionID )
  {
    _sessionID = sessionID;
    _requestManager = newRequestManager();
  }

  @Nonnull
  public String getSessionID()
  {
    return _sessionID;
  }

  @Nonnull
  public RequestManager getRequestManager()
  {
    return _requestManager;
  }

  final LinkedList<DataLoadAction> getPendingActions()
  {
    return _pendingActions;
  }

  final LinkedList<DataLoadAction> getParsedActions()
  {
    return _parsedActions;
  }

  final LinkedList<DataLoadAction> getOobActions()
  {
    return _oobActions;
  }

  protected RequestManager newRequestManager()
  {
    return new RequestManager();
  }

  public int getLastRxSequence()
  {
    return _lastRxSequence;
  }

  public void setLastRxSequence( final int lastRxSequence )
  {
    _lastRxSequence = lastRxSequence;
  }
}
