/**
 * This file provides the @defines for Replicant configuration options.
 * See ReplicantConfig.java for details.
 */
goog.module('replicant');
goog.module.declareLegacyNamespace();

const {addSystemPropertyFromGoogDefine} = goog.require('jre');

/** @define {string} */
const environment = goog.define('replicant.environment', 'production');
addSystemPropertyFromGoogDefine('replicant.environment', environment);

/** @define {string} */
const checkInvariants = goog.define('replicant.check_invariants', 'false');
addSystemPropertyFromGoogDefine('replicant.check_invariants', checkInvariants);

/** @define {string} */
const checkApiInvariants =
    goog.define('replicant.check_api_invariants', 'false');
addSystemPropertyFromGoogDefine(
    'replicant.check_api_invariants', checkApiInvariants);

/** @define {string} */
const enableNames = goog.define('replicant.enable_names', 'false');
addSystemPropertyFromGoogDefine('replicant.enable_names', enableNames);

/** @define {string} */
const enableZones = goog.define('replicant.enable_zones', 'false');
addSystemPropertyFromGoogDefine('replicant.enable_zones', enableZones);

/** @define {string} */
const enableSpies = goog.define('replicant.enable_spies', 'false');
addSystemPropertyFromGoogDefine('replicant.enable_spies', enableSpies);

/** @define {string} */
const validateChangeSetOnRead =
    goog.define('replicant.validateChangeSetOnRead', 'false');
addSystemPropertyFromGoogDefine(
    'replicant.validateChangeSetOnRead', validateChangeSetOnRead);

/** @define {string} */
const validateReplicasAfterMessageProcessing =
    goog.define('replicant.validateReplicasAfterMessageProcessing', 'false');
addSystemPropertyFromGoogDefine(
    'replicant.validateReplicasAfterMessageProcessing',
    validateReplicasAfterMessageProcessing);

/** @define {string} */
const logger = goog.define('replicant.logger', 'console');
addSystemPropertyFromGoogDefine('replicant.logger', logger);

exports = {
  check_api_invariants: checkApiInvariants,
  check_invariants: checkInvariants,
  enable_names: enableNames,
  enable_spies: enableSpies,
  enable_zones: enableZones,
  environment,
  logger,
  validate_change_set_on_read: validateChangeSetOnRead,
  validate_replicas_after_message_processing:
      validateReplicasAfterMessageProcessing,
};
