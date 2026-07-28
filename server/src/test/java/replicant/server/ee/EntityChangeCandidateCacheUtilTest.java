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
        final var entityChangeCandidateSet = EntityChangeCandidateCacheUtil.getEntityChangeCandidateSet(
                TransactionSynchronizationRegistryUtil.lookup());

        assertNotNull(entityChangeCandidateSet);
        assertEquals(entityChangeCandidateSet, EntityChangeCandidateCacheUtil.lookupEntityChangeCandidateSet());
        assertEquals(
                entityChangeCandidateSet,
                EntityChangeCandidateCacheUtil.getEntityChangeCandidateSet(
                        TransactionSynchronizationRegistryUtil.lookup()));

        // Now we remove EntityChangeCandidateSet
        assertEquals(entityChangeCandidateSet, EntityChangeCandidateCacheUtil.removeEntityChangeCandidateSet());
        assertNull(EntityChangeCandidateCacheUtil.lookupEntityChangeCandidateSet());

        // Duplicate remove returns null
        assertNull(EntityChangeCandidateCacheUtil.removeEntityChangeCandidateSet());
    }

    @Test
    public void initiatingSessionChangeSet() {
        assertNull(EntityChangeCandidateCacheUtil.lookupInitiatingSessionChangeSet());

        // Now we force the creation of the Initiating Session Change Set
        final var initiatingSessionChangeSet = EntityChangeCandidateCacheUtil.getInitiatingSessionChangeSet();

        assertNotNull(initiatingSessionChangeSet);
        assertEquals(initiatingSessionChangeSet, EntityChangeCandidateCacheUtil.lookupInitiatingSessionChangeSet());
        assertEquals(initiatingSessionChangeSet, EntityChangeCandidateCacheUtil.getInitiatingSessionChangeSet());

        // Now we remove the Initiating Session Change Set
        assertEquals(initiatingSessionChangeSet, EntityChangeCandidateCacheUtil.removeInitiatingSessionChangeSet());
        assertNull(EntityChangeCandidateCacheUtil.lookupInitiatingSessionChangeSet());

        // Duplicate remove returns null
        assertNull(EntityChangeCandidateCacheUtil.removeInitiatingSessionChangeSet());

        // Ensure that it works with regular changes
        assertNull(EntityChangeCandidateCacheUtil.removeEntityChangeCandidateSet());
    }

    @Test(expectedExceptions = IllegalStateException.class)
    public void lookupOfResourceOutsideReplicationInvocation() {
        TransactionSynchronizationRegistryUtil.lookup().putResource(ServerConstants.REPLICATION_INVOCATION_KEY, null);

        EntityChangeCandidateCacheUtil.lookupInitiatingSessionChangeSet();
    }
}
