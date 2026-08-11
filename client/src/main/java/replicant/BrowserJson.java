package replicant;

import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsType;
import jsinterop.base.Any;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

@JsType(isNative = true, name = "JSON", namespace = JsPackage.GLOBAL)
final class BrowserJson {
    @Nullable
    static native Any parse(@NonNull String text);

    @NonNull
    static native String stringify(@NonNull Any value);

    private BrowserJson() {}
}
