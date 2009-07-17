package org.knime.ext.ssh;

import org.eclipse.core.runtime.Plugin;
import org.eclipse.jsch.core.IJSchService;
import org.knime.core.node.NodeLogger;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

/**
 * The activator class controls the plug-in life cycle
 */
public class ExtSSHNodeActivator extends Plugin {

    /** The plug-in ID. */
    public static final String PLUGIN_ID = "org.knime.ext.ssh";

    // The shared instance
    private static ExtSSHNodeActivator plugin;

    private IJSchService m_ijschService;

    /**
     * The constructor
     */
    public ExtSSHNodeActivator() {
        // empty
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void start(final BundleContext context) throws Exception {
        super.start(context);
        plugin = this;
        NodeLogger.getLogger(ExtSSHNodeActivator.class).info(
                "Starting Plug-in " + PLUGIN_ID);
        BundleContext bundleContext = getBundle().getBundleContext();
        ServiceReference service =
                bundleContext.getServiceReference(IJSchService.class.getName());
        m_ijschService = (IJSchService)bundleContext.getService(service);
    }

    /**
     * @return the JSch service.
     */
    public IJSchService getIJSchService() {
        return m_ijschService;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void stop(final BundleContext context) throws Exception {
        plugin = null;
        super.stop(context);
    }

    /**
     * Returns the shared instance
     *
     * @return the shared instance
     */
    public static ExtSSHNodeActivator getDefault() {
        return plugin;
    }

}
