package replicant.server.transport;

public interface ReplicantChangeRecorder
{
  void recordEntityMessageForEntity( Object object, boolean isUpdate );
}
