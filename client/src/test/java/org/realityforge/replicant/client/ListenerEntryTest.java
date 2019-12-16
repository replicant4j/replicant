package org.realityforge.replicant.client;

import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class ListenerEntryTest
{
  @Test
  public void basicConstruction()
  {
    final RecordingListener listener = new RecordingListener();
    final ListenerEntry entry = new ListenerEntry( listener );
    assertEquals( entry.getListener(), listener );
  }

  @Test
  public void isEmpty()
  {
    final Object instance = new Object();
    final RecordingListener listener = new RecordingListener();
    final ListenerEntry entry = new ListenerEntry( listener );

    assertTrue( entry.isEmpty() );
    entry.setGlobalListener( true );
    assertFalse( entry.isEmpty() );
    entry.setGlobalListener( false );
    assertTrue( entry.isEmpty() );
    entry.interestedTypeSet().add( String.class );
    assertFalse( entry.isEmpty() );
    entry.interestedTypeSet().remove( String.class );
    assertTrue( entry.isEmpty() );
    entry.interestedInstanceSet().add( instance );
    assertFalse( entry.isEmpty() );
    entry.interestedInstanceSet().remove( instance );
    assertTrue( entry.isEmpty() );
    assertEquals( entry.getListener(), listener );
  }
}
