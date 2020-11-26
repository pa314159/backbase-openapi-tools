package com.backbase.oss.codegen.java;

import java.io.File;

import lombok.Getter;
import lombok.Setter;
import org.openapitools.codegen.CliOption;
import org.openapitools.codegen.CodegenModel;
import org.openapitools.codegen.CodegenParameter;
import org.openapitools.codegen.CodegenProperty;
import org.openapitools.codegen.SupportingFile;
import org.openapitools.codegen.languages.JavaClientCodegen;

public class BoatJavaCodeGen extends JavaClientCodegen {
    public static final String NAME = "boat-java";

    public static final String USE_WITH_MODIFIERS = "useWithModifiers";
    public static final String USE_SET_FOR_UNIQUE_ITEMS = "useSetForUniqueItems";

    public static final String USE_CLASS_LEVEL_BEAN_VALIDATION = "useClassLevelBeanValidation";
    public static final String USE_JACKSON_CONVERSION = "useJacksonConversion";
    public static final String BACKBASE_SERVICE_ID = "backbaseServiceId";

    /**
     * Whether to use {@code with} prefix for pojos modifiers.
     */
    @Setter
    @Getter
    protected boolean useWithModifiers;

    @Setter
    @Getter
    protected boolean useSetForUniqueItems;

    /**
     * Add @Validated to class-level Api interfaces. Defaults to false
     */
    @Setter
    @Getter
    protected boolean useClassLevelBeanValidation;

    /**
     * Whether to use Jackson to convert query parameters to String.
     */
    @Setter
    @Getter
    protected boolean useJacksonConversion;

    @Setter
    @Getter
    protected String backbaseServiceId;

    public BoatJavaCodeGen() {
        this.embeddedTemplateDir = this.templateDir = NAME;

        this.cliOptions.add(CliOption.newBoolean(USE_CLASS_LEVEL_BEAN_VALIDATION,
            "Add @Validated to class-level Api interfaces", this.useClassLevelBeanValidation));
        this.cliOptions.add(CliOption.newBoolean(USE_WITH_MODIFIERS,
            "Whether to use \"with\" prefix for POJO modifiers", this.useWithModifiers));
        this.cliOptions.add(CliOption.newBoolean(USE_SET_FOR_UNIQUE_ITEMS,
            "Use java.util.Set for arrays that have uniqueItems set to true", this.useSetForUniqueItems));
        this.cliOptions.add(CliOption.newBoolean(USE_JACKSON_CONVERSION,
            "Whether to use Jackson to convert query parameters to String", this.useJacksonConversion));
        this.cliOptions.add(CliOption.newString(BACKBASE_SERVICE_ID,
            "Generate Backbase specific bean factory for ApiClient"));
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public void processOpts() {
        super.processOpts();

        if (this.additionalProperties.containsKey(USE_WITH_MODIFIERS)) {
            this.useWithModifiers = convertPropertyToBoolean(USE_WITH_MODIFIERS);
        }
        writePropertyBack(USE_WITH_MODIFIERS, this.useWithModifiers);

        if (this.additionalProperties.containsKey(USE_SET_FOR_UNIQUE_ITEMS)) {
            this.useSetForUniqueItems = convertPropertyToBoolean(USE_SET_FOR_UNIQUE_ITEMS);
        }
        writePropertyBack(USE_SET_FOR_UNIQUE_ITEMS, this.useSetForUniqueItems);

        if (this.useSetForUniqueItems) {
            this.typeMapping.put("set", "java.util.Set");

            this.importMapping.put("Set", "java.util.Set");
            this.importMapping.put("LinkedHashSet", "java.util.LinkedHashSet");
        }

        if (RESTTEMPLATE.equals(getLibrary())) {
            if (this.additionalProperties.containsKey(USE_CLASS_LEVEL_BEAN_VALIDATION)) {
                this.useClassLevelBeanValidation = convertPropertyToBoolean(USE_CLASS_LEVEL_BEAN_VALIDATION);
            }
            if (this.additionalProperties.containsKey(USE_JACKSON_CONVERSION)) {
                this.useJacksonConversion = convertPropertyToBoolean(USE_JACKSON_CONVERSION);

                if (this.useJacksonConversion) {
                    this.supportingFiles.removeIf(f -> f.templateFile.equals("RFC3339DateFormat.mustache"));
                }
            }
            if (this.additionalProperties.containsKey(BACKBASE_SERVICE_ID)) {
                this.backbaseServiceId = (String) this.additionalProperties.get(BACKBASE_SERVICE_ID);

                this.supportingFiles.add(new SupportingFile("ApiClientConfiguration.mustache",
                    getInvokerPackage().replace('.', File.separatorChar), "ApiClientConfiguration.java"));
            }

            writePropertyBack(USE_CLASS_LEVEL_BEAN_VALIDATION, this.useClassLevelBeanValidation);
            writePropertyBack(USE_JACKSON_CONVERSION, this.useJacksonConversion);
        }

        if (!getLibrary().startsWith("jersey")) {
            this.supportingFiles.removeIf(f -> f.templateFile.equals("ServerConfiguration.mustache"));
            this.supportingFiles.removeIf(f -> f.templateFile.equals("ServerVariable.mustache"));
        }
    }

    @Override
    public void postProcessModelProperty(CodegenModel model, CodegenProperty p) {
        super.postProcessModelProperty(model, p);

        if (p.isContainer) {
            if (this.useSetForUniqueItems && p.getUniqueItems()) {
                p.containerType = "set";
                p.baseType = "java.util.Set";
                p.dataType = "java.util.Set<" + p.items.dataType + ">";
                p.datatypeWithEnum = "java.util.Set<" + p.items.datatypeWithEnum + ">";
                p.defaultValue = "new " + "java.util.LinkedHashSet<>()";
            }
        }
    }

    @Override
    public void postProcessParameter(CodegenParameter p) {
        super.postProcessParameter(p);

        if (p.isContainer) {
            // XXX the model set baseType to the container type, why is this different?
            p.baseType = p.dataType.replaceAll("^([^<]+)<.+>$", "$1");

            if (this.useSetForUniqueItems && p.getUniqueItems()) {
                p.baseType = "java.util.Set";
                p.dataType = "java.util.Set<" + p.items.dataType + ">";
                p.datatypeWithEnum = "java.util.Set<" + p.items.datatypeWithEnum + ">";
                p.defaultValue = "new " + "java.util.LinkedHashSet<>()";
            }
        }
    }
}
