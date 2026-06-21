package com.ecoamazonas.eco_agua.restaurant;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component("restaurantFormat")
public class RestaurantFormat {

    public String money(BigDecimal value) {
        return RestaurantDecimalFormat.money(value);
    }

    public String preciseMoney(BigDecimal value) {
        return RestaurantDecimalFormat.preciseMoney(value);
    }

    public String quantity(BigDecimal value) {
        return RestaurantDecimalFormat.quantity(value);
    }

    public String percentage(BigDecimal value) {
        return RestaurantDecimalFormat.percentage(value);
    }
}
