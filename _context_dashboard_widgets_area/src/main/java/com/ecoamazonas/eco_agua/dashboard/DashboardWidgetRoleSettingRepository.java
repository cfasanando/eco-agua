package com.ecoamazonas.eco_agua.dashboard;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface DashboardWidgetRoleSettingRepository extends JpaRepository<DashboardWidgetRoleSetting, Long> {

    Optional<DashboardWidgetRoleSetting> findByRoleCodeAndWidgetKey(String roleCode, String widgetKey);

    List<DashboardWidgetRoleSetting> findByRoleCodeIn(Collection<String> roleCodes);
}
