package org.realityforge.replicant.server.json;

/**
 * Constants used to build up JSON payload transmitted to the client.
 */
interface TransportConstants
{
  String SEQUENCE = "seq";
  String REQUEST_ID = "requestId";
  String ETAG = "etag";
  String CHANGES = "changes";
  String ENTITY_ID = "id";
  String DATA = "data";
  String CHANNEL_ACTIONS = "channels";
  String FILTERED_CHANNEL_ACTIONS = "fchannels";
  String CHANNEL = "channel";
  String CHANNELS = "channels";
  String CHANNEL_FILTER = "filter";
}
