package com.backbase.oss.boat;

import static java.util.stream.Collectors.groupingBy;
import static org.junit.Assert.assertEquals;

import com.backbase.oss.codegen.SpringCodeGenBB;
import org.junit.Test;
import org.openapitools.codegen.CliOption;

public class SpringCodeGenBBTests {

    @Test
    public void clientOptsUnicity() {
        SpringCodeGenBB gen = new SpringCodeGenBB();
        gen.cliOptions()
            .stream()
            .collect(groupingBy(CliOption::getOpt))
            .forEach((k, v) -> assertEquals(k + " is described multiple times", v.size(), 1));
    }
}