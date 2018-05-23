package replicant;

public final class TestData
{
  public static final ChannelSchema METADATA_CHANNEL =
    new ChannelSchema( 0, "MetaData", true, ChannelSchema.FilterType.NONE, true, true );
  public static final ChannelSchema EVENT_CHANNEL =
    new ChannelSchema( 1, "Event", false, ChannelSchema.FilterType.NONE, false, true );
  public static final ChannelSchema EVENTS_LIST_CHANNEL =
    new ChannelSchema( 2, "EventsList", true, ChannelSchema.FilterType.STATIC, false, true );
  public static final ChannelSchema PHYSICAL_UNIT_CHANNEL =
    new ChannelSchema( 3, "PhysicalUnit", false, ChannelSchema.FilterType.NONE, false, false );

  public static final EntitySchema EVENT_TYPE = new EntitySchema( 0, "EventType", Integer.class );
  public static final EntitySchema EVENT = new EntitySchema( 1, "Event", Integer.class );
  public static final EntitySchema PHYSICAL_UNIT = new EntitySchema( 2, "PhysicalUnit", Integer.class );

  public static final SystemSchema ROSE_SYSTEM = new SystemSchema( 1,
                                                                   "Rose",
                                                                   new ChannelSchema[]{ METADATA_CHANNEL,
                                                                                        EVENT_CHANNEL,
                                                                                        EVENTS_LIST_CHANNEL,
                                                                                        PHYSICAL_UNIT_CHANNEL },
                                                                   new EntitySchema[]{
                                                                     EVENT_TYPE,
                                                                     EVENT,
                                                                     PHYSICAL_UNIT
                                                                   } );

  private TestData()
  {
  }
}
