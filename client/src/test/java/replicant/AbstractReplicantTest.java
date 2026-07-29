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
        connector.onReplicantSessionCreated(ValueUtil.randomString());
        final Connection connection = connector.ensureConnection();
        connection.setReplicantSessionId(ValueUtil.randomString());
        return connection;
    }

    @NonNull
    final ReplicaEntry findOrCreateReplicaEntry(@NonNull final Class<?> type, final int id) {
        return safeAction(() -> Replicant.context()
                .getReplicaRegistry()
                .findOrCreateReplicaEntry(
                        Replicant.areNamesEnabled() ? type.getSimpleName() + "/" + id : null, type, id));
    }

    @NonNull
    protected final Subscription createSubscription(
            @NonNull final DatasetAddress datasetAddress,
            @Nullable final Object filterParameter,
            @NonNull final SubscriptionMode mode) {
        return safeAction(() ->
                Replicant.context().getSubscriptionService().createSubscription(datasetAddress, filterParameter, mode));
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
    protected final SystemSchema newSystemSchema() {
        return newSystemSchema(ValueUtil.randomInt());
    }

    @NonNull
    final SystemSchema newSystemSchema(final int systemSchemaId) {
        final Dataset[] datasets = new Dataset[0];
        final EntityType[] entityTypes = new EntityType[0];
        return new SystemSchema(
                systemSchemaId,
                replicant.Replicant.areNamesEnabled() ? ValueUtil.randomString() : null,
                datasets,
                entityTypes);
    }

    @NonNull
    final Connector createConnector() {
        return createConnector(newSystemSchema(1));
    }

    @NonNull
    final Connector createConnector(@NonNull final SystemSchema systemSchema) {
        return (Connector) Replicant.context().registerConnector(systemSchema, mock(Transport.class));
    }

    @NonNull
    final Connection createConnection() {
        final Connection connection = Connection.create(createConnector());
        connection.setReplicantSessionId(ValueUtil.randomString());
        return connection;
    }
}
