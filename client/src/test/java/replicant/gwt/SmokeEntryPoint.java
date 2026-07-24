package replicant.gwt;

import com.google.gwt.core.client.EntryPoint;
import replicant.Replicant;

public final class SmokeEntryPoint implements EntryPoint {
    @Override
    public void onModuleLoad() {
        Replicant.isProductionMode();
    }
}
