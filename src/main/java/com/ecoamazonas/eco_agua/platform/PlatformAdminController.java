package com.ecoamazonas.eco_agua.platform;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Set;

@Controller
@RequestMapping("/admin/platform")
public class PlatformAdminController {

    private final PlatformManagementService platformManagementService;
    private final PlatformProvisioningService platformProvisioningService;
    private final PlatformRuntimeService platformRuntimeService;

    public PlatformAdminController(PlatformManagementService platformManagementService,
                                   PlatformProvisioningService platformProvisioningService,
                                   PlatformRuntimeService platformRuntimeService) {
        this.platformManagementService = platformManagementService;
        this.platformProvisioningService = platformProvisioningService;
        this.platformRuntimeService = platformRuntimeService;
    }

    @GetMapping({"", "/clients"})
    public String clients(Model model) {
        model.addAttribute("activePage", "platform_clients");
        model.addAttribute("summary", platformManagementService.buildSummary());
        model.addAttribute("clients", platformManagementService.listClients());
        return "admin/platform/clients";
    }

    @GetMapping("/clients/new")
    public String newClient(@RequestParam(value = "templateId", required = false) Long templateId,
                            Model model) {
        PlatformClientForm form = platformManagementService.newClientForm(templateId);
        Set<String> selectedModules = platformManagementService.recommendedModuleKeys(form.getTemplateId());
        addClientFormAttributes(model, form, selectedModules);
        return "admin/platform/client_form";
    }

    @PostMapping("/clients")
    public String createClient(@ModelAttribute PlatformClientForm form,
                               @RequestParam(value = "selectedModules", required = false) List<String> selectedModules,
                               RedirectAttributes redirectAttributes,
                               Model model) {
        try {
            PlatformBusinessClient client = platformManagementService.createClient(form, selectedModules);
            redirectAttributes.addFlashAttribute("successMessage", "Negocio creado correctamente. Revisa sus módulos activos antes de crear la base de datos.");
            return "redirect:/admin/platform/clients/" + client.getId();
        } catch (IllegalArgumentException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            addClientFormAttributes(model, form, selectedModules == null ? Set.of() : Set.copyOf(selectedModules));
            return "admin/platform/client_form";
        }
    }

    @GetMapping("/clients/{id}")
    public String clientDetail(@PathVariable Long id, Model model) {
        PlatformBusinessClient client = platformManagementService.getClient(id);
        model.addAttribute("activePage", "platform_clients");
        model.addAttribute("client", client);
        model.addAttribute("clientModules", platformManagementService.getClientModules(id));
        model.addAttribute("groupedModules", platformManagementService.groupedModules());
        model.addAttribute("selectedModuleKeys", platformManagementService.getClientModules(id).stream()
                .map(item -> item.getModule().getModuleKey())
                .toList());
        return "admin/platform/client_detail";
    }

    @PostMapping("/clients/{id}/modules")
    public String updateClientModules(@PathVariable Long id,
                                      @RequestParam(value = "selectedModules", required = false) List<String> selectedModules,
                                      RedirectAttributes redirectAttributes) {
        platformManagementService.updateClientModules(id, selectedModules);
        redirectAttributes.addFlashAttribute("successMessage", "Módulos del negocio actualizados correctamente.");
        return "redirect:/admin/platform/clients/" + id;
    }

    @GetMapping("/clients/{id}/provisioning")
    public String clientProvisioning(@PathVariable Long id, Model model) {
        PlatformProvisioningPlan plan = platformProvisioningService.buildPlan(id);
        model.addAttribute("activePage", "platform_clients");
        model.addAttribute("client", plan.client());
        model.addAttribute("plan", plan);
        model.addAttribute("logs", platformProvisioningService.listLogs(id));
        return "admin/platform/client_provisioning";
    }

    @PostMapping("/clients/{id}/provisioning/create-database")
    public String createClientDatabase(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            platformProvisioningService.createDatabase(id);
            redirectAttributes.addFlashAttribute("successMessage", "Base de datos creada o validada correctamente.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/platform/clients/" + id + "/provisioning";
    }

    @PostMapping("/clients/{id}/provisioning/mark-structure-ready")
    public String markStructureReady(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        platformProvisioningService.markStructureReady(id);
        redirectAttributes.addFlashAttribute("successMessage", "Estructura marcada como lista. Ahora puedes aplicar el SQL de configuración inicial.");
        return "redirect:/admin/platform/clients/" + id + "/provisioning";
    }

    @PostMapping("/clients/{id}/provisioning/mark-active")
    public String markClientActive(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        platformProvisioningService.markActive(id);
        redirectAttributes.addFlashAttribute("successMessage", "Negocio marcado como activo para demo o pruebas internas.");
        return "redirect:/admin/platform/clients/" + id + "/provisioning";
    }

    @PostMapping("/clients/{id}/provisioning/reset")
    public String resetProvisioning(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        platformProvisioningService.resetProvisioning(id);
        redirectAttributes.addFlashAttribute("successMessage", "Estado de aprovisionamiento reiniciado. No se eliminó ninguna base de datos.");
        return "redirect:/admin/platform/clients/" + id + "/provisioning";
    }


    @GetMapping("/clients/{id}/provisioning/create-database.sql")
    public ResponseEntity<String> downloadCreateDatabaseSql(@PathVariable Long id) {
        PlatformProvisioningPlan plan = platformProvisioningService.buildPlan(id);
        return downloadableSql(plan.createDatabaseFileName(), plan.createDatabaseSql());
    }

    @GetMapping("/clients/{id}/provisioning/bootstrap.sql")
    public ResponseEntity<String> downloadBootstrapSql(@PathVariable Long id) {
        PlatformProvisioningPlan plan = platformProvisioningService.buildPlan(id);
        return downloadableSql(plan.bootstrapFileName(), plan.bootstrapSql());
    }


    @GetMapping("/clients/{id}/runtime")
    public String clientRuntime(@PathVariable Long id, Model model) {
        PlatformRuntimePlan runtime = platformRuntimeService.buildPlan(id);
        model.addAttribute("activePage", "platform_clients");
        model.addAttribute("client", runtime.client());
        model.addAttribute("runtime", runtime);
        return "admin/platform/client_runtime";
    }

    @PostMapping("/clients/{id}/runtime")
    public String saveClientRuntime(@PathVariable Long id,
                                    @RequestParam(value = "runtimeProfile", required = false) String runtimeProfile,
                                    @RequestParam(value = "runtimePort", required = false) Integer runtimePort,
                                    @RequestParam(value = "publicUrl", required = false) String publicUrl,
                                    RedirectAttributes redirectAttributes) {
        try {
            platformRuntimeService.saveRuntimeSettings(id, runtimeProfile, runtimePort, publicUrl);
            redirectAttributes.addFlashAttribute("successMessage", "Perfil de ejecución guardado correctamente.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/platform/clients/" + id + "/runtime";
    }

    @GetMapping("/clients/{id}/runtime/application.properties")
    public ResponseEntity<String> downloadRuntimeApplication(@PathVariable Long id) {
        PlatformRuntimePlan runtime = platformRuntimeService.buildPlan(id);
        return downloadableText(runtime.applicationFileName(), runtime.applicationProperties(), "text/plain");
    }

    @GetMapping("/clients/{id}/runtime/run-script.sh")
    public ResponseEntity<String> downloadRuntimeScript(@PathVariable Long id) {
        PlatformRuntimePlan runtime = platformRuntimeService.buildPlan(id);
        return downloadableText(runtime.runScriptFileName(), runtime.runScript(), "text/x-shellscript");
    }

    @GetMapping("/templates")
    public String templates(Model model) {
        model.addAttribute("activePage", "platform_templates");
        model.addAttribute("templateSummaries", platformManagementService.templateSummaries());
        model.addAttribute("templateModulesByTemplate", platformManagementService.templateModulesByTemplate());
        return "admin/platform/templates";
    }

    @GetMapping("/modules")
    public String modules(Model model) {
        model.addAttribute("activePage", "platform_modules");
        model.addAttribute("groupedModules", platformManagementService.groupedModules());
        return "admin/platform/modules";
    }


    private ResponseEntity<String> downloadableSql(String filename, String content) {
        return downloadableText(filename, content, MediaType.TEXT_PLAIN_VALUE);
    }

    private ResponseEntity<String> downloadableText(String filename, String content, String contentType) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(contentType));
        headers.setContentDisposition(ContentDisposition.attachment().filename(filename).build());
        return ResponseEntity.ok()
                .headers(headers)
                .body(content);
    }
    private void addClientFormAttributes(Model model, PlatformClientForm form, Set<String> selectedModuleKeys) {
        model.addAttribute("activePage", "platform_clients");
        model.addAttribute("form", form);
        model.addAttribute("templates", platformManagementService.listTemplates());
        model.addAttribute("groupedModules", platformManagementService.groupedModules());
        model.addAttribute("selectedModuleKeys", selectedModuleKeys);
    }
}
