package org.realityforge.replicant.client.runtime;

import org.realityforge.guiceyloops.shared.ValueUtil;
import org.realityforge.replicant.client.ChannelDescriptor;
import org.testng.annotations.Test;
import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

@SuppressWarnings( "Duplicates" )
public class AreaOfInterestServiceTest
{
  enum TestGraph
  {
    A, B
  }

  @Test
  public void findOrCreateScope()
  {
    final AreaOfInterestServiceImpl service = new AreaOfInterestServiceImpl();

    assertEquals( service.getScopeNames().size(), 0 );

    final String name1 = ValueUtil.randomString();
    final String name2 = ValueUtil.randomString();

    final Scope scope1 = service.findOrCreateScope( name1 );

    assertEquals( service.getScopeNames().size(), 1 );
    assertEquals( scope1.getName(), name1 );

    final Scope scope1b = service.findOrCreateScope( name1 );

    assertEquals( service.getScopeNames().size(), 1 );
    assertEquals( scope1b.getName(), name1 );
    assertEquals( scope1b, scope1 );

    final Scope scope2 = service.findOrCreateScope( name2 );

    assertEquals( service.getScopeNames().size(), 2 );
    assertEquals( scope2.getName(), name2 );
  }

  @Test
  public void releaseScopesExcept()
  {
    final AreaOfInterestServiceImpl service = new AreaOfInterestServiceImpl();

    final String name1 = ValueUtil.randomString();
    final String name2 = ValueUtil.randomString();
    final String name3 = ValueUtil.randomString();
    final String name4 = ValueUtil.randomString();

    service.createScopeReference( name1 );
    service.createScopeReference( name2 );
    service.createScopeReference( name3 );
    service.createScopeReference( name4 );

    assertEquals( service.getScopeNames().size(), 4 );

    service.releaseScopesExcept( name1, name2, name3, name4 );

    assertEquals( service.getScopeNames().size(), 4 );

    service.releaseScopesExcept( name1, name2, name3 );

    assertEquals( service.getScopeNames().size(), 3 );

    service.releaseScopesExcept( name1, ValueUtil.randomString(), ValueUtil.randomString() );

    assertEquals( service.getScopeNames().size(), 1 );
  }

  @Test
  public void basicScopeManagement()
  {
    final AreaOfInterestServiceImpl service = new AreaOfInterestServiceImpl();

    final AreaOfInterestListener listener = mock( AreaOfInterestListener.class );
    assertTrue( service.addAreaOfInterestListener( listener ) );
    assertFalse( service.addAreaOfInterestListener( listener ) );

    final String name = ValueUtil.randomString();
    final String subScopeName = ValueUtil.randomString();

    assertNull( service.findScope( name ) );
    assertEquals( service.getScopeNames().size(), 0 );
    assertEquals( service.getScopes().size(), 0 );

    final ScopeReference reference = service.createScopeReference( name );

    assertNotNull( reference );
    final Scope scope = reference.getScope();
    assertEquals( scope.getName(), name );
    assertEquals( scope.isActive(), true );
    assertEquals( scope.getReferenceCount(), 1 );

    verify( listener ).scopeCreated( scope );

    assertEquals( service.findScope( name ), scope );

    assertEquals( service.getScopeNames().size(), 1 );
    assertEquals( service.getScopes().size(), 1 );
    assertEquals( service.getScopeNames().stream().anyMatch( n -> n.equals( name ) ), true );
    assertEquals( service.getScopes().contains( scope ), true );

    {
      final ScopeReference reference2 = scope.createReference();
      assertEquals( reference2.getScope(), scope );
      assertEquals( reference2.hasBeenReleased(), false );
      assertEquals( scope.getReferenceCount(), 2 );

      reference2.release();
      assertEquals( reference2.hasBeenReleased(), true );
      assertEquals( scope.getReferenceCount(), 1 );
    }

    {
      final ScopeReference reference2 = service.createScopeReference( name );
      assertEquals( reference2.getScope(), scope );
      assertEquals( reference2.hasBeenReleased(), false );
      assertEquals( scope.getReferenceCount(), 2 );

      reference2.release();
      assertEquals( reference2.hasBeenReleased(), true );
      assertEquals( scope.getReferenceCount(), 1 );
    }

    final ScopeReference subScopeReference = service.createScopeReference( subScopeName );

    final Scope subScope = subScopeReference.getScope();
    assertNotNull( subScope );

    verify( listener ).scopeCreated( subScope );

    //Link to parent scope
    subScope.requireScope( scope );

    assertEquals( subScope.getName(), subScopeName );
    assertEquals( subScope.isActive(), true );
    assertEquals( subScope.getReferenceCount(), 1 );
    assertEquals( scope.getReferenceCount(), 2 );

    assertEquals( service.findScope( subScopeName ), subScope );

    assertEquals( service.getScopeNames().size(), 2 );
    assertEquals( service.getScopeNames().stream().anyMatch( n -> n.equals( name ) ), true );
    assertEquals( service.getScopeNames().stream().anyMatch( n -> n.equals( subScopeName ) ), true );

    assertEquals( service.getScopes().size(), 2 );
    assertEquals( service.getScopes().contains( scope ), true );
    assertEquals( service.getScopes().contains( subScope ), true );

    assertEquals( scope.isActive(), true );
    assertEquals( subScope.isActive(), true );

    // Delete reference to original scope. This means there is no explicit subscription anymore

    assertFalse( reference.hasBeenReleased() );
    reference.release();
    assertTrue( reference.hasBeenReleased() );

    // All scopes should still exist
    assertEquals( service.getScopeNames().size(), 2 );
    assertEquals( service.getScopeNames().stream().anyMatch( n -> n.equals( name ) ), true );
    assertEquals( service.getScopeNames().stream().anyMatch( n -> n.equals( subScopeName ) ), true );

    assertEquals( service.getScopes().size(), 2 );
    assertEquals( service.getScopes().contains( scope ), true );
    assertEquals( service.getScopes().contains( subScope ), true );

    assertEquals( scope.getReferenceCount(), 1 );
    assertEquals( subScope.getReferenceCount(), 1 );

    assertEquals( scope.isActive(), true );
    assertEquals( subScope.isActive(), true );

    // Delete reference to subscope

    assertFalse( subScopeReference.hasBeenReleased() );
    subScopeReference.release();
    assertTrue( subScopeReference.hasBeenReleased() );

    assertEquals( service.getScopeNames().size(), 0 );
    assertEquals( service.getScopeNames().stream().anyMatch( n -> n.equals( name ) ), false );
    assertEquals( service.getScopeNames().stream().anyMatch( n -> n.equals( subScopeName ) ), false );

    assertEquals( service.getScopes().size(), 0 );

    assertEquals( scope.getReferenceCount(), 0 );
    assertEquals( subScope.getReferenceCount(), 0 );

    assertEquals( scope.isActive(), false );
    assertEquals( subScope.isActive(), false );

    verify( listener ).scopeDeleted( scope );
    verify( listener ).scopeDeleted( subScope );

    //Now that the scope is inactive should not be able to create reference
    assertThrows( ScopeInactiveException.class, scope::createReference );

    assertTrue( service.removeAreaOfInterestListener( listener ) );
    assertFalse( service.removeAreaOfInterestListener( listener ) );
  }

  @Test
  public void manualScopeRelease()
  {
    final AreaOfInterestServiceImpl service = new AreaOfInterestServiceImpl();
    final String name = ValueUtil.randomString();

    assertNull( service.findScope( name ) );
    assertEquals( service.getScopeNames().size(), 0 );

    final ScopeReference reference = service.createScopeReference( name );

    final Scope scope = reference.getScope();
    assertNotNull( scope );

    assertEquals( scope.isActive(), true );
    assertEquals( service.getScopeNames().size(), 1 );
    assertEquals( reference.hasBeenReleased(), false );

    scope.release();

    assertEquals( service.getScopeNames().size(), 0 );
    assertEquals( scope.isActive(), false );
    assertEquals( reference.hasBeenReleased(), true );
  }

  @Test
  public void duplicateScopeLinkDisallowed()
  {
    final AreaOfInterestServiceImpl service = new AreaOfInterestServiceImpl();

    final Scope scope1 = service.createScopeReference( ValueUtil.randomString() ).getScope();
    final Scope scope2 = service.createScopeReference( ValueUtil.randomString() ).getScope();

    scope2.requireScope( scope1 );

    assertEquals( scope1.getReferenceCount(), 2 );
    assertEquals( scope2.getReferenceCount(), 1 );

    assertThrows( ScopeAlreadyRequiredException.class, () -> scope2.requireScope( scope1 ) );
  }

  @Test
  public void basicSubscriptionManagement()
  {
    final AreaOfInterestServiceImpl service = new AreaOfInterestServiceImpl();

    final AreaOfInterestListener listener = mock( AreaOfInterestListener.class );
    service.addAreaOfInterestListener( listener );

    final Scope scope1 = service.createScopeReference( ValueUtil.randomString() ).getScope();
    final Scope scope2 = service.createScopeReference( ValueUtil.randomString() ).getScope();

    final ChannelDescriptor descriptor1 = new ChannelDescriptor( TestGraph.A, null );
    final ChannelDescriptor descriptor2 = new ChannelDescriptor( TestGraph.B, 1 );
    final ChannelDescriptor descriptor3 = new ChannelDescriptor( TestGraph.B, 2 );

    assertEquals( scope1.getSubscriptionReferenceCount(), 0 );

    final SubscriptionReference reference1 = service.createSubscriptionReference( scope1, descriptor1 );

    final Subscription subscription1 = reference1.getSubscription();
    assertNotNull( subscription1 );

    assertEquals( subscription1.getDescriptor(), descriptor1 );
    assertEquals( subscription1.getReferenceCount(), 1 );
    assertEquals( subscription1.isActive(), true );

    assertEquals( scope1.getRequiredSubscriptions().contains( subscription1 ), true );
    assertEquals( scope1.getSubscriptionReferenceCount(), 1 );

    verify( listener ).subscriptionCreated( subscription1 );

    assertEquals( service.getSubscriptionsChannels().size(), 1 );
    assertEquals( service.getSubscriptionsChannels().stream().anyMatch( n -> n.equals( descriptor1 ) ), true );
    assertEquals( service.getSubscriptionsChannels().stream().anyMatch( n -> n.equals( descriptor2 ) ), false );
    assertEquals( service.getSubscriptionsChannels().stream().anyMatch( n -> n.equals( descriptor3 ) ), false );

    final Object newFilter = new Object();
    service.updateSubscription( subscription1, newFilter );

    assertEquals( subscription1.getFilter(), newFilter );

    verify( listener ).subscriptionUpdated( subscription1 );

    // Verify a second reference to channel in different scope is fine
    {
      final SubscriptionReference reference1c = service.createSubscriptionReference( scope2, descriptor1 );

      assertEquals( reference1c.getSubscription(), subscription1 );
      assertEquals( subscription1.getReferenceCount(), 2 );
      assertEquals( service.getSubscriptionsChannels().size(), 1 );

      assertFalse( reference1c.hasBeenReleased() );
      reference1c.release();
      assertTrue( reference1c.hasBeenReleased() );

      assertEquals( subscription1.getReferenceCount(), 1 );
    }

    assertFalse( reference1.hasBeenReleased() );
    reference1.release();
    assertTrue( reference1.hasBeenReleased() );

    assertEquals( subscription1.isActive(), false );

    assertEquals( service.getSubscriptionsChannels().size(), 0 );

    verify( listener ).subscriptionDeleted( subscription1 );
  }

  @Test
  public void createSubscription()
  {
    final AreaOfInterestServiceImpl service = new AreaOfInterestServiceImpl();

    final AreaOfInterestListener listener = mock( AreaOfInterestListener.class );
    service.addAreaOfInterestListener( listener );

    final ChannelDescriptor descriptor1 = new ChannelDescriptor( TestGraph.A );
    final ChannelDescriptor descriptor2 = new ChannelDescriptor( TestGraph.B );

    final String filer1 = "Filer1";
    final String filer2 = null;

    final Subscription subscription1 = service.createSubscription( descriptor1, filer1 );

    assertEquals( subscription1.getDescriptor(), descriptor1 );
    assertEquals( subscription1.getFilter(), filer1 );
    assertEquals( subscription1.getReferenceCount(), 0 );

    verify( listener ).subscriptionCreated( subscription1 );

    final Subscription subscription2 = service.createSubscription( descriptor2, filer2 );

    assertEquals( subscription2.getDescriptor(), descriptor2 );
    assertEquals( subscription2.getFilter(), filer2 );
    assertEquals( subscription2.getReferenceCount(), 0 );

    verify( listener ).subscriptionCreated( subscription2 );
  }
}
