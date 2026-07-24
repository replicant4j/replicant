package replicant.server.ee;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import javax.annotation.Resource;
import javax.enterprise.concurrent.ManagedExecutorService;
import javax.enterprise.concurrent.ManagedScheduledExecutorService;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.transaction.TransactionSynchronizationRegistry;
import replicant.server.runtime.ReplicantSystem;

@ApplicationScoped
@SuppressWarnings("NullAway.Init")
public class ReplicantResources {
    @Resource(lookup = "replicant/concurrent/ManagedScheduledExecutorService")
    private ManagedScheduledExecutorService _managedScheduledExecutorService;

    @Resource(lookup = "replicant/concurrent/ManagedExecutorService")
    private ManagedExecutorService _managedExecutorService;

    @Resource(lookup = "replicant/broker/maxConcurrentDrainTasks")
    private Integer _maxConcurrentDrainTasks;

    @Resource(lookup = "replicant/broker/maxPacketsPerRun")
    private Integer _maxPacketsPerRun;

    @Resource(lookup = "replicant/broker/maxSessionsPerDrainTask")
    private Integer _maxSessionsPerDrainTask;

    @Resource
    private TransactionSynchronizationRegistry _transactionSynchronizationRegistry;

    @Produces
    @ReplicantSystem
    public TransactionSynchronizationRegistry transactionSynchronizationRegistry() {
        return _transactionSynchronizationRegistry;
    }

    @Produces
    @ReplicantSystem("ScheduledExecutorService")
    public ScheduledExecutorService scheduledExecutorService() {
        return _managedScheduledExecutorService;
    }

    @Produces
    @ReplicantSystem("ExecutorService")
    public ExecutorService executorService() {
        return _managedExecutorService;
    }

    @Produces
    @ReplicantSystem("broker/maxConcurrentDrainTasks")
    public Integer maxConcurrentDrainTasks() {
        return _maxConcurrentDrainTasks;
    }

    @Produces
    @ReplicantSystem("broker/maxPacketsPerRun")
    public Integer maxPacketsPerRun() {
        return _maxPacketsPerRun;
    }

    @Produces
    @ReplicantSystem("broker/maxSessionsPerDrainTask")
    public Integer maxSessionsPerDrainTask() {
        return _maxSessionsPerDrainTask;
    }
}
