goog.module('replicant.j2cl.TestBuild');

const Replicant = goog.require('replicant.Replicant');
const ReplicantContext = goog.require('replicant.ReplicantContext');
const ReplicantLogger = goog.require('replicant.ReplicantLogger');
const TransportContextImpl = goog.require('replicant.TransportContextImpl');

goog.exportSymbol('replicant.j2cl.Replicant', Replicant);
goog.exportSymbol('replicant.j2cl.ReplicantContext', ReplicantContext);
goog.exportSymbol('replicant.j2cl.ReplicantLogger', ReplicantLogger);
goog.exportSymbol('replicant.j2cl.TransportContextImpl', TransportContextImpl);
