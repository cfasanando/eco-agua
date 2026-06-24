package com.ecoamazonas.eco_agua.platform.control.appearance;

import com.ecoamazonas.eco_agua.platform.PlatformBusinessClient;
import com.ecoamazonas.eco_agua.platform.PlatformBusinessClientRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@ConditionalOnProperty(name = "matrix26.control-center.enabled", havingValue = "true")
public class Matrix26AppearanceCatalogService {

    private static final String ACTIVE = "ACTIVE";

    private final Matrix26ThemeCatalogRepository themeRepository;
    private final Matrix26LayoutCatalogRepository layoutRepository;
    private final Matrix26InstanceAppearanceRepository appearanceRepository;
    private final Matrix26InstanceAppearanceHistoryRepository historyRepository;
    private final Matrix26InstanceAppearanceDraftRepository draftRepository;
    private final PlatformBusinessClientRepository clientRepository;

    public Matrix26AppearanceCatalogService(
            Matrix26ThemeCatalogRepository themeRepository,
            Matrix26LayoutCatalogRepository layoutRepository,
            Matrix26InstanceAppearanceRepository appearanceRepository,
            Matrix26InstanceAppearanceHistoryRepository historyRepository,
            Matrix26InstanceAppearanceDraftRepository draftRepository,
            PlatformBusinessClientRepository clientRepository
    ) {
        this.themeRepository = themeRepository;
        this.layoutRepository = layoutRepository;
        this.appearanceRepository = appearanceRepository;
        this.historyRepository = historyRepository;
        this.draftRepository = draftRepository;
        this.clientRepository = clientRepository;
    }

    public Matrix26AppearanceOverview overview() {
        List<Matrix26InstanceAppearance> appearances = appearanceRepository.findAll();
        long published = appearances.stream().filter(item -> "PUBLISHED".equals(item.getStatus())).count();
        long drafts = draftRepository.count();
        return new Matrix26AppearanceOverview(
                themeRepository.count(),
                layoutRepository.count(),
                clientRepository.count(),
                published,
                drafts
        );
    }

    public List<Matrix26ThemeCatalog> activeThemes() {
        return themeRepository.findByStatusOrderByDisplayOrderAscNameAsc(ACTIVE);
    }

    public List<Matrix26LayoutCatalog> activeLayouts() {
        return layoutRepository.findByStatusOrderByAreaAscDisplayOrderAscNameAsc(ACTIVE);
    }

    public Map<String, List<Matrix26LayoutCatalog>> layoutsByArea() {
        Map<String, List<Matrix26LayoutCatalog>> grouped = new LinkedHashMap<>();
        for (Matrix26LayoutCatalog layout : activeLayouts()) {
            grouped.computeIfAbsent(layout.getArea(), ignored -> new ArrayList<>()).add(layout);
        }
        return grouped;
    }

    public Matrix26ThemeCatalog getTheme(String code) {
        return themeRepository.findByCode(code)
                .orElseThrow(() -> new IllegalArgumentException("El theme solicitado no existe."));
    }

    public Matrix26LayoutCatalog getLayout(String code) {
        return layoutRepository.findByCode(code)
                .orElseThrow(() -> new IllegalArgumentException("El layout solicitado no existe."));
    }

    public List<Matrix26ThemeUsageView> themeUsage() {
        List<Matrix26InstanceAppearance> appearances = appearanceRepository.findAll();
        return activeThemes().stream()
                .map(theme -> new Matrix26ThemeUsageView(
                        theme,
                        appearances.stream().filter(item -> theme.getCode().equals(item.getPublicThemeCode())).count(),
                        appearances.stream().filter(item -> theme.getCode().equals(item.getAdminThemeCode())).count()
                ))
                .toList();
    }

    public List<Matrix26LayoutUsageView> layoutUsage() {
        List<Matrix26InstanceAppearance> appearances = appearanceRepository.findAll();
        return activeLayouts().stream()
                .map(layout -> new Matrix26LayoutUsageView(
                        layout,
                        appearances.stream().filter(item -> usesLayout(item, layout.getCode())).count()
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Matrix26InstanceAppearanceView> instanceAppearances() {
        List<PlatformBusinessClient> instances = clientRepository.findAllByOrderByBusinessNameAsc();
        Map<Long, Matrix26InstanceAppearance> byInstance = appearanceRepository.findAll().stream()
                .collect(Collectors.toMap(item -> item.getInstance().getId(), Function.identity()));
        Map<String, Matrix26ThemeCatalog> themes = themeRepository.findAll().stream()
                .collect(Collectors.toMap(Matrix26ThemeCatalog::getCode, Function.identity()));
        Map<String, Matrix26LayoutCatalog> layouts = layoutRepository.findAll().stream()
                .collect(Collectors.toMap(Matrix26LayoutCatalog::getCode, Function.identity()));

        List<Matrix26InstanceAppearanceView> result = new ArrayList<>();
        for (PlatformBusinessClient instance : instances) {
            Matrix26InstanceAppearance appearance = byInstance.get(instance.getId());
            if (appearance == null) {
                continue;
            }
            result.add(toView(instance, appearance, themes, layouts));
        }
        return result;
    }

    @Transactional(readOnly = true)
    public Matrix26InstanceAppearanceView instanceAppearance(Long instanceId) {
        PlatformBusinessClient instance = clientRepository.findById(instanceId)
                .orElseThrow(() -> new IllegalArgumentException("La instancia no existe."));
        Matrix26InstanceAppearance appearance = appearanceRepository.findByInstance_Id(instanceId)
                .orElseThrow(() -> new IllegalArgumentException("La instancia todavía no tiene una apariencia registrada."));
        Map<String, Matrix26ThemeCatalog> themes = themeRepository.findAll().stream()
                .collect(Collectors.toMap(Matrix26ThemeCatalog::getCode, Function.identity()));
        Map<String, Matrix26LayoutCatalog> layouts = layoutRepository.findAll().stream()
                .collect(Collectors.toMap(Matrix26LayoutCatalog::getCode, Function.identity()));
        return toView(instance, appearance, themes, layouts);
    }

    public List<Matrix26InstanceAppearanceHistory> history(Long instanceId) {
        return historyRepository.findTop20ByInstance_IdOrderByVersionDesc(instanceId);
    }

    @Transactional(readOnly = true)
    public List<Matrix26InstanceAppearanceView> recentAppearances() {
        return instanceAppearances().stream().limit(4).toList();
    }

    private Matrix26InstanceAppearanceView toView(
            PlatformBusinessClient instance,
            Matrix26InstanceAppearance appearance,
            Map<String, Matrix26ThemeCatalog> themes,
            Map<String, Matrix26LayoutCatalog> layouts
    ) {
        return new Matrix26InstanceAppearanceView(
                instance,
                appearance,
                themes.get(appearance.getPublicThemeCode()),
                layouts.get(appearance.getPublicLayoutCode()),
                themes.get(appearance.getAdminThemeCode()),
                layouts.get(appearance.getAdminLayoutCode()),
                layouts.get(appearance.getLoginLayoutCode())
        );
    }

    private boolean usesLayout(Matrix26InstanceAppearance appearance, String code) {
        return code.equals(appearance.getPublicLayoutCode())
                || code.equals(appearance.getAdminLayoutCode())
                || code.equals(appearance.getLoginLayoutCode());
    }
}
