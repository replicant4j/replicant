package replicant.server.runtime;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import javax.inject.Qualifier;

/**
 * Qualifier for the replicant system.
 */
@Qualifier
@Retention( RetentionPolicy.RUNTIME )
@Target( { ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.TYPE } )
public @interface ReplicantSystem
{
  String value() default "";
}
