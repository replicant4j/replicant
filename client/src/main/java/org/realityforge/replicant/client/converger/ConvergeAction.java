package org.realityforge.replicant.client.converger;

enum ConvergeAction
{
  /// The submission has been added to the AOI queue
  SUBMITTED_ADD,
  /// The submission has been added to the AOI queue
  SUBMITTED_UPDATE,
  /// The submission has been added to the AOI queue, and can't be grouped
  TERMINATE,
  /// The submission is already in progress, still waiting for a response
  IN_PROGRESS,
  /// Nothing was done, fully converged
  NO_ACTION
}
