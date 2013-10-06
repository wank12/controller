/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yang.logback.config;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.management.InstanceAlreadyExistsException;
import javax.management.JMX;
import javax.management.ObjectName;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.config.api.jmx.ObjectNameUtil;
import org.opendaylight.controller.config.manager.impl.AbstractConfigTest;
import org.opendaylight.controller.config.manager.impl.factoriesresolver.HardcodedModuleFactoriesResolver;
import org.opendaylight.controller.config.util.ConfigTransactionClient;
import org.opendaylight.controller.config.util.ConfigTransactionJMXClient;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.rolling.FixedWindowRollingPolicy;
import ch.qos.logback.core.rolling.SizeBasedTriggeringPolicy;

import com.google.common.collect.Lists;

public class LogbackModuleWithInitialConfigurationTest extends
        AbstractConfigTest {

    private LogbackModuleFactory factory;

    @Before
    public void setUp() throws IOException, ClassNotFoundException {

        factory = new LogbackModuleFactory();
        super.initConfigTransactionManagerImpl(new HardcodedModuleFactoriesResolver(
                factory));
    }

    /**
     * Tests that initial configuration was changed. Changed attributes:
     * location, fileName, duplicateInsertTries
     *
     */
    @Test
    public void test() throws Exception {

        createBeans();

        ConfigTransactionClient transaction = configRegistryClient
                .createTransaction();

        LogbackModuleMXBean bean = JMX.newMXBeanProxy(
                ManagementFactory.getPlatformMBeanServer(),
                transaction.lookupConfigBean("logback", "singleton"),
                LogbackModuleMXBean.class);
        assertEquals(1, bean.getConsoleAppenderTO().size());
        assertEquals(1, bean.getRollingFileAppenderTO().size());
        assertEquals(1, bean.getLoggerTO().size());

        RollingFileAppenderTO rolling = new RollingFileAppenderTO();
        RollingFileAppenderTO old = bean
                .getRollingFileAppenderTO().get(0);
        rolling.setAppend(old.getAppend());
        rolling.setEncoderPattern(old.getEncoderPattern());
        rolling.setFileName("target/logFile1.log");
        rolling.setFileNamePattern("target/%i.log");
        rolling.setMaxFileSize(old.getMaxFileSize());
        rolling.setMinIndex(old.getMinIndex());
        rolling.setMaxIndex(old.getMaxIndex());
        rolling.setName("FILE");

        ConsoleAppenderTO console = new ConsoleAppenderTO();
        console.setEncoderPattern("%date %level [%thread] %logger{10} %msg%n");
        console.setName("SYSTEM");
        console.setThresholdFilter("DEBUG");

        bean.setConsoleAppenderTO(Lists.newArrayList(console));
        bean.setRollingFileAppenderTO(Lists.newArrayList(rolling));

        LoggerTO logger = new LoggerTO();
        logger.setLevel("INFO");
        logger.setLoggerName("logger");
        logger.setAppenders(Lists.newArrayList("SYSTEM"));
        List<LoggerTO> loggers = Lists
                .newArrayList(logger);
        bean.setLoggerTO(loggers);

        transaction.commit();

        LogbackModuleMXBean logback = configRegistryClient.newMXBeanProxy(
                ObjectNameUtil.createReadOnlyModuleON("logback", "singleton"),
                LogbackModuleMXBean.class);


        List<RollingFileAppenderTO> rollingList = logback
                .getRollingFileAppenderTO();
        assertEquals(1, rollingList.size());

        RollingFileAppenderTO rollingApp = rollingList
                .get(0);
        assertEquals(rollingApp.getFileName(), "target/logFile1.log");
        assertEquals(rollingApp.getName(), "FILE");

        List<ConsoleAppenderTO> consoleList = logback
                .getConsoleAppenderTO();
        assertEquals(1, consoleList.size());

        ConsoleAppenderTO consoleApp = consoleList
                .get(0);
        assertEquals(consoleApp.getThresholdFilter(), "DEBUG");
        assertEquals(consoleApp.getName(), "SYSTEM");

        loggers = logback.getLoggerTO();
        assertEquals(1, loggers.size());

    }

    public ObjectName createBeans() throws JoranException,
            InstanceAlreadyExistsException, IOException {

        LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();

        JoranConfigurator configurator = new JoranConfigurator();
        configurator.setContext(lc);
        configurator
                .doConfigure("src/test/resources/simple_config_logback.xml");
        File f = new File("target/it");
        if (f.exists())
            FileUtils.cleanDirectory(f);
        ch.qos.logback.classic.Logger logger = lc.getLogger(ch.qos.logback.classic.Logger.ROOT_LOGGER_NAME);
        ch.qos.logback.core.rolling.RollingFileAppender<ILoggingEvent> fileAppender = (ch.qos.logback.core.rolling.RollingFileAppender<ILoggingEvent>) logger
                .getAppender("VARLOGFILE");
        fileAppender.start();

        ch.qos.logback.core.ConsoleAppender<ILoggingEvent> consoleAppender = (ch.qos.logback.core.ConsoleAppender<ILoggingEvent>) logger
                .getAppender("STDOUT");
        consoleAppender.start();
        List<RollingFileAppenderTO> rollingAppenders = new ArrayList<>();
        RollingFileAppenderTO rollingApp = new RollingFileAppenderTO();
        rollingApp.setAppend(fileAppender.isAppend());
        PatternLayoutEncoder enc = (PatternLayoutEncoder) fileAppender
                .getEncoder();
        rollingApp.setEncoderPattern(enc.getPattern());
        rollingApp.setFileName(fileAppender.getFile());
        FixedWindowRollingPolicy rollingPolicy = (FixedWindowRollingPolicy) fileAppender
                .getRollingPolicy();
        rollingApp.setMaxIndex(rollingPolicy.getMaxIndex());
        rollingApp.setMinIndex(rollingPolicy.getMinIndex());
        SizeBasedTriggeringPolicy<ILoggingEvent> triggeringPolicy = (SizeBasedTriggeringPolicy<ILoggingEvent>) fileAppender
                .getTriggeringPolicy();
        rollingApp.setMaxFileSize(triggeringPolicy.getMaxFileSize());
        rollingApp.setName(fileAppender.getName());
        rollingApp.setFileNamePattern(rollingPolicy.getFileNamePattern());
        rollingAppenders.add(rollingApp);

        assertEquals(rollingApp.getFileName(), "target/osgi.log");
        assertEquals(rollingApp.getMaxFileSize(), "50MB");
        assertEquals(rollingApp.getName(), "VARLOGFILE");

        List<ConsoleAppenderTO> consoleAppenders = new ArrayList<>();
        ConsoleAppenderTO consoleApp = new ConsoleAppenderTO();
        enc = (PatternLayoutEncoder) consoleAppender.getEncoder();
        consoleApp.setEncoderPattern(enc.getPattern());
        consoleApp.setName(consoleAppender.getName());
        consoleApp.setThresholdFilter("ALL");
        consoleAppenders.add(consoleApp);

        List<LoggerTO> loggersDTOs = new ArrayList<>();
        LoggerTO log = new LoggerTO();
        log.setAppenders(Arrays.asList(fileAppender.getName(),
                consoleApp.getName()));

        log.setLevel(logger.getLevel().toString());
        log.setLoggerName(logger.getName());
        loggersDTOs.add(log);

        ConfigTransactionJMXClient transaction = configRegistryClient
                .createTransaction();
        ObjectName nameCreated = transaction.createModule(
                factory.getImplementationName(), "singleton");
        LogbackModuleMXBean bean = transaction.newMXBeanProxy(nameCreated,
                LogbackModuleMXBean.class);

        bean.setLoggerTO(loggersDTOs);
        bean.setRollingFileAppenderTO(rollingAppenders);
        bean.setConsoleAppenderTO(consoleAppenders);

        transaction.commit();

        return nameCreated;
    }
}