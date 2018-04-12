package org.realityforge.replicant.client.react4j;

import javax.annotation.Nullable;
import react4j.annotations.Prop;
import react4j.core.Component;

/**
 * A base class that domgen extends when generating per-graph subscription components.
 */
public abstract class AbstractGraphSubscriptionComponent
  extends Component
{
  @Prop
  @Nullable
  protected abstract ReplicantSubscription.OnNotAskedCallback onNotAsked();

  @Prop
  @Nullable
  protected abstract ReplicantSubscription.OnLoadingCallback onLoading();

  @Prop
  @Nullable
  protected abstract ReplicantSubscription.OnFailureCallback onFailure();
}
