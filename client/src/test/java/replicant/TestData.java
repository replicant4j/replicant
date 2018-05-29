package replicant;

final class TestData
{
  private static final ChannelSchema METADATA_CHANNEL =
    new ChannelSchema( 0, "MetaData", true, ChannelSchema.FilterType.NONE, null, true, true );

  static final SystemSchema ROSE_SYSTEM =
    new SystemSchema( 1,
                      "Rose",
                      new ChannelSchema[]{ METADATA_CHANNEL },
                      new EntitySchema[ 0 ] );

  private TestData()
  {
  }
}
