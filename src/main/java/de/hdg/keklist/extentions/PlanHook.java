package de.hdg.keklist.extentions;

import com.djrapitops.plan.capability.CapabilityService;
import com.djrapitops.plan.extension.Caller;
import com.djrapitops.plan.extension.ExtensionService;
import de.hdg.keklist.Keklist;
import de.hdg.keklist.extentions.KekDataExtension;
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
            Keklist.getInstance().getLogger().warning(Keklist.getTranslations().get("plan.not-enabled"));
        } catch (IllegalArgumentException exception) {
            Keklist.getInstance().getLogger().severe(Keklist.getTranslations().get("plan.error"));
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
