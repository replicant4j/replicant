package org.realityforge.replicant.client;

import org.testng.annotations.Test;
import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

public class EntityRepositoryValidatorTest
{
  @Test
  public void validate_LinkableThatReturnsTrueFor_isValid()
    throws Exception
  {
    final EntityRepository r = new EntityRepositoryImpl();
    assertValidated( r );

    final Linkable linkable = mock( Linkable.class );
    when( linkable.isValid() ).thenReturn( true );

    r.registerEntity( Linkable.class, "2", linkable );

    assertValidated( r );
  }

  @Test
  public void validate_LinkableThatReturnsFalseFor_isValid()
    throws Exception
  {
    final EntityRepository r = new EntityRepositoryImpl();
    assertValidated( r );

    final Linkable linkable = mock( Linkable.class );
    when( linkable.isValid() ).thenReturn( false );

    r.registerEntity( Linkable.class, "2", linkable );

    assertFailedToValidate( r );
  }

  @Test
  public void validate_VerifiableRaisesException()
    throws Exception
  {
    final EntityRepository r = new EntityRepositoryImpl();
    assertValidated( r );

    final Verifiable verifiable = mock( Verifiable.class );
    doThrow( new Exception() ).when( verifiable ).verify();

    r.registerEntity( Verifiable.class, "3", verifiable );
    assertFailedToValidate( r );
  }

  @Test
  public void validate_nonLinkable()
    throws Exception
  {
    final EntityRepository r = new EntityRepositoryImpl();
    assertValidated( r );
    r.registerEntity( String.class, "foo", "foo" );
    assertValidated( r );
  }

  private void assertValidated( final EntityRepository r )
  {
    try
    {
      new EntityRepositoryValidator().validate( r );
    }
    catch ( final Exception e )
    {
      fail( "Expected to validate but failed due to : " + e );
    }
  }

  private void assertFailedToValidate( final EntityRepository r )
  {
    boolean invalid = false;
    try
    {
      new EntityRepositoryValidator().validate( r );
    }
    catch ( final Throwable e )
    {
      invalid = true;
    }
    if ( !invalid )
    {
      fail( "Expected invalid object to raise exception in validate method" );
    }
  }
}
