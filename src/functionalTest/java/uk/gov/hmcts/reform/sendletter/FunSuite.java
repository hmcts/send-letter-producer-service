package uk.gov.hmcts.reform.sendletter;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.contrib.java.lang.system.EnvironmentVariables;

/**
 * Abstract class for functional test suite.
 * Microsoft Application Insights SDK uses APPLICATION_INSIGHTS_IKEY as an instrumentation key for the client.
 * Need to manually set it so the app context loads.
 */
@SuppressWarnings("PMD.AbstractClassWithoutAbstractMethod")
public abstract class FunSuite {

    @ClassRule
    public static final EnvironmentVariables variables = new EnvironmentVariables();

    @BeforeClass
    public static void setUp() {
        variables.set("APPLICATION_INSIGHTS_IKEY", "some-key");
    }
}
