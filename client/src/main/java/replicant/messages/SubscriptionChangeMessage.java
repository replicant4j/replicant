package replicant.messages;

import jsinterop.annotations.JsOverlay;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import replicant.shared.Messages;

/**
 * A message fragment defining a reported Subscription Change.
 */
@JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "Object")
@SuppressWarnings("NullAway.Init")
public class SubscriptionChangeMessage {
    @JsProperty(name = Messages.ChangeSet.SUBSCRIPTION_CHANGE)
    private String subscriptionChange;

    @JsProperty(name = Messages.Common.FILTER_PARAMETER)
    @Nullable
    private Object filterParameter;

    /**
     * Create a SubscriptionChangeMessage.
     *
     * @return the new SubscriptionChangeMessage.
     */
    @JsOverlay
    public static SubscriptionChangeMessage create(
            @NonNull final String subscriptionChange, @Nullable final Object filterParameter) {
        final SubscriptionChangeMessage message = new SubscriptionChangeMessage();
        message.subscriptionChange = subscriptionChange;
        message.filterParameter = filterParameter;
        return message;
    }

    private SubscriptionChangeMessage() {}

    /**
     * Return the Subscription Change descriptor.
     *
     * @return the Subscription Change descriptor.
     */
    @JsOverlay
    @NonNull
    public final String getSubscriptionChange() {
        return subscriptionChange;
    }

    /**
     * @return the Filter Parameter associated with the Subscription Change.
     */
    @Nullable
    @JsOverlay
    public final Object getFilterParameter() {
        return filterParameter;
    }
}
