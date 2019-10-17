package replicant;

import arez.Disposable;
import java.util.ArrayList;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import static org.realityforge.braincheck.Guards.*;

/**
 * A utility class that contains reference to zone data when zones are enabled.
 * This is extracted to a separate class to eliminate the <clinit> from Replicant and thus
 * make it much easier for GWT to optimize out code based on build time compilation parameters.
 */
final class ReplicantZoneHolder
{
  /**
   * Default zone if zones are enabled.
   */
  @Nullable
  private static Zone c_defaultZone = Replicant.areZonesEnabled() ? new Zone() : null;
  /**
   * Default zone if zones are enabled.
   */
  @Nullable
  private static Zone c_zone = Replicant.areZonesEnabled() ? c_defaultZone : null;
  /**
   * The zones that were previously active.
   * If there is no zones in the stack and a zone is deactivated then the default zone is made current.
   */
  @Nullable
  private static ArrayList<Zone> c_zoneStack = Replicant.areZonesEnabled() ? new ArrayList<>() : null;

  private ReplicantZoneHolder()
  {
  }

  /**
   * Return the ReplicantContext from the provider.
   *
   * @return the ReplicantContext.
   */
  @Nonnull
  static ReplicantContext context()
  {
    assert null != c_zone;
    return c_zone.getContext();
  }

  /**
   * Save the old zone and make the specified zone the current zone.
   */
  @SuppressWarnings( "ConstantConditions" )
  static void activateZone( @Nonnull final Zone zone )
  {
    if ( Replicant.shouldCheckInvariants() )
    {
      invariant( Replicant::areZonesEnabled,
                 () -> "Replicant-0002: Invoked Replicant.activateZone() but zones are not enabled." );
    }
    assert null != c_zoneStack;
    assert null != zone;
    c_zoneStack.add( c_zone );
    c_zone = zone;
  }

  /**
   * Restore the old zone.
   * This takes the zone that was current when {@link #activateZone(Zone)} was called for the active zone
   * and restores it to being the current zone.
   */
  static void deactivateZone( @Nonnull final Zone zone )
  {
    if ( Replicant.shouldCheckInvariants() )
    {
      invariant( Replicant::areZonesEnabled,
                 () -> "Replicant-0003: Invoked Replicant.deactivateZone() but zones are not enabled." );
    }
    if ( Replicant.shouldCheckApiInvariants() )
    {
      apiInvariant( () -> c_zone == zone, () -> "Replicant-0004: Attempted to deactivate zone that is not active." );
    }
    assert null != c_zoneStack;
    c_zone = c_zoneStack.isEmpty() ? c_defaultZone : c_zoneStack.remove( c_zoneStack.size() - 1 );
  }

  /**
   * Return the current zone.
   *
   * @return the current zone.
   */
  @Nonnull
  static Zone currentZone()
  {
    if ( Replicant.shouldCheckInvariants() )
    {
      invariant( Replicant::areZonesEnabled,
                 () -> "Replicant-0005: Invoked Replicant.currentZone() but zones are not enabled." );
    }
    assert null != c_zone;
    return c_zone;
  }

  /**
   * Clear the state to cleanup .
   * This is dangerous as it may leave dangling references and should only be done in tests.
   */
  static void reset()
  {
    if ( null != c_defaultZone )
    {
      Disposable.dispose( c_defaultZone );
    }
    if ( null != c_zoneStack )
    {
      c_zoneStack.forEach( Disposable::dispose );
    }
    c_defaultZone = new Zone();
    c_zone = c_defaultZone;
    c_zoneStack = new ArrayList<>();
  }

  @Nonnull
  static Zone getDefaultZone()
  {
    assert null != c_defaultZone;
    return c_defaultZone;
  }

  @Nonnull
  static ArrayList<Zone> getZoneStack()
  {
    assert null != c_zoneStack;
    return c_zoneStack;
  }
}
