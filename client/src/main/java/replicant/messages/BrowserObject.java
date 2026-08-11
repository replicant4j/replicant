package replicant.messages;

import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsType;
import jsinterop.base.JsArrayLike;
import org.jspecify.annotations.NonNull;

@JsType(isNative = true, name = "Object", namespace = JsPackage.GLOBAL)
final class BrowserObject {
    @NonNull
    static native JsArrayLike<String> keys(@NonNull Object object);

    private BrowserObject() {}
}
