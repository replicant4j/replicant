package replicant;

import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class ReplicantTest
  extends AbstractReplicantTest
{
  @Test
  public void setting_areSpiesEnabled()
  {
    assertTrue( Replicant.areSpiesEnabled() );
    ReplicantTestUtil.disableSpies();
    assertFalse( Replicant.areSpiesEnabled() );
  }

  @Test
  public void setting_areZonesEnabled()
  {
    assertFalse( Replicant.areZonesEnabled() );
    ReplicantTestUtil.enableZones();
    assertTrue( Replicant.areZonesEnabled() );
  }

  @Test
  public void setting_shouldValidateChangeSetOnRead()
  {
    assertTrue( Replicant.shouldValidateChangeSetOnRead() );
    ReplicantTestUtil.noValidateChangeSetOnRead();
    assertFalse( Replicant.shouldValidateChangeSetOnRead() );
  }

  @Test
  public void setting_shouldValidateEntitiesOnLoad()
  {
    assertTrue( Replicant.shouldValidateEntitiesOnLoad() );
    ReplicantTestUtil.noValidateEntitiesOnLoad();
    assertFalse( Replicant.shouldValidateEntitiesOnLoad() );
  }

  @Test
  public void setting_shouldCheckInvariants()
  {
    assertTrue( Replicant.shouldCheckInvariants() );
    ReplicantTestUtil.noCheckInvariants();
    assertFalse( Replicant.shouldCheckInvariants() );
  }

  @Test
  public void setting_shouldCheckApiInvariants()
  {
    assertTrue( Replicant.shouldCheckApiInvariants() );
    ReplicantTestUtil.noCheckApiInvariants();
    assertFalse( Replicant.shouldCheckApiInvariants() );
  }

  @Test
  public void context_when_zones_disabled()
  {
    ReplicantTestUtil.disableZones();

    final ReplicantContext context1 = Replicant.context();
    assertNotNull( context1 );
    final ReplicantContext context2 = Replicant.context();
    assertTrue( context1 == context2 );
  }

  @Test
  public void zone_basicOperation()
  {
    ReplicantTestUtil.enableZones();

    assertEquals( ReplicantZoneHolder.getDefaultZone().getContext(), Replicant.context() );
    assertEquals( ReplicantZoneHolder.getZoneStack().size(), 0 );

    final Zone zone1 = Replicant.createZone();

    assertEquals( ReplicantZoneHolder.getDefaultZone().getContext(), Replicant.context() );
    assertEquals( ReplicantZoneHolder.getZoneStack().size(), 0 );
    assertEquals( zone1.isActive(), false );

    zone1.safeRun( () -> {
      assertEquals( zone1.getContext(), Replicant.context() );
      assertEquals( ReplicantZoneHolder.getZoneStack().size(), 1 );
      assertEquals( zone1.isActive(), true );
    } );

    assertEquals( ReplicantZoneHolder.getDefaultZone().getContext(), Replicant.context() );
    assertEquals( ReplicantZoneHolder.getZoneStack().size(), 0 );
  }

  @Test
  public void zone_multipleZones()
  {
    ReplicantTestUtil.enableZones();

    assertEquals( ReplicantZoneHolder.getDefaultZone().getContext(), Replicant.context() );
    assertEquals( ReplicantZoneHolder.getZoneStack().size(), 0 );

    final Zone zone1 = Replicant.createZone();
    final Zone zone2 = Replicant.createZone();
    final Zone zone3 = Replicant.createZone();

    assertEquals( ReplicantZoneHolder.getDefaultZone().getContext(), Replicant.context() );
    assertEquals( ReplicantZoneHolder.getZoneStack().size(), 0 );
    assertEquals( zone1.isActive(), false );
    assertEquals( zone2.isActive(), false );
    assertEquals( zone3.isActive(), false );

    zone1.safeRun( () -> {

      assertEquals( zone1.getContext(), Replicant.context() );
      assertEquals( ReplicantZoneHolder.getZoneStack().size(), 1 );
      assertEquals( zone1.isActive(), true );
      assertEquals( zone2.isActive(), false );
      assertEquals( zone3.isActive(), false );

      zone2.safeRun( () -> {

        assertEquals( zone2.getContext(), Replicant.context() );
        assertEquals( ReplicantZoneHolder.getZoneStack().size(), 2 );
        assertEquals( ReplicantZoneHolder.getZoneStack().get( 0 ), ReplicantZoneHolder.getDefaultZone() );
        assertEquals( ReplicantZoneHolder.getZoneStack().get( 1 ), zone1 );
        assertEquals( zone1.isActive(), false );
        assertEquals( zone2.isActive(), true );
        assertEquals( zone3.isActive(), false );

        zone1.safeRun( () -> {

          assertEquals( zone1.getContext(), Replicant.context() );
          assertEquals( ReplicantZoneHolder.getZoneStack().size(), 3 );
          assertEquals( ReplicantZoneHolder.getZoneStack().get( 0 ), ReplicantZoneHolder.getDefaultZone() );
          assertEquals( ReplicantZoneHolder.getZoneStack().get( 1 ), zone1 );
          assertEquals( ReplicantZoneHolder.getZoneStack().get( 2 ), zone2 );
          assertEquals( zone1.isActive(), true );
          assertEquals( zone2.isActive(), false );
          assertEquals( zone3.isActive(), false );

          zone3.safeRun( () -> {

            assertEquals( zone3.getContext(), Replicant.context() );
            assertEquals( ReplicantZoneHolder.getZoneStack().size(), 4 );
            assertEquals( ReplicantZoneHolder.getZoneStack().get( 0 ), ReplicantZoneHolder.getDefaultZone() );
            assertEquals( ReplicantZoneHolder.getZoneStack().get( 1 ), zone1 );
            assertEquals( ReplicantZoneHolder.getZoneStack().get( 2 ), zone2 );
            assertEquals( ReplicantZoneHolder.getZoneStack().get( 3 ), zone1 );
            assertEquals( zone1.isActive(), false );
            assertEquals( zone2.isActive(), false );
            assertEquals( zone3.isActive(), true );

          } );

          assertEquals( zone1.getContext(), Replicant.context() );
          assertEquals( ReplicantZoneHolder.getZoneStack().size(), 3 );
          assertEquals( ReplicantZoneHolder.getZoneStack().get( 0 ), ReplicantZoneHolder.getDefaultZone() );
          assertEquals( ReplicantZoneHolder.getZoneStack().get( 1 ), zone1 );
          assertEquals( ReplicantZoneHolder.getZoneStack().get( 2 ), zone2 );
          assertEquals( zone1.isActive(), true );
          assertEquals( zone2.isActive(), false );
          assertEquals( zone3.isActive(), false );

        } );

        assertEquals( zone2.getContext(), Replicant.context() );
        assertEquals( ReplicantZoneHolder.getZoneStack().size(), 2 );
        assertEquals( ReplicantZoneHolder.getZoneStack().get( 0 ), ReplicantZoneHolder.getDefaultZone() );
        assertEquals( ReplicantZoneHolder.getZoneStack().get( 1 ), zone1 );
        assertEquals( zone1.isActive(), false );
        assertEquals( zone2.isActive(), true );
        assertEquals( zone3.isActive(), false );

      } );

      assertEquals( zone1.getContext(), Replicant.context() );
      assertEquals( ReplicantZoneHolder.getZoneStack().size(), 1 );
      assertEquals( ReplicantZoneHolder.getZoneStack().get( 0 ), ReplicantZoneHolder.getDefaultZone() );
      assertEquals( zone1.isActive(), true );
      assertEquals( zone2.isActive(), false );
      assertEquals( zone3.isActive(), false );

    } );

    assertEquals( ReplicantZoneHolder.getDefaultZone().getContext(), Replicant.context() );
    assertEquals( ReplicantZoneHolder.getZoneStack().size(), 0 );
    assertEquals( zone1.isActive(), false );
    assertEquals( zone2.isActive(), false );
    assertEquals( zone3.isActive(), false );
  }

  @Test
  public void createZone_when_zonesDisabled()
  {
    ReplicantTestUtil.disableZones();

    final IllegalStateException exception = expectThrows( IllegalStateException.class, Replicant::createZone );
    assertEquals( exception.getMessage(), "Replicant-0001: Invoked Replicant.createZone() but zones are not enabled." );
  }

  @Test
  public void activateZone_whenZonesNotEnabled()
  {
    ReplicantTestUtil.disableZones();

    final IllegalStateException exception =
      expectThrows( IllegalStateException.class, () -> Replicant.activateZone( new Zone() ) );
    assertEquals( exception.getMessage(),
                  "Replicant-0002: Invoked Replicant.activateZone() but zones are not enabled." );
  }

  @Test
  public void deactivateZone_whenZonesNotEnabled()
  {
    ReplicantTestUtil.disableZones();

    final IllegalStateException exception =
      expectThrows( IllegalStateException.class, () -> Replicant.deactivateZone( new Zone() ) );
    assertEquals( exception.getMessage(),
                  "Replicant-0003: Invoked Replicant.deactivateZone() but zones are not enabled." );
  }

  @Test
  public void currentZone_whenZonesNotEnabled()
  {
    ReplicantTestUtil.disableZones();
    final IllegalStateException exception =
      expectThrows( IllegalStateException.class, Replicant::currentZone );
    assertEquals( exception.getMessage(),
                  "Replicant-0005: Invoked Replicant.currentZone() but zones are not enabled." );
  }

  @Test
  public void deactivateZone_whenNotActive()
  {
    ReplicantTestUtil.enableZones();
    final IllegalStateException exception =
      expectThrows( IllegalStateException.class, () -> Replicant.deactivateZone( new Zone() ) );
    assertEquals( exception.getMessage(), "Replicant-0004: Attempted to deactivate zone that is not active." );
  }
}
