/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME AG, Zurich, Switzerland
 *  Website: http://www.knime.com; Email: contact@knime.com
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, Version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
 *  Hence, KNIME and ECLIPSE are both independent programs and are not
 *  derived from each other. Should, however, the interpretation of the
 *  GNU GPL Version 3 ("License") under any applicable laws result in
 *  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
 *  you the additional permission to use and propagate KNIME together with
 *  ECLIPSE with only the license terms in place for ECLIPSE applying to
 *  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
 *  license terms of ECLIPSE themselves allow for the respective use and
 *  propagation of ECLIPSE together with KNIME.
 *
 *  Additional permission relating to nodes for KNIME that extend the Node
 *  Extension (and in particular that are based on subclasses of NodeModel,
 *  NodeDialog, and NodeView) and that only interoperate with KNIME through
 *  standard APIs ("Nodes"):
 *  Nodes are deemed to be separate and independent programs and to not be
 *  covered works.  Notwithstanding anything to the contrary in the
 *  License, the License does not apply to Nodes, you are not required to
 *  license Nodes under the License, and you are granted a license to
 *  prepare and propagate Nodes, in each case even if such Nodes are
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ------------------------------------------------------------------------
 */

package org.knime.ext.ssh;

import org.apache.commons.lang3.concurrent.ConcurrentException;
import org.apache.commons.lang3.concurrent.LazyInitializer;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.jsch.core.IJSchService;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeLogger.LEVEL;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Logger;

/**
 * The activator class controls the plug-in life cycle.
 */
public class ExtSSHNodeActivator extends Plugin {

    /** The plug-in ID. */
    public static final String PLUGIN_ID = "org.knime.ext.ssh";

    // The shared instance
    private static ExtSSHNodeActivator plugin;

    private LazyInitializer<IJSchService> m_ijschServiceInitializer;

    /**
     * {@inheritDoc}
     */
    @Override
    public void start(final BundleContext context) throws Exception {
        super.start(context);
        plugin = this;
        m_ijschServiceInitializer = new LazyInitializer<IJSchService>() {

            @Override
            protected IJSchService initialize() throws ConcurrentException {
                BundleContext bundleContext = getBundle().getBundleContext();
                ServiceReference<IJSchService> service = bundleContext.getServiceReference(IJSchService.class);
                IJSchService ijschService = bundleContext.getService(service);

                // Set the logger
                final JSchLogger jSchLogger = new JSchLogger();
                JSch.setLogger(jSchLogger);

                return ijschService;
            }
        };
    }

    /**
     * @return the JSch service.
     */
    public IJSchService getIJSchService() {
        try {
            return m_ijschServiceInitializer.get();
        } catch (ConcurrentException e) {
            throw new RuntimeException(e.getMessage(), e.getCause());
        }
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
     * Returns the shared instance.
     *
     * @return the shared instance
     */
    public static ExtSSHNodeActivator getDefault() {
        return plugin;
    }

    /** A {@link Logger} implementation that logs to the KNIME log. */
    private static final class JSchLogger implements Logger {

        final NodeLogger LOGGER = NodeLogger.getLogger("JSch");

        @Override
        public boolean isEnabled(final int jschLevel) {
            return LOGGER.isEnabledFor(getLevel(jschLevel));
        }

        @Override
        public void log(final int jschLevel, final String message) {
            final LEVEL level = getLevel(jschLevel);
            switch (level) {
                case DEBUG:
                    LOGGER.debug(message);
                    break;
                case INFO:
                    LOGGER.info(message);
                    break;
                case WARN:
                    LOGGER.warn(message);
                    break;
                case ERROR:
                    LOGGER.error(message);
                    break;
                case FATAL:
                    LOGGER.fatal(message);
                    break;
                default:
                    // All cases handled
                    break;
            }

        }

        private static NodeLogger.LEVEL getLevel(final int jschLevel) {
            switch (jschLevel) {
                case Logger.DEBUG:
                    return LEVEL.DEBUG;

                case Logger.INFO:
                    return LEVEL.INFO;

                case Logger.WARN:
                    return LEVEL.WARN;

                case Logger.ERROR:
                    return LEVEL.ERROR;

                case Logger.FATAL:
                    return LEVEL.FATAL;

                default:
                    throw new IllegalStateException("Invalide JSch log level: " + jschLevel);
            }
        }
    }
}
