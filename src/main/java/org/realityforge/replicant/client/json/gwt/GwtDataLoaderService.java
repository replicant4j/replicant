package org.realityforge.replicant.client.json.gwt;

import com.google.gwt.core.client.GWT;
import org.realityforge.replicant.client.AbstractDataLoaderService;
import org.realityforge.replicant.client.ChangeSet;
import org.realityforge.replicant.client.transport.ClientSession;

public abstract class GwtDataLoaderService<T extends ClientSession>
  extends AbstractDataLoaderService<T>
{
  @Override
  protected boolean shouldValidateOnLoad()
  {
    return !GWT.isProdMode();
  }

  @Override
  protected ChangeSet parseChangeSet( final String rawJsonData )
  {
    return JsoChangeSet.asChangeSet( rawJsonData );
  }
}
