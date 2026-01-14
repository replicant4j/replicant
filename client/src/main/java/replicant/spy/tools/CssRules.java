package replicant.spy.tools;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.intellij.lang.annotations.Language;
import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.LOCAL_VARIABLE;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;

/**
 * An annotation that ensures the the IDEA IDE will treat annotated elements as containing css rules.
 */
@Retention( RetentionPolicy.CLASS )
@Target( { METHOD, FIELD, PARAMETER, LOCAL_VARIABLE, ANNOTATION_TYPE } )
@Language( value = "CSS", prefix = ".a {", suffix = "}" )
@interface CssRules
{
}
