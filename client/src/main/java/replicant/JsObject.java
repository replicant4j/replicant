package replicant;

import akasha.lang.JsArray;
import javax.annotation.Nonnull;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsType;

@JsType( isNative = true, name = "Object", namespace = JsPackage.GLOBAL )
public final class JsObject
{
  @Nonnull
  public static native JsArray<String> keys( @Nonnull Object obj );
}
