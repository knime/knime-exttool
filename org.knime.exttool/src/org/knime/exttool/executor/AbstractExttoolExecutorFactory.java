/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2010
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
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
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
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
 *  propagated with or for interoperation with KNIME. The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ------------------------------------------------------------------------
 *
 * History
 *   Feb 15, 2010 (wiswedel): created
 */
package org.knime.exttool.executor;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.exttool.node.ExttoolSettings;

/** Factory for a custom {@link AbstractExttoolExecutor executor}.
 * Factories are registered by means of contributions to the extension point
 * {@value #EXTENSION_ID}.
 *
 * <p><b>Warning:</b> API needs review, subclassing outside this package
 * is currently not encouraged.
 *
 * @author Bernd Wiswedel, KNIME.com, Zurich, Switzerland
 */
public abstract class AbstractExttoolExecutorFactory {

    private static final NodeLogger LOGGER =
        NodeLogger.getLogger(AbstractExttoolExecutorFactory.class);

    /** Extension point ID. */
    public static final String EXTENSION_ID = "org.knime.exttool.executor";

    private static Map<String, AbstractExttoolExecutorFactory> factoryMap;

    /** IDs of all registered executors. By convention, the ID is the fully
     * qualified class name of the factory.
     * @return The set of registered IDs.
     * @see #get(String)
     */
    public static final Set<String> keySet() {
        ensureInitExtensions();
        return factoryMap.keySet();
    }

    /** Look-up of registered executor.
     * @param id The ID (fully qualified class name) of the factory.
     * @return The associated factory, never <code>null</code>.
     * @throws InvalidSettingsException If <code>id</code> is null or unknown.
     */
    public static final AbstractExttoolExecutorFactory get(final String id)
        throws InvalidSettingsException {
        ensureInitExtensions();
        AbstractExttoolExecutorFactory result = factoryMap.get(id);
        if (result == null) {
            throw new InvalidSettingsException("Unknown executor ID \"" + id
                    + "\"; valid values are: "
                    + Arrays.toString(keySet().toArray()));
        }
        return result;
    }

    /** Collects all registered extensions when called the first time. */
    private static void ensureInitExtensions() {
        if (factoryMap == null) {
            Map<String, AbstractExttoolExecutorFactory> map =
                new LinkedHashMap<String, AbstractExttoolExecutorFactory>();
            IConfigurationElement[] config = Platform.getExtensionRegistry()
                .getConfigurationElementsFor(EXTENSION_ID);
            for (IConfigurationElement e : config) {
                try {
                    Object o = e.createExecutableExtension("factory");
                    String name = e.getAttribute("name");
                    if (o instanceof AbstractExttoolExecutorFactory) {
                        AbstractExttoolExecutorFactory f =
                            (AbstractExttoolExecutorFactory)o;
                        map.put(o.getClass().getName(), f);
                        LOGGER.debug("Adding exttool executor \""
                                + f.getClass().getName() + "\"");
                    } else {
                        LOGGER.warn("Ignoring contribution \"" + name
                                + "\" to extension point " + EXTENSION_ID
                                + ", not of expected class");
                    }
                } catch (CoreException e1) {
                    LOGGER.warn("Unable to instantiate contribution to "
                            + "extension point " + EXTENSION_ID, e1);
                }
            }
            factoryMap = Collections.unmodifiableMap(map);
        }
    }

    /** Create a new instance of an executor. This method is called often, e.g.
     * for each individual chunk to be processed.
     * @return A new executor.
     */
    public abstract AbstractExttoolExecutor createNewInstance();

    /** User-friendly name of the executor (shown to the user). This could be
     * "SSH Executor" or "Grid Executor".
     * @return The constant name of the executor.
     */
    public abstract String getName();

    /** Create a handle for the external tool's output data.
     * @param settings The settings for the current node (can mostly be ignored)
     * @param suggestOutFile The file suggested by the framework. This can
     * either be a user-entered name or a temp file name that is
     * automatically generated.
     * @return A new output handle.
     * @throws InvalidSettingsException If the settings are inappropriate.
     */
    public abstract OutputDataHandle createOutputDataHandle(
            final ExttoolSettings settings,
            final File suggestOutFile)
        throws InvalidSettingsException;

    /** Create a handle for the external tool's input data.
     * @param settings The settings for the current node (can mostly be ignored)
     * @param suggestInFile The file suggested by the framework. This can
     * either be a user-entered name or a temp file name that is
     * automatically generated.
     * @return A new input handle.
     * @throws InvalidSettingsException If the settings are inappropriate.
     */
    public abstract InputDataHandle createInputDataHandle(
            final ExttoolSettings settings,
            final File suggestInFile)
        throws InvalidSettingsException;

}
