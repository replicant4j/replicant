package org.realityforge.replicant.server.ee;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import javax.interceptor.InterceptorBinding;

/**
 * Interceptor binding for pushing replication interceptor.
 * Usually added to methods/services that occur in CDI event handling after transaction completes.
 */
@Inherited
@Target( { ElementType.TYPE, ElementType.METHOD } )
@Retention( RetentionPolicy.RUNTIME )
@InterceptorBinding
public @interface PushReplicationState
{
}
