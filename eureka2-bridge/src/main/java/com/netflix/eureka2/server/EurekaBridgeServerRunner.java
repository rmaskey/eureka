package com.netflix.eureka2.server;

import com.google.inject.Module;
import com.google.inject.util.Modules;
import com.netflix.discovery.guice.EurekaModule;
import com.netflix.eureka2.server.module.CommonEurekaServerModule;
import com.netflix.eureka2.server.spi.ExtAbstractModule;
import com.netflix.eureka2.server.spi.ExtAbstractModule.ServerType;
import com.netflix.governator.DefaultGovernatorConfiguration;
import com.netflix.governator.Governator;
import com.netflix.governator.LifecycleInjector;
import com.netflix.governator.auto.ModuleListProviders;
import netflix.adminresources.resources.KaryonWebAdminModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.netflix.eureka2.server.config.ServerConfigurationNames.DEFAULT_CONFIG_PREFIX;

/**
 * A Bridge (Write) server that captures snapshots of Eureka 1.0 Data and replicates changes of the 1.0 data
 * to other Eureka Write servers.
 *
 * @author David Liu
 */
public class EurekaBridgeServerRunner extends EurekaServerRunner<EurekaBridgeServer> {

    private static final Logger logger = LoggerFactory.getLogger(EurekaBridgeServerRunner.class);

    public EurekaBridgeServerRunner(String name) {
        super(name, EurekaBridgeServer.class);
    }

    @Override
    public EurekaBridgeServer getEurekaServer() {
        return injector.getInstance(EurekaBridgeServer.class);
    }

    @Override
    protected LifecycleInjector createInjector() {
        Module applicationModule = Modules.combine(
                new CommonEurekaServerModule(name),
                new EurekaBridgeServerModule(DEFAULT_CONFIG_PREFIX),
                applyEurekaOverride(new EurekaModule()),  // eureka 1
                new KaryonWebAdminModule()
        );

        return Governator.createInjector(
                DefaultGovernatorConfiguration.builder()
                        .addProfile(ServerType.Bridge.name())
                        .addModuleListProvider(ModuleListProviders.forServiceLoader(ExtAbstractModule.class))
                        .build(),
                applicationModule
        );
    }

    /**
     * Primary purpose of this class is to override Eureka1 bindings in the integration test scenarios.
     */
    protected Module applyEurekaOverride(Module module) {
        return module;
    }

    public static void main(String[] args) {
        logger.info("Eureka 2.0 Dashboard Server");
        EurekaBridgeServerRunner runner = new EurekaBridgeServerRunner("eureka-bridge-server");
        if (runner.start()) {
            runner.awaitTermination();
        }
        // In case we have non-daemon threads running
        System.exit(0);
    }
}