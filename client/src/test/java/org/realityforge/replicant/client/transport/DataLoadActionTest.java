package org.realityforge.replicant.client.transport;

import replicant.AbstractReplicantTest;

public class DataLoadActionTest
  extends AbstractReplicantTest
{
  /*
  @DataProvider( name = "actionDescriptions" )
  public Object[][] actionDescriptions()
  {
    final List<Boolean> flags = Arrays.asList( Boolean.TRUE, Boolean.FALSE );

    final ArrayList<Object[]> objects = new ArrayList<>();
    for ( final boolean normalCompletion : flags )
    {
      for ( final boolean useRunnable : flags )
      {
        for ( final boolean isLinkableEntity : flags )
        {
          for ( final boolean oob : flags )
          {
            for ( final boolean update : flags )
            {
              final boolean expectLink = isLinkableEntity && update;
              final EntityChange changeUpdate =
                EntityChange.create( ValueUtil.randomInt(),
                               ValueUtil.randomInt(),
                               new EntityChannel[ 0 ],
                               update ? JsPropertyMap.of() : null );
              final TestChangeSet changeSet =
                new TestChangeSet( ChangeSet.create( ValueUtil.randomInt(),
                                                     null,
                                                     null,
                                                     new ChannelChange[ 0 ],
                                                     new EntityChange[]{ changeUpdate } ),
                                   useRunnable ? new MockRunner() : null );
              final Object entity = isLinkableEntity ? new MockLinkable() : new Object();
              objects.add( new Object[]{ normalCompletion, oob, changeSet, entity, expectLink } );
            }
            final boolean expectLink = false;
            final EntityChange changeUpdate =
              EntityChange.create( ValueUtil.randomInt(), ValueUtil.randomInt(), new EntityChannel[ 0 ], JsPropertyMap.of() );
            final EntityChange changeRemove =
              EntityChange.create( ValueUtil.randomInt(), ValueUtil.randomInt(), new EntityChannel[ 0 ], null );
            final ChangeSet cs =
              ChangeSet.create( ValueUtil.randomInt(),
                                ValueUtil.randomString(),
                                null,
                                new ChannelChange[ 0 ],
                                new EntityChange[]{ changeUpdate, changeRemove } );
            final TestChangeSet changeSet =
              new TestChangeSet( cs,
                                 useRunnable ? new MockRunner() : null );
            final Object entity = isLinkableEntity ? new MockLinkable() : new Object();
            objects.add( new Object[]{ normalCompletion, oob, changeSet, entity, expectLink } );
          }
        }
      }
    }

    return objects.toArray( new Object[ objects.size() ][] );
  }

  @Test( dataProvider = "actionDescriptions" )
  public void verifyActionLifecycle( final boolean normalCompletion,
                                     final boolean oob,
                                     final TestChangeSet changeSet,
                                     final Object entity,
                                     final boolean expectedLink )
  {
    final MockRunner runnable = (MockRunner) changeSet.getRunnable();
    final DataLoadAction action = new DataLoadAction( "BLAH", oob );

    //Ensure the initial state is as expected
    assertEquals( action.getRawJsonData(), "BLAH" );
    assertEquals( action.getChangeSet(), null );
    if ( null != runnable )
    {
      assertEquals( runnable.getRunCount(), 0 );
    }

    assertEquals( action.areEntityLinksCalculated(), false );
    assertEquals( action.areEntityLinksPending(), false );
    assertEquals( action.areChangesPending(), false );
    assertEquals( action.hasWorldBeenNotified(), false );

    if ( oob )
    {
      action.setRunnable( changeSet.getRunnable() );
      action.setChangeSet( changeSet.getChangeSet(), null );
    }
    else
    {
      final String requestID = changeSet.getChangeSet().getRequestID();
      assertNotNull( requestID );
      final RequestEntry request = new RequestEntry( requestID, "MyOperation", null );
      if ( normalCompletion )
      {
        request.setNormalCompletionAction( runnable );
      }
      else
      {
        request.setNonNormalCompletionAction( runnable );
      }
      action.setChangeSet( changeSet.getChangeSet(), request );
    }
    assertEquals( action.getRunnable(), runnable );

    assertEquals( action.getChangeSet(), changeSet.getChangeSet() );
    assertEquals( action.getRawJsonData(), null );

    assertEquals( action.areChangesPending(), true );
    final EntityChange change = action.nextChange();
    assertNotNull( change );
    assertEquals( change, changeSet.getChangeSet().getChange( 0 ) );

    action.changeProcessed( change.isUpdate(), entity );

    if ( 1 == changeSet.getChangeSet().getChangeCount() )
    {
      assertEquals( action.areChangesPending(), false );
    }
    else
    {
      while ( action.areChangesPending() )
      {
        final EntityChange nextChange = action.nextChange();
        assertNotNull( nextChange );
        action.changeProcessed( nextChange.isUpdate(), entity );
      }
    }

    assertEquals( action.areEntityLinksCalculated(), false );

    action.calculateEntitiesToLink();

    assertEquals( action.areEntityLinksCalculated(), true );

    if ( expectedLink )
    {
      assertEquals( action.areEntityLinksPending(), true );
      assertEquals( action.nextEntityToLink(), entity );
      assertEquals( action.areEntityLinksPending(), false );
    }
    else
    {
      assertEquals( action.areEntityLinksPending(), false );
    }

    assertEquals( action.hasWorldBeenNotified(), false );

    action.markWorldAsNotified();

    assertEquals( action.hasWorldBeenNotified(), true );
  }

  static final class MockRunner
    implements Runnable
  {
    private int _runCount;

    int getRunCount()
    {
      return _runCount;
    }

    @Override
    public void run()
    {
      _runCount++;
    }

    @Override
    public String toString()
    {
      return "Listener:" + System.identityHashCode( this );
    }
  }

  static final class MockLinkable
    implements Linkable
  {
    @Override
    public void link()
    {
    }

    @Override
    public String toString()
    {
      return "Entity:" + System.identityHashCode( this );
    }
  }
  */
}
