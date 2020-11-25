package com.backbase.oss.codegen.java;

import org.junit.Assert;
import org.junit.Test;
import org.openapitools.codegen.CliOption;
import org.openapitools.codegen.CodegenModel;
import org.openapitools.codegen.CodegenParameter;
import org.openapitools.codegen.CodegenProperty;

import static com.backbase.oss.codegen.java.BoatJavaCodeGen.*;

import static java.util.stream.Collectors.groupingBy;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import java.util.Map;

public class BoatJavaCodeGenTests {

    @Test
    public void clientOptsUnicity() {
        final BoatJavaCodeGen gen = new BoatJavaCodeGen();
        gen.cliOptions()
            .stream()
            .collect(groupingBy(CliOption::getOpt))
            .forEach((k, v) -> Assert.assertEquals(k + " is described multiple times", v.size(), 1));
    }

    @Test
    public void processOptsWithRestTemplate() {
        final BoatJavaCodeGen gen = new BoatJavaCodeGen();
        final Map<String, Object> options = gen.additionalProperties();

        gen.setLibrary("resttemplate");
        options.put(USE_WITH_MODIFIERS, "true");
        options.put(USE_SET_FOR_UNIQUE_ITEMS, "true");
        options.put(USE_CLASS_LEVEL_BEAN_VALIDATION, "true");
        options.put(USE_JACKSON_CONVERSION, "true");
        options.put(BACKBASE_SERVICE_ID, "service-id");

        gen.processOpts();

        assertThat(gen.useWithModifiers, is(true));
        assertThat(gen.useSetForUniqueItems, is(true));
        assertThat(gen.useClassLevelBeanValidation, is(true));
        assertThat(gen.useJacksonConversion, is(true));
        assertThat(gen.backbaseServiceId, is("service-id"));
    }

    @Test
    public void processOptsWithoutRestTemplate() {
        final BoatJavaCodeGen gen = new BoatJavaCodeGen();
        final Map<String, Object> options = gen.additionalProperties();

        options.put(USE_WITH_MODIFIERS, "true");
        options.put(USE_SET_FOR_UNIQUE_ITEMS, "true");
        options.put(USE_CLASS_LEVEL_BEAN_VALIDATION, "true");
        options.put(USE_JACKSON_CONVERSION, "true");
        options.put(BACKBASE_SERVICE_ID, "service-id");

        gen.processOpts();

        assertThat(gen.useWithModifiers, is(true));
        assertThat(gen.useSetForUniqueItems, is(true));
        assertThat(gen.useClassLevelBeanValidation, is(false));
        assertThat(gen.useJacksonConversion, is(false));
        assertThat(gen.backbaseServiceId, is(nullValue()));
    }

    @Test
    public void uniquePropertyToSet() {
        final BoatJavaCodeGen gen = new BoatJavaCodeGen();
        final CodegenProperty prop = new CodegenProperty();

        gen.useSetForUniqueItems = true;
        prop.isContainer = true;
        prop.setUniqueItems(true);
        prop.items = new CodegenProperty();
        prop.items.dataType = "String";
        prop.baseType = "java.util.List";
        prop.dataType = "java.util.List<String>";

        gen.postProcessModelProperty(new CodegenModel(), prop);

        assertThat(prop.containerType, is("set"));
        assertThat(prop.baseType, is("java.util.Set"));
        assertThat(prop.dataType, is("java.util.Set<String>"));
    }

    @Test
    public void uniqueParameterToSet() {
        final BoatJavaCodeGen gen = new BoatJavaCodeGen();
        final CodegenParameter param = new CodegenParameter();

        gen.useSetForUniqueItems = true;
        param.isContainer = true;
        param.setUniqueItems(true);
        param.items = new CodegenProperty();
        param.items.dataType = "String";
        param.baseType = "java.util.List<String>";
        param.dataType = "java.util.List<String>";

        gen.postProcessParameter(param);

        assertThat(param.baseType, is("java.util.Set"));
        assertThat(param.dataType, is("java.util.Set<String>"));
    }
}
