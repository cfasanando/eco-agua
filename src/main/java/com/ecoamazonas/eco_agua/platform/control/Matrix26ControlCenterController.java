package com.ecoamazonas.eco_agua.platform.control;

import com.ecoamazonas.eco_agua.platform.PlatformBusinessClient;
import com.ecoamazonas.eco_agua.platform.PlatformBusinessClientRepository;
import com.ecoamazonas.eco_agua.platform.PlatformClientModule;
import com.ecoamazonas.eco_agua.platform.PlatformClientModuleRepository;
import com.ecoamazonas.eco_agua.platform.PlatformModuleCatalog;
import com.ecoamazonas.eco_agua.platform.PlatformModuleCatalogRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/control-center")
@ConditionalOnProperty(name = "matrix26.control-center.enabled", havingValue = "true")
public class Matrix26ControlCenterController {

    private final Matrix26InstanceHealthService healthService;
    private final PlatformBusinessClientRepository clientRepository;
    private final PlatformModuleCatalogRepository moduleRepository;
    private final PlatformClientModuleRepository clientModuleRepository;
    private final Matrix26ControlCenterProperties properties;

    public Matrix26ControlCenterController(
            Matrix26InstanceHealthService healthService,
            PlatformBusinessClientRepository clientRepository,
            PlatformModuleCatalogRepository moduleRepository,
            PlatformClientModuleRepository clientModuleRepository,
            Matrix26ControlCenterProperties properties
    ) {
        this.healthService = healthService;
        this.clientRepository = clientRepository;
        this.moduleRepository = moduleRepository;
        this.clientModuleRepository = clientModuleRepository;
        this.properties = properties;
    }

    @GetMapping({"", "/dashboard"})
    public String dashboard(
            @RequestParam(value = "refresh", required = false) String refresh,
            Model model
    ) {
        List<Matrix26InstanceStatus> statuses = healthService.currentStatuses("1".equals(refresh));
        Map<Long, List<PlatformClientModule>> modulesByInstance = modulesByInstance(statuses.stream()
                .map(Matrix26InstanceStatus::instance)
                .toList());
        long totalModules = moduleRepository.findAllByActiveTrueOrderByAreaAscDisplayOrderAscNameAsc().size();

        model.addAttribute("activePage", "matrix26_dashboard");
        model.addAttribute("statuses", statuses);
        model.addAttribute("modulesByInstance", modulesByInstance);
        model.addAttribute("summary", healthService.buildSummary(statuses, totalModules));
        model.addAttribute("recentChecks", healthService.recentChecks());
        model.addAttribute("controlProperties", properties);
        return "control_center/dashboard";
    }

    @GetMapping("/instances")
    public String instances(
            @RequestParam(value = "refresh", required = false) String refresh,
            Model model
    ) {
        List<Matrix26InstanceStatus> statuses = healthService.currentStatuses("1".equals(refresh));
        model.addAttribute("activePage", "matrix26_instances");
        model.addAttribute("statuses", statuses);
        model.addAttribute("modulesByInstance", modulesByInstance(statuses.stream()
                .map(Matrix26InstanceStatus::instance)
                .toList()));
        model.addAttribute("summary", healthService.buildSummary(
                statuses,
                moduleRepository.findAllByActiveTrueOrderByAreaAscDisplayOrderAscNameAsc().size()
        ));
        return "control_center/instances";
    }

    @GetMapping("/modules")
    public String modules(Model model) {
        List<PlatformBusinessClient> instances = clientRepository.findByMonitorVisibleTrueOrderByCreatedAtDescIdDesc();
        List<PlatformModuleCatalog> modules = moduleRepository.findAllByActiveTrueOrderByAreaAscDisplayOrderAscNameAsc();
        model.addAttribute("activePage", "matrix26_modules");
        model.addAttribute("instances", instances);
        model.addAttribute("modules", modules);
        Map<Long, List<PlatformClientModule>> modulesByInstance = modulesByInstance(instances);
        model.addAttribute("modulesByInstance", modulesByInstance);
        model.addAttribute("moduleKeysByInstance", moduleKeysByInstance(modulesByInstance));
        return "control_center/modules";
    }

    @GetMapping("/settings")
    public String settings(Model model) {
        model.addAttribute("activePage", "matrix26_settings");
        model.addAttribute("controlProperties", properties);
        model.addAttribute("instanceCount", clientRepository.count());
        model.addAttribute("moduleCount", moduleRepository.count());
        return "control_center/settings";
    }

    private Map<Long, Set<String>> moduleKeysByInstance(Map<Long, List<PlatformClientModule>> assignments) {
        Map<Long, Set<String>> result = new LinkedHashMap<>();
        assignments.forEach((instanceId, values) -> result.put(
                instanceId,
                values.stream()
                        .filter(PlatformClientModule::isEnabled)
                        .map(item -> item.getModule().getModuleKey())
                        .collect(Collectors.toCollection(java.util.LinkedHashSet::new))
        ));
        return result;
    }

    private Map<Long, List<PlatformClientModule>> modulesByInstance(List<PlatformBusinessClient> instances) {
        Map<Long, List<PlatformClientModule>> result = new LinkedHashMap<>();
        for (PlatformBusinessClient instance : instances) {
            result.put(instance.getId(), clientModuleRepository.findClientModules(instance.getId()));
        }
        return result;
    }
}
