/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2013
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
 *   Jan 20, 2010 (wiswedel): created
 */
package org.knime.exttool.filetype;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;


/**
 * A file type factories adds support for different data types and file
 * formats. It is used to write the input data for the external tool in a custom
 * format (such as CSV) and also parse the result. Factories are registered
 * as contribution to the extension point {@value #EXTENSION_ID}.
 *
 * <p><b>Warning:</b> API needs review, subclassing and usage outside this
 * package is currently not encouraged.
 *
 * @author Bernd Wiswedel, KNIME.com, Zurich, Switzerland
 */
public abstract class AbstractFileTypeFactory {

    /** Extension point ID for file types. */
    public static final String EXTENSION_ID = "org.knime.exttool.filetype";

    private static final NodeLogger LOGGER =
        NodeLogger.getLogger(AbstractFileTypeFactory.class);

    private static Map<String, AbstractFileTypeFactory> factoryMapWrite;
    private static Map<String, AbstractFileTypeFactory> factoryMapRead;

    private static void ensureInitExtensions() {
        if (factoryMapWrite == null) {
            Map<String, AbstractFileTypeFactory> mapWrite =
                new TreeMap<String, AbstractFileTypeFactory>();
            Map<String, AbstractFileTypeFactory> mapRead =
                new TreeMap<String, AbstractFileTypeFactory>();
            IConfigurationElement[] config = Platform.getExtensionRegistry()
                .getConfigurationElementsFor(EXTENSION_ID);
            for (IConfigurationElement e : config) {
                try {
                    Object o = e.createExecutableExtension("factory");
                    if (o instanceof AbstractFileTypeFactory) {
                        AbstractFileTypeFactory factory =
                            (AbstractFileTypeFactory)o;
                        String id = factory.getID();
                        if (id == null) {
                            LOGGER.warn("Ignoring file type extension \""
                                    + o.getClass().getName()
                                    + "\"; ID must not be null");
                        }
                        String read = "";  // used to create, e.g. "rw"
                        String write = ""; // in the following if-statement
                        if (factory.canWrite()) {
                            mapWrite.put(id, factory);
                            write = "w";
                        }
                        if (factory.canRead()) {
                            mapRead.put(id, factory);
                            read = "r";
                        }
                        if ((read + write).length() > 0) {
                            LOGGER.debug("Added exttool file type extension \""
                                    + id + "\" (" + read + write + ")");
                        } else {
                            LOGGER.debug("Ignoring exttool file type extension "
                                    + "\"" + id + "\", neither read nor write "
                                    + "support");
                        }
                    } else {
                        LOGGER.warn("Ignoring contribution \""
                                + o.getClass().getName()
                                + "\" to extension point " + EXTENSION_ID
                                + ", not of expected class");
                    }
                } catch (CoreException coreException) {
                    LOGGER.warn("Unable to instantiate contribution to "
                            + "extension point " + EXTENSION_ID, coreException);
                }
            }
            factoryMapWrite = Collections.unmodifiableMap(mapWrite);
            factoryMapRead = Collections.unmodifiableMap(mapRead);
        }
    }

    /** Look up the file type factory associated with the given key. This
     * method will never return null, if the key is unknown or null, it will
     * throw an {@link InvalidSettingsException}.
     * @param name The key/class to look up
     *        (as defined by the extension point contribution)
     * @return The factory associated with the key.
     * @throws InvalidSettingsException If the key is unknown.
     */
    public static final AbstractFileTypeFactory get(final String name)
        throws InvalidSettingsException {
        ensureInitExtensions();
        AbstractFileTypeFactory result = factoryMapWrite.get(name);
        if (result == null) {
            throw new InvalidSettingsException("Invalid file type: " + name);
        }
        return result;
    }

    /** Get IDs (class name) of all factories for writing (input) file types.
     * @return The set of write IDs.
     */
    public static final Set<String> getWriteIDs() {
        ensureInitExtensions();
        return factoryMapWrite.keySet();
    }

    /** @return Get all write factories. */
    public static final Collection<AbstractFileTypeFactory>
        getWriteFactories() {
        ensureInitExtensions();
        return factoryMapWrite.values();
    }

    /** Get IDs (class name) of all factories for reading (output) file types.
     * @return The set of read IDs.
     */
    public static final Set<String> getReadIDs() {
        ensureInitExtensions();
        return factoryMapRead.keySet();
    }

    /** @return Get all read factories. */
    public static final Collection<AbstractFileTypeFactory> getReadFactories() {
        ensureInitExtensions();
        return factoryMapRead.values();
    }

    /** Get ID of this concrete file type. By convention, the ID is the fully
     * qualified class name of the factory.
     * @return The ID.
     */
    public final String getID() {
        return getClass().getName();
    }

    /** If this factory supports writing files (possible input type). If so,
     * the {@link #createNewWriteInstance()} must not contain <code>null</code>.
     * @return Whether this file type can write.
     */
    public abstract boolean canWrite();

    /** Create a new file type for writing.
     * @return A new instance.
     * @see #canWrite()
     */
    public abstract AbstractFileTypeWrite createNewWriteInstance();

    /** Create a new configuration object that represents the settings to
     * the write instance. The returned object must be of a class that is
     * accepted by the write instance's {@link
     * AbstractFileTypeWrite#writeTable(org.knime.core.data.DataTableSpec,
     * org.knime.core.data.RowIterator, int, java.io.OutputStream,
     * org.knime.core.node.ExecutionMonitor) writeTable method}, whereby the
     * write instance is created using the {@link #createNewWriteInstance()}
     * method.
     * @return A new configuration representing the writer settings.
     * @see #canWrite()
     * @see #createNewWriteInstance()
     */
    public abstract AbstractFileTypeWriteConfig createNewWriteConfig();

    /** If this factory supports reading files (possible output type). If so,
     * the {@link #createNewReadInstance()} must not contain <code>null</code>.
     * @return Whether this file type can read.
     */
    public abstract boolean canRead();

    /** Create a new file type for reading.
     * @return A new instance.
     * @see #canRead()
     */
    public abstract AbstractFileTypeRead createNewReadInstance();

    /** Create a new configuration object that represents the settings to
     * the read instance. The returned object must be of a class that is
     * accepted by the read instance's {@linkplain
     * AbstractFileTypeRead#readTable(
     * org.knime.exttool.executor.OutputDataHandle,
     * org.knime.core.node.ExecutionContext) readTable method},
     * whereby the read instance is created using the
     * {@link #createNewReadInstance()} method.
     * @return A new configuration representing the reader settings.
     * @see #canRead()
     * @see #createNewReadInstance()
     */
    public abstract AbstractFileTypeReadConfig createNewReadConfig();

    /** Get the file suffix (excluding the dot). For CSV types, this would
     * be <i>csv</i>.
     * @return The file suffix, appended to temp files. */
    public abstract String getSuffix();

    /** Get a user friendly name of this file type. For CSV types, this would
     * be <i>CSV</i>.
     * @return A user-friendly name (shown in the dialog).
     */
    public abstract String getUserFriendlyName();

    /** Whether this file type can write the argument column type.
     * @param spec The spec to check.
     * @return true when it can write the type.
     */
    public abstract boolean accepts(final DataColumnSpec spec);

}
