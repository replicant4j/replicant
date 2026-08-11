goog.module('replicant.j2cl.BuildTest');

const BrowserInteropLinker = goog.require('replicant.j2cl.BrowserInteropLinker');
const Replicant = goog.require('replicant.Replicant');
const ReplicantContext = goog.require('replicant.ReplicantContext');
const SharedConstants = goog.require('replicant.shared.SharedConstants');

goog.exportSymbol('replicant.j2cl.Replicant', Replicant);
goog.exportSymbol('replicant.j2cl.ReplicantContext', ReplicantContext);
goog.exportSymbol('replicant.j2cl.BrowserInteropLinker.link', BrowserInteropLinker.link);
goog.exportSymbol('replicant.j2cl.SharedConstants', SharedConstants);
