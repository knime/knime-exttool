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
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ---------------------------------------------------------------------
 *
 * History
 *   19.08.2008 (thor): created
 */
package org.knime.exttool.chem.filetype.sdf;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.config.Config;

/**
 * This class stores the settings the user can set in the dialog.
 *
 * @author Thorsten Meinl, University of Konstanz
 */
public class SDFReaderSettings {
    /**
     * Struct for one property inside an SDF molecule.
     *
     * @author Thorsten Meinl, University of Konstanz
     */
    public static class Property {
        /**
         * <code>true</code> if this property should be extracted,
         * <code>false</code> otherwise.
         */
        public boolean extract;

        /** The property's name. */
        public final String name;

        /**
         * The property's type, either {@link Integer}, {@link Double} or
         * {@link String} as last resort.
         */
        public Class<?> type;

        /**
         * Creates a new property.
         *
         * @param extract <code>true</code> if this property should be
         *            extracted, <code>false</code> otherwise
         * @param name the property's name
         * @param type the property's type, either {@link Integer},
         *            {@link Double} or {@link String} as last resort
         */
        public Property(final boolean extract, final String name,
                final Class<?> type) {
            this.extract = extract;
            this.name = name;
            this.type = type;
        }

        /**
         * Creates a new property as a copy of the given property.
         *
         * @param copy the property that should be copied
         */
        public Property(final Property copy) {
            this.extract = copy.extract;
            this.name = copy.name;
            this.type = copy.type;
        }
    }

    private boolean m_useRowID = true;

    private boolean m_extractName;

    private boolean m_extractSDF = true;

    private boolean m_extractMol;

    private boolean m_extractCTab;

    private boolean m_extractCounts;

    private String m_fileName;

    private List<Property> m_properties = new ArrayList<Property>();

    /**
     * Returns the file's name (may also be an URL).
     *
     * @return the file's name
     */
    public String fileName() {
        return m_fileName;
    }

    /**
     * Sets the file's name (may also be an URL).
     *
     * @param name the file's name
     */
    public void fileName(final String name) {
        m_fileName = name;
    }

    /**
     * Returns if the molecules' names should be used as row keys.
     *
     * @return <code>true</code> if the names should be taken as row keys,
     *         <code>false</code> if row keys should be generated
     */
    public boolean useRowID() {
        return m_useRowID;
    }

    /**
     * Sets if the molecules' names should be used as row keys.
     *
     * @param b <code>true</code> if the names should be taken as row keys,
     *            <code>false</code> if row keys should be generated
     */
    public void useRowID(final boolean b) {
        m_useRowID = b;
    }

    /**
     * Returns if the molecules' names should be put into a column.
     *
     * @return <code>true</code> if the names should be put into a column,
     *         <code>false</code> if row keys should be generated
     */
    public boolean extractName() {
        return m_extractName;
    }

    /**
     * Sets if the molecule's names should be put into a column.
     *
     * @param b <code>true</code> if the names should be put into a column,
     *            <code>false</code> if row keys should be generated
     */
    public void extractName(final boolean b) {
        m_extractName = b;
    }

    /**
     * Returns if complete SDF molecules should be extracted into a column.
     *
     * @return <code>true</code> if a column containing the complete SDF
     *         structured should be created, <code>false</code> otherwise
     */
    public boolean extractSDF() {
        return m_extractSDF;
    }

    /**
     * Sets if complete SDF molecules should be extracted into a column.
     *
     * @param b <code>true</code> if a column containing the complete SDF
     *            structured should be created, <code>false</code> otherwise
     */
    public void extractSDF(final boolean b) {
        m_extractSDF = b;
    }

    /**
     * Returns if the molecules' Molfile blocks should be extracted into a
     * column.
     *
     * @return <code>true</code> if a column containing the Molfile blocks
     *         should be created, <code>false</code> otherwise
     */
    public boolean extractMol() {
        return m_extractMol;
    }

    /**
     * Sets if the molecules' Molfile blocks should be extracted into a column.
     *
     * @param b <code>true</code> if a column containing the Molfile blocks
     *            should be created, <code>false</code> otherwise
     */
    public void extractMol(final boolean b) {
        m_extractMol = b;
    }

    /**
     * Returns if the molecules' Ctab blocks should be extracted into a column.
     *
     * @return <code>true</code> if a column containing the Ctab blocks should
     *         be created, <code>false</code> otherwise
     */
    public boolean extractCtab() {
        return m_extractCTab;
    }

    /**
     * Sets if the molecules' Ctab blocks should be extracted into a column.
     *
     * @param b <code>true</code> if a column containing the Ctab blocks
     *            should be created, <code>false</code> otherwise
     */
    public void extractCTab(final boolean b) {
        m_extractCTab = b;
    }

    /**
     * Returns if the molecules' atom and bond counts be extracted into a
     * column.
     *
     * @return <code>true</code> if a column containing the counts should be
     *         created, <code>false</code> otherwise
     */
    public boolean extractCounts() {
        return m_extractCounts;
    }

    /**
     * Sets if the molecules' atom and bond counts be extracted into a
     * column.
     *
     * @param b <code>true</code> if a column containing the counts should be
     *         created, <code>false</code> otherwise
     */
    public void extractCounts(final boolean b) {
        m_extractCounts = b;
    }

    /**
     * Clears all properties.
     */
    public void clearProperties() {
        m_properties.clear();
    }

    /**
     * Adds a new property.
     *
     * @param prop a property
     */
    public void addProperty(final Property prop) {
        m_properties.add(prop);
    }

    /**
     * Returns the collection of all properties.
     *
     * @return an unmodifiable collection
     */
    public Collection<Property> properties() {
        return Collections.unmodifiableCollection(m_properties);
    }

    /**
     * Saves all settings into the given node settings object.
     *
     * @param settings the node settings
     */
    public void saveSettings(final NodeSettingsWO settings) {
        settings.addString("filename", m_fileName);
        settings.addBoolean("useRowID", m_useRowID);
        settings.addBoolean("extractName", m_extractName);
        settings.addBoolean("extractSDF", m_extractSDF);
        settings.addBoolean("extractMol", m_extractMol);
        settings.addBoolean("extractCTab", m_extractCTab);
        settings.addBoolean("extractCounts", m_extractCounts);

        Config props = settings.addConfig("properties");

        props.addInt("count", m_properties.size());
        for (int i = 0; i < m_properties.size(); i++) {
            props.addBoolean("extract_" + i, m_properties.get(i).extract);
            props.addString("name_" + i, m_properties.get(i).name);
            props.addString("type_" + i, m_properties.get(i).type.getName());
        }
    }

    /**
     * Loads all settings from the given node settings object.
     *
     * @param settings the node settings
     * @throws InvalidSettingsException if a setting is missing
     */
    public void loadSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_fileName = settings.getString("filename");
        m_useRowID = settings.getBoolean("useRowID");
        m_extractName = settings.getBoolean("extractName", false);
        m_extractSDF = settings.getBoolean("extractSDF");
        m_extractMol = settings.getBoolean("extractMol");
        m_extractCTab = settings.getBoolean("extractCTab");
        m_extractCounts = settings.getBoolean("extractCounts");

        m_properties.clear();
        Config props = settings.getConfig("properties");
        int count = props.getInt("count");

        for (int i = 0; i < count; i++) {
            try {
                m_properties.add(new Property(props.getBoolean("extract_" + i),
                        props.getString("name_" + i), Class.forName(props
                                .getString("type_" + i))));
            } catch (ClassNotFoundException ex) {
                throw new InvalidSettingsException(ex);
            }
        }
    }

    /**
     * Loads all settings from the given node settings object, using default
     * values if a setting is missing.
     *
     * @param settings the node settings
     */
    public void loadSettingsForDialog(final NodeSettingsRO settings) {
        m_fileName = settings.getString("filename", null);
        m_useRowID = settings.getBoolean("useRowID", true);
        m_extractName = settings.getBoolean("extractName", false);
        m_extractSDF = settings.getBoolean("extractSDF", true);
        m_extractMol = settings.getBoolean("extractMol", false);
        m_extractCTab = settings.getBoolean("extractCTab", false);
        m_extractCounts = settings.getBoolean("extractCounts", false);
        m_properties.clear();

        m_properties.clear();

        try {
            Config props = settings.getConfig("properties");

            int count = props.getInt("count");

            for (int i = 0; i < count; i++) {
                m_properties.add(new Property(props.getBoolean("extract_" + i),
                        props.getString("name_" + i), Class.forName(props
                                .getString("type_" + i))));
            }
        } catch (Exception ex) {
            // just ignore it
        }
    }
}
