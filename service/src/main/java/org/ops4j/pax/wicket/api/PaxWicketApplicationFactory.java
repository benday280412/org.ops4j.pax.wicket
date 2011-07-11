/**
 * Copyright OPS4J
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may obtain
 * a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ops4j.pax.wicket.api;

import static org.ops4j.lang.NullArgumentException.validateNotEmpty;
import static org.ops4j.lang.NullArgumentException.validateNotNull;
import static org.ops4j.pax.wicket.api.Constants.APPLICATION_NAME;
import static org.ops4j.pax.wicket.api.Constants.HOMEPAGE_CLASSNAME;
import static org.ops4j.pax.wicket.api.Constants.MOUNTPOINT;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.wicket.Page;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.protocol.http.IWebApplicationFactory;
import org.apache.wicket.protocol.http.WebApplication;
import org.apache.wicket.protocol.http.WicketFilter;
import org.apache.wicket.settings.IApplicationSettings;
import org.apache.wicket.settings.IDebugSettings;
import org.apache.wicket.settings.IExceptionSettings;
import org.apache.wicket.settings.IFrameworkSettings;
import org.apache.wicket.settings.IMarkupSettings;
import org.apache.wicket.settings.IPageSettings;
import org.apache.wicket.settings.IRequestCycleSettings;
import org.apache.wicket.settings.IResourceSettings;
import org.apache.wicket.settings.ISecuritySettings;
import org.apache.wicket.settings.ISessionSettings;
import org.ops4j.pax.wicket.internal.DelegatingClassResolver;
import org.ops4j.pax.wicket.internal.PageMounterTracker;
import org.ops4j.pax.wicket.internal.PaxWicketPageFactory;
import org.ops4j.pax.wicket.internal.injection.ComponentInstantiationListenerFacade;
import org.ops4j.pax.wicket.internal.injection.DelegatingComponentInstanciationListener;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;

public final class PaxWicketApplicationFactory implements IWebApplicationFactory, ManagedService {

    private static final String[] APPLICATION_FACTORY_SERVICE_NAMES = {
        PaxWicketApplicationFactory.class.getName(), ManagedService.class.getName()
    };

    private final BundleContext bundleContext;
    private Class<? extends Page> homepageClass;
    private final Properties properties;

    private PaxWicketPageFactory pageFactory;
    private DelegatingClassResolver delegatingClassResolver;
    private DelegatingComponentInstanciationListener delegatingComponentInstanciationListener;

    private ServiceRegistration registration;

    private Class<? extends WebPage> signinPage;
    private Map<?, ?> contextParams;
    private PageMounter pageMounter;
    private final ApplicationFactory applicationFactory;

    /**
     * Construct an instance of {@code PaxWicketApplicationFactory} with the specified arguments.
     * 
     * @param context The bundle context. This argument must not be {@code null}.
     * @param homepageClass The homepage class. This argument must not be {@code null}.
     * @param mountPoint The mount point. This argument must not be be {@code null}.
     * @param applicationName The application name. This argument must not be {@code null}.
     * @param applicationFactory An alternative method to create an application service and maintain almost full control
     *        about the application
     * 
     * @throws IllegalArgumentException Thrown if one or some or all arguments are {@code null}.
     * @since 1.0.0
     */
    public PaxWicketApplicationFactory(
            BundleContext context,
            Class<? extends Page> homepageClass,
            String mountPoint,
            String applicationName, ApplicationFactory applicationFactory)
        throws IllegalArgumentException {
        this(context, homepageClass, mountPoint, applicationName, applicationFactory, null, null);
    }

    /**
     * Construct an instance of {@code PaxWicketApplicationFactory} with the specified arguments.
     * 
     * @param context The bundle context. This argument must not be {@code null}.
     * @param homepageClass The homepage class. This argument must not be {@code null}.
     * @param mountPoint The mount point. This argument must not be be {@code null}.
     * @param applicationName The application name. This argument must not be {@code null}.
     * @param applicationFactory An alternative method to create an application service and maintain almost full control
     *        about the application
     * @param contextParams allows to define inital contextParams as in web.xml
     * 
     * @throws IllegalArgumentException Thrown if one or some or all arguments are {@code null}.
     * @since 1.0.0
     */
    public PaxWicketApplicationFactory(
            BundleContext context,
            Class<? extends Page> homepageClass,
            String mountPoint,
            String applicationName, ApplicationFactory applicationFactory, Map<?, ?> contextParams,
            String defaultInjectionSource)
        throws IllegalArgumentException {
        validateNotNull(context, "context");
        validateNotNull(homepageClass, "homepageClass");
        validateNotNull(mountPoint, "mountPoint");
        validateNotEmpty(applicationName, "applicationName");

        properties = new Properties();
        this.applicationFactory = applicationFactory;

        this.homepageClass = homepageClass;
        bundleContext = context;

        setMountPoint(mountPoint);
        setApplicationName(applicationName);

        String homepageClassName = homepageClass.getName();
        properties.setProperty(HOMEPAGE_CLASSNAME, homepageClassName);

        this.contextParams = contextParams;
    }

    /**
     * Clear resources used by this {@code PaxWicketApplicationFactory} instance.
     * <p>
     * Note: dispose does not unregister this {@code PaxWicketApplicationFactory} instance from the OSGi container.
     * </p>
     * 
     * @since 1.0.0
     */
    public final void dispose() {
        synchronized (this) {
            pageFactory.dispose();
            delegatingClassResolver.dispose();
            delegatingComponentInstanciationListener.dispose();
        }
    }

    /**
     * Returns the mount point that the wicket application will be accessible. This method must not return {@code null}
     * string.
     * 
     * @return The mount point that the wicket application will be accessible.
     * 
     * @since 1.0.0
     */
    public final String getMountPoint() {
        synchronized (this) {
            return properties.getProperty(MOUNTPOINT);
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public final void updated(Dictionary config)
        throws ConfigurationException {
        if (config == null) {
            synchronized (this) {
                registration.setProperties(properties);
            }

            return;
        }

        String classname = (String) config.get(HOMEPAGE_CLASSNAME);
        if (classname == null) {
            synchronized (this) {
                String homepageClassName = homepageClass.getName();
                config.put(HOMEPAGE_CLASSNAME, homepageClassName);
            }
        } else {
            synchronized (this) {
                try {
                    Bundle bundle = bundleContext.getBundle();
                    homepageClass = bundle.loadClass(classname);
                } catch (ClassNotFoundException e) {
                    throw new ConfigurationException(HOMEPAGE_CLASSNAME, "Class not found in application bundle.", e);
                }
            }
        }

        String applicationName = (String) config.get(APPLICATION_NAME);
        if (applicationName == null) {
            String currentApplicationName;
            synchronized (this) {
                currentApplicationName = (String) properties.get(APPLICATION_NAME);
            }

            config.put(APPLICATION_NAME, currentApplicationName);
        } else {
            setApplicationName(applicationName);
        }

        String mountPoint = (String) config.get(MOUNTPOINT);
        if (mountPoint == null) {
            String currentMountPoint;
            synchronized (this) {
                currentMountPoint = properties.getProperty(MOUNTPOINT);
            }
            config.put(MOUNTPOINT, currentMountPoint);
        } else {
            setMountPoint(mountPoint);
        }

        registration.setProperties(config);
    }

    /**
     * Register this {@code PaxWicketApplicationFactory} instance to OSGi container.
     * 
     * @return The service registration.
     * 
     * @since 1.0.0
     */
    public final ServiceRegistration register() {
        registration = bundleContext.registerService(APPLICATION_FACTORY_SERVICE_NAMES, this, properties);
        return registration;
    }

    private void setApplicationName(String applicationName) {
        synchronized (this) {
            if (pageFactory != null) {
                pageFactory.dispose();
            }
            if (delegatingClassResolver != null) {
                delegatingClassResolver.dispose();
            }
            if (delegatingComponentInstanciationListener != null) {
                delegatingComponentInstanciationListener.dispose();
            }

            delegatingClassResolver = new DelegatingClassResolver(bundleContext, applicationName);
            delegatingClassResolver.intialize();

            delegatingComponentInstanciationListener =
                new DelegatingComponentInstanciationListener(bundleContext, applicationName);
            delegatingComponentInstanciationListener.intialize();

            pageFactory = new PaxWicketPageFactory(bundleContext, applicationName);
            pageFactory.initialize();

            properties.setProperty(APPLICATION_NAME, applicationName);
        }
    }

    private void setMountPoint(String mountPoint) {
        synchronized (this) {
            properties.put(MOUNTPOINT, mountPoint);
        }
    }

    /**
     * Returns the context params defined for this application. This is what you typically do in your web.xml using the
     * context-param tag.
     */
    public Map<?, ?> getContextParams() {
        return contextParams;
    }

    public final void setPageMounter(PageMounter pageMounter) {
        validateNotNull(pageMounter, "pageMounter");
        this.pageMounter = pageMounter;
    }

    private String getApplicationName() {
        return properties.getProperty(APPLICATION_NAME);
    }

    /**
     * Creates a web application.
     * 
     * @param filter The wicket filter.
     * 
     * @return The new web application.
     */
    public final WebApplication createApplication(WicketFilter filter) {
        synchronized (this) {
            return createWicketApplicationViaCustomFactory();
        }
    }

    private WebApplication createWicketApplicationViaCustomFactory() {
        WebApplication paxWicketApplication;
        paxWicketApplication = applicationFactory.createWebApplication(new ApplicationLifecycleListener() {
            private final List<ServiceRegistration> m_serviceRegistrations =
                new ArrayList<ServiceRegistration>();
            private PageMounterTracker mounterTracker;
            private ComponentInstantiationListenerFacade componentInstanciationListener;

            public void onInit(WebApplication wicketApplication) {
                componentInstanciationListener = new ComponentInstantiationListenerFacade(
                    delegatingComponentInstanciationListener);
                wicketApplication.addComponentInstantiationListener(componentInstanciationListener);

                IApplicationSettings applicationSettings = wicketApplication.getApplicationSettings();
                applicationSettings.setClassResolver(delegatingClassResolver);
                addWicketService(IApplicationSettings.class, applicationSettings);

                ISessionSettings sessionSettings = wicketApplication.getSessionSettings();
                sessionSettings.setPageFactory(pageFactory);
                addWicketService(ISessionSettings.class, sessionSettings);

                addWicketService(IDebugSettings.class, wicketApplication.getDebugSettings());
                addWicketService(IExceptionSettings.class, wicketApplication.getExceptionSettings());
                addWicketService(IFrameworkSettings.class, wicketApplication.getFrameworkSettings());
                addWicketService(IMarkupSettings.class, wicketApplication.getMarkupSettings());
                addWicketService(IPageSettings.class, wicketApplication.getPageSettings());
                addWicketService(IRequestCycleSettings.class, wicketApplication.getRequestCycleSettings());
                addWicketService(IResourceSettings.class, wicketApplication.getResourceSettings());
                addWicketService(ISecuritySettings.class, wicketApplication.getSecuritySettings());

                if (pageMounter != null) {
                    for (MountPointInfo bookmark : pageMounter.getMountPoints()) {
                        wicketApplication.mount(bookmark.getCodingStrategy());
                    }
                }

                // Now add a tracker so we can still mount pages later
                mounterTracker = new PageMounterTracker(bundleContext, wicketApplication, getApplicationName());
                mounterTracker.open();

            }

            private <T> void addWicketService(Class<T> service, T serviceImplementation) {
                Properties props = new Properties();

                // Note: This is kept for legacy
                props.setProperty("applicationId", getApplicationName());
                props.setProperty(APPLICATION_NAME, getApplicationName());

                String serviceName = service.getName();
                ServiceRegistration registration =
                    bundleContext.registerService(serviceName, serviceImplementation, props);
                m_serviceRegistrations.add(registration);
            }

            public void onDestroy(WebApplication wicketApplication) {
                wicketApplication.removeComponentInstantiationListener(componentInstanciationListener);

                if (mounterTracker != null) {
                    mounterTracker.close();
                    mounterTracker = null;
                }

                for (ServiceRegistration reg : m_serviceRegistrations) {
                    reg.unregister();
                }
                m_serviceRegistrations.clear();
            }
        });
        return paxWicketApplication;
    }

}
