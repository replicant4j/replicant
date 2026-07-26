package replicant.messages;

import jsinterop.annotations.JsOverlay;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsType;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * A message fragment defining a Subscription lifecycle operation.
 */
@JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "Object")
@SuppressWarnings("NullAway.Init")
public class SubscriptionChangeMessage {
    private String subscriptionAction;

    @Nullable
    private Object filterParameter;

    /**
     * Create a SubscriptionChangeMessage.
     *
     * @return the new SubscriptionChangeMessage.
     */
    @JsOverlay
    public static SubscriptionChangeMessage create(
            @NonNull final String subscriptionAction, @Nullable final Object filterParameter) {
        final SubscriptionChangeMessage message = new SubscriptionChangeMessage();
        message.subscriptionAction = subscriptionAction;
        message.filterParameter = filterParameter;
        return message;
    }

    private SubscriptionChangeMessage() {}

    /**
     * Return the Subscription action descriptor.
     *
     * @return the Subscription action descriptor.
     */
    @JsOverlay
    @NonNull
    public final String getSubscriptionAction() {
        return subscriptionAction;
    }

    /**
     * @return the Filter Parameter associated with the Subscription action.
     */
    @Nullable
    @JsOverlay
    public final Object getFilterParameter() {
        return filterParameter;
    }
}
