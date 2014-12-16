package org.realityforge.replicant.client.json.gwt;

import org.realityforge.gwt.webpoller.client.WebPoller;

final class TestWebPoller
  extends WebPoller
{
  @Override
  protected void stopTimer()
  {
  }

  @Override
  protected boolean isTimerActive()
  {
    return false;
  }

  @Override
  protected void startErrorTimer()
  {
  }

  @Override
  protected void stopErrorTimer()
  {
  }

  @Override
  protected void startTimer()
  {
  }
}
