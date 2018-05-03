package replicant;

import java.text.ParseException;
import org.realityforge.guiceyloops.shared.ValueUtil;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class ZoneTest
  extends AbstractReplicantTest
{
  @Test
  public void zone_safeRun_Function()
  {
    ReplicantTestUtil.enableZones();

    final Zone zone1 = Replicant.createZone();

    assertEquals( ReplicantZoneHolder.getDefaultZone().getContext(), Replicant.context() );
    assertEquals( ReplicantZoneHolder.getZoneStack().size(), 0 );
    assertEquals( zone1.isActive(), false );

    final String expected = ValueUtil.randomString();
    final String actual = zone1.safeRun( () -> {
      assertEquals( zone1.getContext(), Replicant.context() );
      assertEquals( ReplicantZoneHolder.getZoneStack().size(), 1 );
      assertEquals( zone1.isActive(), true );
      return expected;
    } );

    assertEquals( actual, expected );

    assertEquals( ReplicantZoneHolder.getDefaultZone().getContext(), Replicant.context() );
    assertEquals( ReplicantZoneHolder.getZoneStack().size(), 0 );
  }

  @Test
  public void zone_safeRun_Procedure()
  {
    ReplicantTestUtil.enableZones();

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
  public void zone_run_Function_throwsException()
    throws Throwable
  {
    ReplicantTestUtil.enableZones();

    final Zone zone1 = Replicant.createZone();

    assertEquals( ReplicantZoneHolder.getDefaultZone().getContext(), Replicant.context() );
    assertEquals( ReplicantZoneHolder.getZoneStack().size(), 0 );
    assertEquals( zone1.isActive(), false );

    assertThrows( ParseException.class, () -> zone1.run( () -> {
      assertEquals( zone1.getContext(), Replicant.context() );
      assertEquals( ReplicantZoneHolder.getZoneStack().size(), 1 );
      assertEquals( zone1.isActive(), true );
      throw new ParseException( "", 1 );
    } ) );

    assertEquals( ReplicantZoneHolder.getDefaultZone().getContext(), Replicant.context() );
    assertEquals( ReplicantZoneHolder.getZoneStack().size(), 0 );
  }

  @Test
  public void zone_run_Procedure_throwsException()
    throws Throwable
  {
    ReplicantTestUtil.enableZones();

    final Zone zone1 = Replicant.createZone();

    assertEquals( ReplicantZoneHolder.getDefaultZone().getContext(), Replicant.context() );
    assertEquals( ReplicantZoneHolder.getZoneStack().size(), 0 );
    assertEquals( zone1.isActive(), false );

    final Procedure procedure = () -> {
      assertEquals( zone1.getContext(), Replicant.context() );
      assertEquals( ReplicantZoneHolder.getZoneStack().size(), 1 );
      assertEquals( zone1.isActive(), true );
      throw new ParseException( "", 1 );
    };
    assertThrows( ParseException.class, () -> zone1.run( procedure ) );

    assertEquals( ReplicantZoneHolder.getDefaultZone().getContext(), Replicant.context() );
    assertEquals( ReplicantZoneHolder.getZoneStack().size(), 0 );
  }

  @Test
  public void zone_run_Procedure_completesNormally()
    throws Throwable
  {
    ReplicantTestUtil.enableZones();

    final Zone zone1 = Replicant.createZone();

    assertEquals( ReplicantZoneHolder.getDefaultZone().getContext(), Replicant.context() );
    assertEquals( ReplicantZoneHolder.getZoneStack().size(), 0 );
    assertEquals( zone1.isActive(), false );

    final Procedure procedure = () -> {
      assertEquals( zone1.getContext(), Replicant.context() );
      assertEquals( ReplicantZoneHolder.getZoneStack().size(), 1 );
      assertEquals( zone1.isActive(), true );
    };
    zone1.run( procedure );

    assertEquals( ReplicantZoneHolder.getDefaultZone().getContext(), Replicant.context() );
    assertEquals( ReplicantZoneHolder.getZoneStack().size(), 0 );
  }
}
