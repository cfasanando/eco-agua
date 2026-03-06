package com.ecoamazonas.eco_agua.config;

import jakarta.persistence.*;

@Entity
@Table(name = "platform_setting")
public class PlatformSetting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // OJO: se llama "variable" como en tu tabla
    @Column(name = "variable", nullable = false, unique = true, length = 100)
    private String variable;

    @Column(name = "value", nullable = false, length = 4000)
    private String value;

    @Column(name = "type", length = 50)
    private String type;

    @Column(name = "category", length = 50)
    private String category;

    @Column(name = "description", length = 255)
    private String description;

    public Long getId() {
        return id;
    }

    public String getVariable() {
        return variable;
    }

    public void setVariable(String variable) {
        this.variable = variable;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
