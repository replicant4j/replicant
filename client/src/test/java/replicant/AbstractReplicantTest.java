package replicant;

import static org.mockito.Mockito.*;

import arez.testng.ActionWrapper;
import arez.testng.ArezTestSupport;
import java.lang.reflect.Field;
import java.util.Objects;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import zemeckis.ZemeckisTestUtil;

@Listeners(MessageCollector.class)
@ActionWrapper(enable = false)
public abstract class AbstractReplicantTest implements ArezTestSupport {
    @BeforeMethod
    @Override
    public void preTest() throws Exception {
        ZemeckisTestUtil.resetConfig(false);
        ArezTestSupport.super.preTest();
        ReplicantTestUtil.resetConfig(false);
        getProxyLogger().setLogger(new TestLogger());
    }

    @AfterMethod
    @Override
    public void postTest() {
        ZemeckisTestUtil.resetConfig(true);
        ReplicantTestUtil.resetConfig(true);
        ArezTestSupport.super.postTest();
    }

    @NonNull
    final Connection newConnection(@NonNull final Connector connector) {
        connector.onConnection(ValueUtil.randomString());
        final Connection connection = connector.ensureConnection();
        connection.setConnectionId(ValueUtil.randomString());
        return connection;
    }

    @NonNull
    final Entity findOrCreateEntity(@NonNull final Class<?> type, final int id) {
        return safeAction(() -> Replicant.context()
                .getEntityService()
                .findOrCreateEntity(Replicant.areNamesEnabled() ? type.getSimpleName() + "/" + id : null, type, id));
    }

    @NonNull
    protected final Subscription createSubscription(
            @NonNull final ChannelAddress address, @Nullable final Object filter, final boolean explicitSubscription) {
        return safeAction(() ->
                Replicant.context().getSubscriptionService().createSubscription(address, filter, explicitSubscription));
    }

    @NonNull
    final TestLogger getTestLogger() {
        return (TestLogger) Objects.requireNonNull(getProxyLogger().getLogger());
    }

    private ReplicantLogger.@NonNull ProxyLogger getProxyLogger() {
        return (ReplicantLogger.ProxyLogger) ReplicantLogger.getLogger();
    }

    @SuppressWarnings("NonJREEmulationClassesInClientCode")
    @NonNull
    private Field toField(@NonNull final Class<?> type, @NonNull final String fieldName) {
        Class<?> clazz = type;
        while (null != clazz && Object.class != clazz) {
            try {
                final Field field = clazz.getDeclaredField(fieldName);
                field.setAccessible(true);
                return field;
            } catch (final Throwable t) {
                clazz = clazz.getSuperclass();
            }
        }

        throw new AssertionError("Field '" + fieldName + "' not found in class hierarchy");
    }

    @SuppressWarnings("SameParameterValue")
    @Nullable
    final Object getFieldValue(@NonNull final Object object, @NonNull final String fieldName) {
        try {
            return toField(object.getClass(), fieldName).get(object);
        } catch (final Throwable t) {
            throw new AssertionError(t);
        }
    }

    @NonNull
    protected final TestSpyEventHandler registerTestSpyEventHandler() {
        final TestSpyEventHandler handler = new TestSpyEventHandler();
        Replicant.context().getSpy().addSpyEventHandler(handler);
        return handler;
    }

    @NonNull
    protected final SystemSchema newSchema() {
        return newSchema(ValueUtil.randomInt());
    }

    @NonNull
    final SystemSchema newSchema(final int schemaId) {
        final ChannelSchema[] channels = new ChannelSchema[0];
        final EntitySchema[] entities = new EntitySchema[0];
        return new SystemSchema(
                schemaId, replicant.Replicant.areNamesEnabled() ? ValueUtil.randomString() : null, channels, entities);
    }

    @NonNull
    final Connector createConnector() {
        return createConnector(newSchema(1));
    }

    @NonNull
    final Connector createConnector(@NonNull final SystemSchema schema) {
        return (Connector) Replicant.context().registerConnector(schema, mock(Transport.class));
    }

    @NonNull
    final Connection createConnection() {
        final Connection connection = Connection.create(createConnector());
        connection.setConnectionId(ValueUtil.randomString());
        return connection;
    }
}
