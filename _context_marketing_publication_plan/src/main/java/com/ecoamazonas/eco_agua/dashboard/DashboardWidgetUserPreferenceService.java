package com.ecoamazonas.eco_agua.dashboard;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class DashboardWidgetUserPreferenceService {

    private final DashboardWidgetUserSettingRepository userSettingRepository;
    private final DashboardWidgetAccessService widgetAccessService;

    public DashboardWidgetUserPreferenceService(
            DashboardWidgetUserSettingRepository userSettingRepository,
            DashboardWidgetAccessService widgetAccessService
    ) {
        this.userSettingRepository = userSettingRepository;
        this.widgetAccessService = widgetAccessService;
    }

    @Transactional(readOnly = true)
    public DashboardWidgetPreferenceResponse getPreferences(Authentication authentication) {
        String username = username(authentication);
        List<DashboardWidgetAccessService.DashboardWidgetDefinition> allowedDefinitions = widgetAccessService.getAllowedWidgetDefinitions(authentication);
        Set<String> allowedKeys = allowedDefinitions.stream()
                .map(DashboardWidgetAccessService.DashboardWidgetDefinition::key)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        Map<String, DashboardWidgetUserSetting> settingsByWidget = userSettingRepository.findByUsername(username).stream()
                .filter(setting -> allowedKeys.contains(setting.getWidgetKey()))
                .collect(Collectors.toMap(
                        DashboardWidgetUserSetting::getWidgetKey,
                        Function.identity(),
                        (left, right) -> right,
                        LinkedHashMap::new
                ));

        Map<String, List<String>> columns = new LinkedHashMap<>();
        columns.put("left", new ArrayList<>());
        columns.put("right", new ArrayList<>());

        Map<String, Boolean> collapsed = new LinkedHashMap<>();
        Map<String, Boolean> hidden = new LinkedHashMap<>();

        List<DashboardWidgetOption> widgets = new ArrayList<>();
        for (DashboardWidgetAccessService.DashboardWidgetDefinition definition : allowedDefinitions) {
            DashboardWidgetUserSetting setting = settingsByWidget.get(definition.key());
            boolean isHidden = setting != null && setting.isHidden();
            boolean isCollapsed = setting != null && setting.isCollapsed();
            String column = normalizeColumn(setting != null ? setting.getColumnPosition() : null);

            hidden.put(definition.key(), isHidden);
            collapsed.put(definition.key(), isCollapsed);
            if (!isHidden) {
                columns.computeIfAbsent(column, ignored -> new ArrayList<>()).add(definition.key());
            }

            widgets.add(new DashboardWidgetOption(
                    definition.key(),
                    definition.area(),
                    definition.title(),
                    definition.description()
            ));
        }

        sortColumnsByStoredOrder(columns, settingsByWidget);

        return new DashboardWidgetPreferenceResponse(widgets, columns, collapsed, hidden);
    }

    @Transactional
    public void savePreferences(Authentication authentication, DashboardWidgetPreferenceRequest request) {
        String username = username(authentication);
        List<DashboardWidgetAccessService.DashboardWidgetDefinition> allowedDefinitions = widgetAccessService.getAllowedWidgetDefinitions(authentication);
        Set<String> allowedKeys = allowedDefinitions.stream()
                .map(DashboardWidgetAccessService.DashboardWidgetDefinition::key)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        Map<String, String> columnByWidget = new HashMap<>();
        Map<String, Integer> sortOrderByWidget = new HashMap<>();

        Map<String, List<String>> requestedColumns = request != null && request.getColumns() != null
                ? request.getColumns()
                : Map.of();

        for (Map.Entry<String, List<String>> entry : requestedColumns.entrySet()) {
            String column = normalizeColumn(entry.getKey());
            List<String> widgetKeys = entry.getValue() != null ? entry.getValue() : List.of();
            for (int index = 0; index < widgetKeys.size(); index++) {
                String widgetKey = widgetKeys.get(index);
                if (!allowedKeys.contains(widgetKey)) {
                    continue;
                }
                columnByWidget.put(widgetKey, column);
                sortOrderByWidget.put(widgetKey, index + 1);
            }
        }

        Map<String, Boolean> collapsed = request != null && request.getCollapsed() != null
                ? request.getCollapsed()
                : Map.of();
        Map<String, Boolean> hidden = request != null && request.getHidden() != null
                ? request.getHidden()
                : Map.of();

        for (String widgetKey : allowedKeys) {
            DashboardWidgetUserSetting setting = userSettingRepository
                    .findByUsernameAndWidgetKey(username, widgetKey)
                    .orElseGet(() -> new DashboardWidgetUserSetting(username, widgetKey));

            setting.setHidden(Boolean.TRUE.equals(hidden.get(widgetKey)));
            setting.setCollapsed(Boolean.TRUE.equals(collapsed.get(widgetKey)));
            setting.setColumnPosition(columnByWidget.getOrDefault(widgetKey, setting.getColumnPosition()));
            setting.setSortOrder(sortOrderByWidget.getOrDefault(widgetKey, setting.getSortOrder()));
            userSettingRepository.save(setting);
        }
    }

    @Transactional
    public void resetPreferences(Authentication authentication) {
        userSettingRepository.deleteByUsername(username(authentication));
    }

    private void sortColumnsByStoredOrder(
            Map<String, List<String>> columns,
            Map<String, DashboardWidgetUserSetting> settingsByWidget
    ) {
        for (List<String> widgetKeys : columns.values()) {
            widgetKeys.sort(Comparator.comparingInt(widgetKey -> {
                DashboardWidgetUserSetting setting = settingsByWidget.get(widgetKey);
                Integer order = setting != null ? setting.getSortOrder() : null;
                return order != null ? order : Integer.MAX_VALUE;
            }));
        }
    }

    private String normalizeColumn(String value) {
        if ("right".equalsIgnoreCase(value)) {
            return "right";
        }
        return "left";
    }

    private String username(Authentication authentication) {
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            throw new IllegalStateException("Authenticated user is required to save dashboard preferences.");
        }
        return authentication.getName();
    }

    public static class DashboardWidgetPreferenceRequest {
        private Map<String, List<String>> columns = new LinkedHashMap<>();
        private Map<String, Boolean> collapsed = new LinkedHashMap<>();
        private Map<String, Boolean> hidden = new LinkedHashMap<>();

        public Map<String, List<String>> getColumns() {
            return columns;
        }

        public void setColumns(Map<String, List<String>> columns) {
            this.columns = columns;
        }

        public Map<String, Boolean> getCollapsed() {
            return collapsed;
        }

        public void setCollapsed(Map<String, Boolean> collapsed) {
            this.collapsed = collapsed;
        }

        public Map<String, Boolean> getHidden() {
            return hidden;
        }

        public void setHidden(Map<String, Boolean> hidden) {
            this.hidden = hidden;
        }
    }

    public record DashboardWidgetPreferenceResponse(
            List<DashboardWidgetOption> allowedWidgets,
            Map<String, List<String>> columns,
            Map<String, Boolean> collapsed,
            Map<String, Boolean> hidden
    ) {
    }

    public record DashboardWidgetOption(
            String key,
            String area,
            String title,
            String description
    ) {
    }
}
