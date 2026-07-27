package replicant.server.ee;

import static org.testng.Assert.*;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import replicant.server.ServerConstants;
import replicant.server.runtime.EntityChangeCandidateCacheUtil;
import replicant.server.runtime.TransactionSynchronizationRegistryUtil;

public class EntityChangeCandidateCacheUtilTest {
    @BeforeMethod
    public void setup() {
        RegistryUtil.bind();
    }

    @AfterMethod
    public void clearContext() {
        RegistryUtil.unbind();
    }

    @Test
    public void ensureCacheBehavesAsExpected() {
        assertNull(EntityChangeCandidateCacheUtil.lookupEntityChangeCandidateSet());

        // Now we force the creation of EntityChangeCandidateSet
        final var messageSet = EntityChangeCandidateCacheUtil.getEntityChangeCandidateSet(
                TransactionSynchronizationRegistryUtil.lookup());

        assertNotNull(messageSet);
        assertEquals(messageSet, EntityChangeCandidateCacheUtil.lookupEntityChangeCandidateSet());
        assertEquals(
                messageSet,
                EntityChangeCandidateCacheUtil.getEntityChangeCandidateSet(
                        TransactionSynchronizationRegistryUtil.lookup()));

        // Now we remove EntityChangeCandidateSet
        assertEquals(messageSet, EntityChangeCandidateCacheUtil.removeEntityChangeCandidateSet());
        assertNull(EntityChangeCandidateCacheUtil.lookupEntityChangeCandidateSet());

        // Duplicate remove returns null
        assertNull(EntityChangeCandidateCacheUtil.removeEntityChangeCandidateSet());
    }

    @Test
    public void clientEntityChangeCandidateSet() {
        assertNull(EntityChangeCandidateCacheUtil.lookupSessionChanges());

        // Now we force the creation of EntityChangeCandidateSet
        final var messageSet = EntityChangeCandidateCacheUtil.getSessionChanges();

        assertNotNull(messageSet);
        assertEquals(messageSet, EntityChangeCandidateCacheUtil.lookupSessionChanges());
        assertEquals(messageSet, EntityChangeCandidateCacheUtil.getSessionChanges());

        // Now we remove EntityChangeCandidateSet
        assertEquals(messageSet, EntityChangeCandidateCacheUtil.removeSessionChanges());
        assertNull(EntityChangeCandidateCacheUtil.lookupSessionChanges());

        // Duplicate remove returns null
        assertNull(EntityChangeCandidateCacheUtil.removeSessionChanges());

        // Ensure that it works with regular changes
        assertNull(EntityChangeCandidateCacheUtil.removeEntityChangeCandidateSet());
    }

    @Test(expectedExceptions = IllegalStateException.class)
    public void lookupOfResourceOutsideReplicationContext() {
        TransactionSynchronizationRegistryUtil.lookup().putResource(ServerConstants.REPLICATION_INVOCATION_KEY, null);

        EntityChangeCandidateCacheUtil.lookupSessionChanges();
    }
}
