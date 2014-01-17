package org.realityforge.replicant.server.ee;

import java.lang.reflect.Field;
import java.util.Collection;
import org.realityforge.replicant.server.EntityMessage;
import org.realityforge.replicant.server.EntityMessageGenerator;
import org.realityforge.replicant.server.EntityMessageSet;
import org.realityforge.replicant.server.MessageTestUtil;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class ChangeRecorderTest
{
  public static final String ENTITY = "ENTITY";
  public static final String EXCLUDED_ENTITY = "EXCLUDED_ENTITY";

  @Test
  public void postRemoveGeneratesMessage()
      throws Exception
  {
    final TestTransactionSynchronizationRegistry registry = new TestTransactionSynchronizationRegistry();
    final ChangeRecorder changeRecorder = createChangeRecorder( registry );
    changeRecorder.preRemove(ENTITY);
    assertMessageGenerated( registry, false );
  }

  @Test
  public void postUpdateGeneratesMessage()
      throws Exception
  {
    final TestTransactionSynchronizationRegistry registry = new TestTransactionSynchronizationRegistry();
    final ChangeRecorder changeRecorder = createChangeRecorder( registry );
    changeRecorder.postUpdate( ENTITY );
    assertMessageGenerated( registry, true );
  }

  @Test
  public void excludedEntityDoesNotGenerateMessage()
      throws Exception
  {
    final TestTransactionSynchronizationRegistry registry = new TestTransactionSynchronizationRegistry();
    final ChangeRecorder changeRecorder = createChangeRecorder( registry );
    changeRecorder.postUpdate( EXCLUDED_ENTITY );
    assertMessageNotGenerated( registry );
  }

  private void assertMessageGenerated( final TestTransactionSynchronizationRegistry registry, final boolean update )
  {
    final EntityMessageSet messageSet = EntityMessageCacheUtil.lookupEntityMessageSet( registry );
    assertNotNull( messageSet );
    final Collection<EntityMessage> messages = messageSet.getEntityMessages();
    assertEquals( messages.size(), 1 );
    final EntityMessage message = messages.iterator().next();
    assertEquals( message.getID(), ENTITY );
    assertEquals( message.isUpdate(), update );
  }

  private void assertMessageNotGenerated( final TestTransactionSynchronizationRegistry registry )
  {
    final EntityMessageSet messageSet = EntityMessageCacheUtil.lookupEntityMessageSet( registry );
    assertNull( messageSet );
  }

  private ChangeRecorder createChangeRecorder( final TestTransactionSynchronizationRegistry registry )
      throws Exception
  {
    final ChangeRecorder changeRecorder = new TestChangeRecorder();
    final Field field = ChangeRecorder.class.getDeclaredField( "_registry" );
    field.setAccessible( true );
    field.set( changeRecorder, registry );
    return changeRecorder;
  }

  static class TestChangeRecorder
      extends ChangeRecorder
      implements EntityMessageGenerator
  {
    @Override
    protected EntityMessageGenerator getEntityMessageGenerator()
    {
      return this;
    }

    @Override
    public EntityMessage convertToEntityMessage( final Object object, final boolean isUpdate )
    {
      if( object.equals( ENTITY ) )
      {
        return MessageTestUtil.createMessage( ENTITY,
                                              1,
                                              0,
                                              "r1",
                                              "r2",
                                              ( isUpdate ? "a1" : null ), ( isUpdate ? "a2" : null ) );
      }
      else
      {
        return null;
      }
    }
  }
}
