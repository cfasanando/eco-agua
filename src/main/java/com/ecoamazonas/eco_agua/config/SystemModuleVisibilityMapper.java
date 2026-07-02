package com.ecoamazonas.eco_agua.config;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Maps Matrix26 module declarations to the runtime flags consumed by client sidebars.
 */
public final class SystemModuleVisibilityMapper {

    private SystemModuleVisibilityMapper() {
    }

    public static Map<String, Boolean> systemModuleFlags(Collection<String> moduleKeys, boolean restaurantProfile) {
        Set<String> keys = normalizeKeys(moduleKeys);
        boolean restaurant = restaurantProfile || any(keys, "restaurant", "restaurant_cash", "restaurant_qr", "restaurant_recipes", "restaurant_reservations");
        boolean sales = any(keys, "sales") || restaurant;
        boolean delivery = any(keys, "delivery") || any(keys, "restaurant_qr");
        boolean inventory = any(keys, "inventory") || restaurant;
        boolean finance = any(keys, "finance") || any(keys, "accounting", "cashflow", "fixed_costs", "break_even", "price_simulator") || restaurant;
        boolean marketing = any(keys, "marketing") || any(keys, "public_catalog", "blog", "testimonials", "academy");
        boolean hr = any(keys, "hr");
        boolean personalFinance = any(keys, "personal_finance", "gasto_claro");

        Map<String, Boolean> flags = new LinkedHashMap<>();
        flags.put("core", true);
        flags.put("dashboard", true);
        flags.put("business_overview", true);
        flags.put("monthly_followup", sales || finance || inventory || marketing || hr || restaurant);
        flags.put("commercial_daily", sales);

        flags.put("sales", sales);
        flags.put("crm", sales);
        flags.put("clients", sales);
        flags.put("promotions", any(keys, "promotions") || marketing);
        flags.put("delivery", delivery);
        flags.put("reorder", any(keys, "reorder"));

        flags.put("income", sales || finance);
        flags.put("expenses", finance || inventory || restaurant);
        flags.put("fixed_costs", any(keys, "fixed_costs") || restaurant);
        flags.put("suppliers", finance || inventory || restaurant);
        flags.put("finance", finance);
        flags.put("accounting", any(keys, "accounting"));
        flags.put("cashflow", any(keys, "cashflow") || finance);
        flags.put("break_even", any(keys, "break_even"));
        flags.put("price_simulator", any(keys, "price_simulator") || restaurant);

        flags.put("inventory", inventory);
        flags.put("products", inventory || any(keys, "public_catalog") || restaurant);
        flags.put("categories", inventory || any(keys, "public_catalog") || restaurant);
        flags.put("warehouse", inventory || any(keys, "warehouse"));
        flags.put("supplies", any(keys, "supplies", "restaurant_recipes") || restaurant);
        flags.put("containers", any(keys, "containers"));
        flags.put("production", any(keys, "production"));

        flags.put("restaurant", restaurant);
        flags.put("restaurant_cash", restaurant && (any(keys, "restaurant_cash") || !hasGranularRestaurantKey(keys)));
        flags.put("restaurant_qr", restaurant && (any(keys, "restaurant_qr") || !hasGranularRestaurantKey(keys)));
        flags.put("restaurant_recipes", restaurant && (any(keys, "restaurant_recipes") || !hasGranularRestaurantKey(keys)));
        flags.put("restaurant_reservations", restaurant && (any(keys, "restaurant_reservations") || !hasGranularRestaurantKey(keys)));

        flags.put("marketing", marketing);
        flags.put("blog", any(keys, "blog"));
        flags.put("academy", any(keys, "academy"));
        flags.put("testimonials", any(keys, "testimonials"));
        flags.put("public_site", any(keys, "public_site") || any(keys, "public_catalog", "blog", "academy", "testimonials") || restaurant);
        flags.put("public_catalog", any(keys, "public_catalog") || restaurant);

        flags.put("hr", hr);
        flags.put("users", hr);
        flags.put("roles_permissions", hr);
        flags.put("platform_settings", true);
        flags.put("personal_finance", personalFinance);

        return flags;
    }

    public static Map<String, Boolean> featureProperties(Collection<String> moduleKeys, boolean restaurantProfile) {
        Map<String, Boolean> moduleFlags = systemModuleFlags(moduleKeys, restaurantProfile);
        Map<String, Boolean> flags = new LinkedHashMap<>();
        flags.put("ecoagua.features.containers", enabled(moduleFlags, "containers"));
        flags.put("ecoagua.features.delivery", enabled(moduleFlags, "delivery"));
        flags.put("ecoagua.features.production", enabled(moduleFlags, "production"));
        flags.put("ecoagua.features.reorder", enabled(moduleFlags, "reorder"));
        flags.put("ecoagua.features.marketing", enabled(moduleFlags, "marketing"));
        flags.put("ecoagua.features.blog", enabled(moduleFlags, "blog"));
        flags.put("ecoagua.features.academy", enabled(moduleFlags, "academy"));
        flags.put("ecoagua.features.restaurant", enabled(moduleFlags, "restaurant"));
        flags.put("ecoagua.features.testimonials", enabled(moduleFlags, "testimonials"));
        flags.put("ecoagua.features.public-catalog", enabled(moduleFlags, "public_catalog"));
        flags.put("ecoagua.features.supplies", enabled(moduleFlags, "supplies"));
        flags.put("ecoagua.features.fixed-costs", enabled(moduleFlags, "fixed_costs"));
        flags.put("ecoagua.features.break-even", enabled(moduleFlags, "break_even"));
        flags.put("ecoagua.features.price-simulator", enabled(moduleFlags, "price_simulator"));
        flags.put("ecoagua.features.personal-finance", enabled(moduleFlags, "personal_finance"));
        return flags;
    }

    public static Set<String> normalizeKeys(Collection<String> values) {
        Set<String> result = new LinkedHashSet<>();
        if (values == null) {
            return result;
        }
        for (String value : values) {
            String normalized = normalize(value);
            if (!normalized.isBlank()) {
                result.add(normalized);
            }
        }
        return result;
    }

    public static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT).replace('-', '_');
    }

    private static boolean enabled(Map<String, Boolean> flags, String key) {
        return Boolean.TRUE.equals(flags.get(key));
    }

    private static boolean any(Set<String> keys, String... candidates) {
        for (String candidate : candidates) {
            if (keys.contains(normalize(candidate))) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasGranularRestaurantKey(Set<String> keys) {
        return any(keys, "restaurant_cash", "restaurant_qr", "restaurant_recipes", "restaurant_reservations");
    }
}
