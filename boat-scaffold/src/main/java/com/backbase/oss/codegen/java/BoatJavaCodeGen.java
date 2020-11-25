package com.backbase.oss.codegen.java;

import lombok.Getter;
import lombok.Setter;
import org.openapitools.codegen.CliOption;
import org.openapitools.codegen.languages.JavaClientCodegen;

public class BoatJavaCodeGen extends JavaClientCodegen {
    public static final String NAME = "boat-java";

    public static final String USE_CLASS_LEVEL_BEAN_VALIDATION = "useClassLevelBeanValidation";
    public static final String USE_WITH_MODIFIERS = "useWithModifiers";

    /**
     * Add @Validated to class-level Api interfaces. Defaults to false
     */
    @Setter
    @Getter
    protected boolean useClassLevelBeanValidation;

    /**
     * Whether to use {@code with} prefix for pojos modifiers.
     */
    @Setter
    @Getter
    protected boolean useWithModifiers;

    public BoatJavaCodeGen() {
        this.embeddedTemplateDir = this.templateDir = NAME;

        this.cliOptions.add(CliOption.newBoolean(USE_CLASS_LEVEL_BEAN_VALIDATION,
            "Add @Validated to class-level Api interfaces", this.useClassLevelBeanValidation));
        this.cliOptions.add(CliOption.newBoolean(USE_WITH_MODIFIERS,
            "Whether to use \"with\" prefix for POJO modifiers", this.useWithModifiers));
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public void processOpts() {
        super.processOpts();

        if (this.additionalProperties.containsKey(USE_CLASS_LEVEL_BEAN_VALIDATION)) {
            this.useClassLevelBeanValidation = convertPropertyToBoolean(USE_CLASS_LEVEL_BEAN_VALIDATION);
        }
        if (this.additionalProperties.containsKey(USE_WITH_MODIFIERS)) {
            this.useWithModifiers = convertPropertyToBoolean(USE_WITH_MODIFIERS);
        }

        writePropertyBack(USE_CLASS_LEVEL_BEAN_VALIDATION, this.useClassLevelBeanValidation);
        writePropertyBack(USE_WITH_MODIFIERS, this.useWithModifiers);
    }
}
