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

  @Test
  public void tryClone()
  {
    final Object instance = new Object();
    final RecordingListener listener = new RecordingListener();
    final ListenerEntry entry = new ListenerEntry( listener );
    entry.setGlobalListener( true );
    entry.interestedTypeSet().add( String.class );
    entry.interestedInstanceSet().add( instance );

    final ListenerEntry clone = entry.clone();

    assertNotSame( clone, entry );
    assertEquals( clone.getListener(), listener );
    assertEquals( clone.isGlobalListener(), entry.isGlobalListener() );
    assertEquals( clone.interestedInstanceSet(), entry.interestedInstanceSet() );
  }

  @Test( expectedExceptions = UnsupportedOperationException.class )
  public void getInterestedTypeSet_raises_UnsupportedOperationException_on_modification()
  {
    final RecordingListener listener = new RecordingListener();
    final ListenerEntry entry = new ListenerEntry( listener );
    entry.interestedTypeSet().add( String.class );

    assertTrue( entry.getInterestedTypeSet().contains( String.class ) );

    // Should raise an exception
    entry.getInterestedTypeSet().remove( String.class );
  }


  @Test( expectedExceptions = UnsupportedOperationException.class )
  public void getInterestedInstanceSet_raises_UnsupportedOperationException_on_modification()
  {
    final RecordingListener listener = new RecordingListener();
    final ListenerEntry entry = new ListenerEntry( listener );
    final Object instance = new Object();
    entry.interestedInstanceSet().add( instance );

    assertTrue( entry.getInterestedInstanceSet().contains( instance ) );

    // Should raise an exception
    entry.getInterestedInstanceSet().remove( instance );
  }
}
