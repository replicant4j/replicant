package replicant;

final class TestData
{
  private static final ChannelSchema METADATA_CHANNEL =
    new ChannelSchema( 0, "MetaData", true, ChannelSchema.FilterType.NONE, null, true, true );
  private static final ChannelSchema EVENT_CHANNEL =
    new ChannelSchema( 1, "Event", false, ChannelSchema.FilterType.NONE, null, false, true );

  static final SystemSchema ROSE_SYSTEM =
    new SystemSchema( 1,
                      "Rose",
                      new ChannelSchema[]{ METADATA_CHANNEL, EVENT_CHANNEL },
                      new EntitySchema[ 0 ] );

  private TestData()
  {
  }
}
