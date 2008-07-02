/*  Copyright 2008 Edward Yakop.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ops4j.pax.wicket.util.classResolver;

import java.util.Properties;
import org.apache.wicket.application.IClassResolver;
import static org.ops4j.lang.NullArgumentException.validateNotNull;
import static org.ops4j.pax.wicket.api.ContentSource.APPLICATION_NAME;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import static org.osgi.framework.Constants.SERVICE_PID;
import org.osgi.framework.ServiceRegistration;

/**
 * {@code BundleClassResolverHelper} is a helper to register {@code IClassResolver}.
 *
 * @author edward.yakop@gmail.com
 * @since 0.5.4
 */
public final class BundleClassResolverHelper
{

    private static final String SERVICE_NAME = IClassResolver.class.getName();

    private final BundleContext m_bundleContext;
    private final Properties m_serviceProperties;

    private ServiceRegistration m_serviceRegistration;

    /**
     * Construct an instance of {@code BundleClassResolver}.
     *
     * @param bundleContext The bundle context. This argument must not be {@code null}.
     *
     * @throws IllegalArgumentException Thrown if the specified {@code bundleContext} argument is {@code null}.
     * @since 0.5.4
     */
    public BundleClassResolverHelper( BundleContext bundleContext )
        throws IllegalArgumentException
    {
        validateNotNull( bundleContext, "bundle" );

        m_bundleContext = bundleContext;
        m_serviceProperties = new Properties();
    }

    /**
     * Sets the service pid of this {@code BundleClassResolverHelper} instance.
     * This is useful if this class resolver needs to be wired to multiple pax-wicket applications.
     *
     * @param servicePid The service pid.
     *
     * @see Constants#SERVICE_PID
     * @since 0.5.4
     */
    public final void setServicePid( String servicePid )
    {
        if( servicePid == null )
        {
            m_serviceProperties.remove( SERVICE_PID );
        }
        else
        {
            m_serviceProperties.setProperty( SERVICE_PID, servicePid );
        }

        synchronized( this )
        {
            if( m_serviceRegistration != null )
            {
                m_serviceRegistration.setProperties( m_serviceProperties );
            }
        }
    }

    /**
     * @return The service pid of this {@code BundleClassResolverHelper}. Returns {@code null} if not set.
     *
     * @since 0.5.4
     */
    public final String getServicePid()
    {
        return m_serviceProperties.getProperty( SERVICE_PID );
    }

    /**
     * Sets the application nane.
     *
     * @param applicationNames The application names.
     *
     * @since 0.5.4
     */
    public final void setApplicationName( String... applicationNames )
    {
        if( applicationNames == null )
        {
            m_serviceProperties.remove( APPLICATION_NAME );
        }
        else
        {
            m_serviceProperties.put( APPLICATION_NAME, applicationNames );
        }

        synchronized( this )
        {
            if( m_serviceRegistration != null )
            {
                m_serviceRegistration.setProperties( m_serviceProperties );
            }
        }
    }

    /**
     * Register class resolver.
     *
     * @since 0.5.4
     */
    public final void register()
    {
        synchronized( this )
        {
            if( m_serviceRegistration == null )
            {
                BundleClassResolver resolver = new BundleClassResolver( m_bundleContext );
                m_serviceRegistration = m_bundleContext.registerService( SERVICE_NAME, resolver, m_serviceProperties );
            }
        }
    }

    /**
     * Unregister class resolver.
     *
     * @since 0.5.4
     */
    public final void unregister()
    {
        synchronized( this )
        {
            if( m_serviceRegistration != null )
            {
                m_serviceRegistration.unregister();
                m_serviceRegistration = null;
            }
        }
    }

    private static final class BundleClassResolver
        implements IClassResolver
    {

        private final BundleContext m_bundleContext;

        private BundleClassResolver( BundleContext bundleContext )
        {
            m_bundleContext = bundleContext;
        }

        public final Class resolveClass( String classname )
            throws ClassNotFoundException
        {
            Bundle bundle = m_bundleContext.getBundle();
            return bundle.loadClass( classname );
        }
    }
}
