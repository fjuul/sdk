package com.fjuul.sdk.test;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;

import com.fjuul.sdk.test.utils.TestTimberTree;

import timber.log.Timber;

// NOTE: unfortunately, robolectric does not support JUnit's @ClassRule. That's why we have to use the class inheritance
// for test suites (see https://github.com/robolectric/robolectric/issues/2637)
public abstract class LoggableTestSuite {
    static protected final TestTimberTree LOGGER = new TestTimberTree();

    @BeforeClass
    public static void setupTestLogger() {
        Timber.plant(LOGGER);
    }

    @Before
    public void resetTestLogger() {
        LOGGER.reset();
    }

    @AfterClass
    public static void removeTestLogger() {
        Timber.uprootAll();
    }
}
