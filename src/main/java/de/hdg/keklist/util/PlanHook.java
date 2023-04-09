package de.hdg.keklist.util;

import com.djrapitops.plan.capability.CapabilityService;
import com.djrapitops.plan.extension.Caller;
import com.djrapitops.plan.extension.ExtensionService;
import de.hdg.keklist.Keklist;
import lombok.Getter;

import java.util.Optional;

public class PlanHook {


    private @Getter Optional<Caller> caller;

    public PlanHook() {

    }

    public void hookIntoPlan() {
        if (!areAllCapabilitiesAvailable()) return;
        registerDataExtension();
        listenForPlanReloads();
    }

    private boolean areAllCapabilitiesAvailable() {
        CapabilityService capabilities = CapabilityService.getInstance();
        return capabilities.hasCapability("DATA_EXTENSION_VALUES");
    }

    private void registerDataExtension() {
        try {
            caller = ExtensionService.getInstance().register(new KekDataExtension());
        } catch (IllegalStateException notEnabled) {
            Keklist.getInstance().getLogger().warning("Plan is not enabled, cannot register DataExtension.");
        } catch (IllegalArgumentException exception) {
            Keklist.getInstance().getLogger().severe("DataExtension implementation is invalid, cannot register DataExtension.");
            exception.printStackTrace();
        }
    }

    private void listenForPlanReloads() {
        CapabilityService.getInstance().registerEnableListener(isPlanEnabled -> {
            if (isPlanEnabled)
                registerDataExtension();
        });
    }
}
