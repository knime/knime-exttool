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

    public static final Set<String> getWriteIDs() {
        ensureInitExtensions();
        return factoryMapWrite.keySet();
    }

    public static final Collection<AbstractFileTypeFactory>
        getWriteFactories() {
        ensureInitExtensions();
        return factoryMapWrite.values();
    }

    public static final Set<String> getReadIDs() {
        ensureInitExtensions();
        return factoryMapRead.keySet();
    }

    public static final Collection<AbstractFileTypeFactory> getReadFactories() {
        ensureInitExtensions();
        return factoryMapRead.values();
    }

    public final String getID() {
        return getClass().getName();
    }

    public abstract boolean canWrite();

    public abstract AbstractFileTypeWrite createNewWriteInstance();

    public abstract boolean canRead();

    public abstract AbstractFileTypeRead createNewReadInstance();

    public abstract String getSuffix();

    public abstract String getUserFriendlyName();

    public abstract boolean accepts(final DataColumnSpec spec);

}
