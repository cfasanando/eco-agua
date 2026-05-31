package com.ecoamazonas.eco_agua.dashboard;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DashboardWidgetUserSettingRepository extends JpaRepository<DashboardWidgetUserSetting, Long> {

    List<DashboardWidgetUserSetting> findByUsername(String username);

    Optional<DashboardWidgetUserSetting> findByUsernameAndWidgetKey(String username, String widgetKey);

    void deleteByUsername(String username);
}
