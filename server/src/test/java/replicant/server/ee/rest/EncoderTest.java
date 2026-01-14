package replicant.server.ee.rest;

import java.io.StringReader;
import java.io.StringWriter;
import java.net.URI;
import java.util.Collections;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonValue;
import javax.json.stream.JsonGenerator;
import javax.ws.rs.core.UriInfo;
import javax.websocket.Session;
import org.testng.annotations.Test;
import replicant.server.ChannelAddress;
import replicant.server.transport.ChannelMetaData;
import replicant.server.transport.ReplicantSession;
import replicant.server.transport.SchemaMetaData;
import replicant.server.transport.SubscriptionEntry;
import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

public final class EncoderTest
{
  @Test
  public void emitSession_withoutNetworkData()
  {
    final SchemaMetaData schema = newSchemaMetaData();
    final ReplicantSession session = newSession( "s1" );
    final UriInfo uri = newUriInfo();
    final ReentrantLock lock = session.getLock();
    lock.lock();
    try
    {
      final JsonObject object = emitObject( g -> Encoder.emitSession( schema, session, g, uri, false ) );

      assertEquals( object.getString( "id" ), "s1" );
      assertEquals( object.getString( "url" ), "http://example/session/s1" );
      assertFalse( object.containsKey( "channels" ) );
    }
    finally
    {
      lock.unlock();
    }
  }

  @Test
  public void emitSession_withNetworkData()
  {
    final SchemaMetaData schema = newSchemaMetaData();
    final ReplicantSession session = newSession( "s1" );
    final UriInfo uri = newUriInfo();
    final ReentrantLock lock = session.getLock();
    lock.lock();
    try
    {
      session.createSubscriptionEntry( new ChannelAddress( 2, 7, "fi" ) );
      session.createSubscriptionEntry( new ChannelAddress( 0 ) );
      session.createSubscriptionEntry( new ChannelAddress( 1, 10 ) );

      final JsonObject object = emitObject( g -> Encoder.emitSession( schema, session, g, uri, true ) );

      final JsonObject channels = object.getJsonObject( "channels" );
      assertEquals( channels.getString( "url" ), "http://example/session/s1/channel" );
      final JsonArray subscriptions = channels.getJsonArray( "subscriptions" );
      assertEquals( subscriptions.size(), 3 );

      assertEquals( subscriptions.getJsonObject( 0 ).getInt( "channelId" ), 0 );
      assertEquals( subscriptions.getJsonObject( 1 ).getInt( "channelId" ), 1 );
      assertEquals( subscriptions.getJsonObject( 2 ).getInt( "channelId" ), 2 );
      assertEquals( subscriptions.getJsonObject( 2 ).getString( "filterInstanceId" ), "fi" );
    }
    finally
    {
      lock.unlock();
    }
  }

  @Test
  public void emitChannelsList_sortsSubscriptions()
  {
    final SchemaMetaData schema = newSchemaMetaData();
    final ReplicantSession session = newSession( "s1" );
    final UriInfo uri = newUriInfo();
    final ReentrantLock lock = session.getLock();
    lock.lock();
    try
    {
      session.createSubscriptionEntry( new ChannelAddress( 2, 7 ) );
      session.createSubscriptionEntry( new ChannelAddress( 0 ) );
      session.createSubscriptionEntry( new ChannelAddress( 1, 10 ) );

      final JsonObject object = emitObject( g -> Encoder.emitChannelsList( schema, session, g, uri ) );
      assertEquals( object.getString( "url" ), "http://example/session/s1/channel" );

      final JsonArray subscriptions = object.getJsonArray( "subscriptions" );
      assertEquals( subscriptions.size(), 3 );
      assertEquals( subscriptions.getJsonObject( 0 ).getInt( "channelId" ), 0 );
      assertEquals( subscriptions.getJsonObject( 1 ).getInt( "channelId" ), 1 );
      assertEquals( subscriptions.getJsonObject( 2 ).getInt( "channelId" ), 2 );
    }
    finally
    {
      lock.unlock();
    }
  }

  @Test
  public void emitInstanceChannelList_filtersSubscriptions()
  {
    final SchemaMetaData schema = newSchemaMetaData();
    final ReplicantSession session = newSession( "s1" );
    final UriInfo uri = newUriInfo();
    final ReentrantLock lock = session.getLock();
    lock.lock();
    try
    {
      session.createSubscriptionEntry( new ChannelAddress( 1, 10 ) );
      session.createSubscriptionEntry( new ChannelAddress( 1, 11, "fi" ) );
      session.createSubscriptionEntry( new ChannelAddress( 2, 7 ) );

      final JsonObject object = emitObject( g -> Encoder.emitInstanceChannelList( schema, 1, session, g, uri ) );
      assertEquals( object.getString( "url" ), "http://example/session/s1/channel/1" );

      final JsonArray subscriptions = object.getJsonArray( "subscriptions" );
      assertEquals( subscriptions.size(), 2 );
      assertEquals( subscriptions.getJsonObject( 0 ).getInt( "channelId" ), 1 );
      assertEquals( subscriptions.getJsonObject( 1 ).getInt( "channelId" ), 1 );
    }
    finally
    {
      lock.unlock();
    }
  }

  @Test
  public void emitChannel_emitsFilterAndLinks()
  {
    final SchemaMetaData schema = newSchemaMetaData();
    final ReplicantSession session = newSession( "s1" );
    final UriInfo uri = newUriInfo();
    final ReentrantLock lock = session.getLock();
    lock.lock();
    try
    {
      final ChannelAddress address = new ChannelAddress( 2, 7, "fi" );
      final SubscriptionEntry entry = session.createSubscriptionEntry( address );
      entry.setExplicitlySubscribed( true );
      entry.setFilter( Collections.singletonMap( "k", "v" ) );
      entry.registerInwardSubscriptions( new ChannelAddress( 1, 9 ) );
      entry.registerOutwardSubscriptions( new ChannelAddress( 0 ), new ChannelAddress( 2, 4, "o1" ) );

      final JsonObject object = emitObject( g -> Encoder.emitChannel( schema, session, g, entry, uri ) );

      assertEquals( object.getString( "url" ), "http://example/session/s1/channel/2.7" );
      assertEquals( object.getString( "name" ), "instanced" );
      assertEquals( object.getInt( "channelId" ), 2 );
      assertEquals( object.getInt( "rootId" ), 7 );
      assertEquals( object.getString( "filterInstanceId" ), "fi" );
      assertTrue( object.getBoolean( "explicitlySubscribed" ) );

      final JsonObject filter = object.getJsonObject( "filter" );
      assertEquals( filter.getString( "k" ), "v" );

      final JsonArray inward = object.getJsonArray( "inwardSubscriptions" );
      assertEquals( inward.size(), 1 );
      final JsonObject inwardDescriptor = inward.getJsonObject( 0 );
      assertEquals( inwardDescriptor.getString( "name" ), "dynamic" );
      assertEquals( inwardDescriptor.getInt( "channelId" ), 1 );
      assertEquals( inwardDescriptor.getInt( "rootId" ), 9 );
      assertFalse( inwardDescriptor.containsKey( "filterInstanceId" ) );

      final JsonArray outward = object.getJsonArray( "outwardSubscriptions" );
      assertEquals( outward.size(), 2 );
      assertNotNull( findDescriptor( outward, 0, null, null ) );
      assertNotNull( findDescriptor( outward, 2, 4, "o1" ) );
    }
    finally
    {
      lock.unlock();
    }
  }

  @Test
  public void emitChannel_omitsFilterWhenUnsupported()
  {
    final SchemaMetaData schema = newSchemaMetaData();
    final ReplicantSession session = newSession( "s1" );
    final UriInfo uri = newUriInfo();
    final ReentrantLock lock = session.getLock();
    lock.lock();
    try
    {
      final SubscriptionEntry entry = session.createSubscriptionEntry( new ChannelAddress( 0 ) );
      final JsonObject object = emitObject( g -> Encoder.emitChannel( schema, session, g, entry, uri ) );

      assertFalse( object.containsKey( "filter" ) );
    }
    finally
    {
      lock.unlock();
    }
  }

  @Test
  public void emitChannel_nullFilterForFilterableChannel()
  {
    final SchemaMetaData schema = newSchemaMetaData();
    final ReplicantSession session = newSession( "s1" );
    final UriInfo uri = newUriInfo();
    final ReentrantLock lock = session.getLock();
    lock.lock();
    try
    {
      final SubscriptionEntry entry = session.createSubscriptionEntry( new ChannelAddress( 1, 11 ) );
      final JsonObject object = emitObject( g -> Encoder.emitChannel( schema, session, g, entry, uri ) );

      assertEquals( object.get( "filter" ), JsonValue.NULL );
    }
    finally
    {
      lock.unlock();
    }
  }

  private JsonObject emitObject( @Nonnull final Consumer<JsonGenerator> emitter )
  {
    final StringWriter writer = new StringWriter();
    final JsonGenerator generator = Json.createGenerator( writer );
    emitter.accept( generator );
    generator.close();
    return Json.createReader( new StringReader( writer.toString() ) ).readObject();
  }

  @Nonnull
  private SchemaMetaData newSchemaMetaData()
  {
    final ChannelMetaData typeChannel =
      new ChannelMetaData( 0,
                           "type",
                           null,
                           ChannelMetaData.FilterType.NONE,
                           null,
                           ChannelMetaData.CacheType.NONE,
                           true );
    final ChannelMetaData dynamicChannel =
      new ChannelMetaData( 1,
                           "dynamic",
                           1,
                           ChannelMetaData.FilterType.DYNAMIC,
                           json -> json,
                           ChannelMetaData.CacheType.NONE,
                           true );
    final ChannelMetaData instancedChannel =
      new ChannelMetaData( 2,
                           "instanced",
                           2,
                           ChannelMetaData.FilterType.DYNAMIC_INSTANCED,
                           json -> json,
                           ChannelMetaData.CacheType.NONE,
                           true );
    return new SchemaMetaData( "Test", typeChannel, dynamicChannel, instancedChannel );
  }

  @Nonnull
  private ReplicantSession newSession( @Nonnull final String id )
  {
    final Session webSocketSession = mock( Session.class );
    when( webSocketSession.getId() ).thenReturn( id );
    return new ReplicantSession( webSocketSession );
  }

  @Nonnull
  private UriInfo newUriInfo()
  {
    final UriInfo uri = mock( UriInfo.class );
    when( uri.getBaseUri() ).thenReturn( URI.create( "http://example/" ) );
    return uri;
  }

  @Nonnull
  private JsonObject findDescriptor( @Nonnull final JsonArray array,
                                     final int channelId,
                                     @Nullable final Integer rootId,
                                     @Nullable final String filterInstanceId )
  {
    for ( int i = 0; i < array.size(); i++ )
    {
      final JsonObject object = array.getJsonObject( i );
      if ( object.getInt( "channelId" ) == channelId &&
           ( null == rootId ? !object.containsKey( "rootId" ) : object.getInt( "rootId" ) == rootId ) &&
           ( null == filterInstanceId ?
             !object.containsKey( "filterInstanceId" ) :
             filterInstanceId.equals( object.getString( "filterInstanceId" ) ) ) )
      {
        return object;
      }
    }
    return null;
  }
}
