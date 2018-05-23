package replicant;

final class TestData
{
  private static final ChannelSchema METADATA_CHANNEL =
    new ChannelSchema( 0, "MetaData", true, ChannelSchema.FilterType.NONE, true, true );
  private static final ChannelSchema EVENT_CHANNEL =
    new ChannelSchema( 1, "Event", false, ChannelSchema.FilterType.NONE, false, true );
  private static final ChannelSchema EVENTS_LIST_CHANNEL =
    new ChannelSchema( 2, "EventsList", true, ChannelSchema.FilterType.STATIC, false, true );

  private static final EntitySchema EVENT_TYPE = new EntitySchema( 0, "EventType", Integer.class );
  private static final EntitySchema EVENT = new EntitySchema( 1, "Event", Integer.class );
  private static final EntitySchema PHYSICAL_UNIT = new EntitySchema( 2, "PhysicalUnit", Integer.class );

  static final SystemSchema ROSE_SYSTEM =
    new SystemSchema( 1,
                      "Rose",
                      new ChannelSchema[]{ METADATA_CHANNEL, EVENT_CHANNEL, EVENTS_LIST_CHANNEL },
                      new EntitySchema[]{ EVENT_TYPE, EVENT, PHYSICAL_UNIT } );

  private TestData()
  {
  }
}
