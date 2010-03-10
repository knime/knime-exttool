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
 *   21.08.2008 (thor): created
 */
package org.knime.exttool.chem.filetype.sdf;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

/**
 * This class holds the settings for the SDF writer node.
 *
 * @author Thorsten Meinl, University of Konstanz
 */
public class SDFWriterSettings {
    /** Name of the row key column in the dialog. */
    static final String ROW_KEY_COL_NAME = "Row ID";

    /** Internally used row key identifier. */
    static final String ROW_KEY_IDENTIFIER = "$RowID$";

    private String m_fileName;

    private String m_structureColumn;

    private String m_titleColumn;

    private boolean m_overwriteOK;

    private final List<String> m_properties = new ArrayList<String>();

    private boolean m_includeAllColumns;

    /**
     * Returns the output file's name.
     *
     * @return the filename
     */
    public String fileName() {
        return m_fileName;
    }

    /**
     * Sets the output file's name.
     *
     * @param fileName the filename
     */
    public void fileName(final String fileName) {
        m_fileName = fileName;
    }

    /**
     * Returns the structure column's name.
     *
     * @return the structure column's name
     */
    public String structureColumn() {
        return m_structureColumn;
    }

    /**
     * Sets the structure column's name.
     *
     * @param col the structure column's name
     */
    public void structureColumn(final String col) {
        m_structureColumn = col;
    }

    /**
     * Returns the title column's name, if the title should be replaced.
     *
     * @return the structure column's name or <code>null</code> if the title
     *         should not be replaced
     */
    public String titleColumn() {
        return m_titleColumn;
    }

    /**
     * Sets the title column's name, if the title should be replaced.
     *
     * @param col the structure column's name or <code>null</code> if the title
     *            should not be replaced
     */
    public void titleColumn(final String col) {
        m_titleColumn = col;
    }

    /** @return current overwriteOK field. */
    public boolean overwriteOK() {
        return m_overwriteOK;
    }

    /** @param ok new value for overwriteOK property. */
    public void overwriteOK(final boolean ok) {
        m_overwriteOK = ok;
    }

    /**
     * Clears all properties.
     */
    public void clearProperties() {
        m_properties.clear();
    }

    /**
     * Adds properties that should be inserted into the output file.
     *
     * @param cols the columns' names that should be used as properties
     */
    public void addProperyColumns(final Collection<String> cols) {
        m_properties.addAll(cols);
    }

    /**
     * Returns an unmodifiable list of column names that should be used as
     * properties.
     *
     * @return a list of column names
     */
    public List<String> properties() {
        return Collections.unmodifiableList(m_properties);
    }

    /**
     * Sets if all (compatible) columns from the input table should be used as
     * properties.
     *
     * @param b <code>true</code> if all columns should be used,
     *            <code>false</code> if the {@link #properties()}-list should be
     *            used
     */
    public void includeAllColumns(final boolean b) {
        m_includeAllColumns = b;
    }

    /**
     * Returns if all (compatible) columns from the input table should be used
     * as properties.
     *
     * @return <code>true</code> if all columns should be used,
     *         <code>false</code> if the {@link #properties()}-list should be
     *         used
     */
    public boolean includeAllColumns() {
        return m_includeAllColumns;
    }

    /**
     * Saves all settings into the given node settings object.
     *
     * @param settings the node settings
     */
    public void saveSettings(final NodeSettingsWO settings) {
        settings.addString("filename", m_fileName);
        settings.addString("titleColumn", m_titleColumn);
        settings.addString("structureColumn", m_structureColumn);
        settings.addBoolean("overwriteOK", m_overwriteOK);

        settings.addStringArray("properties", m_properties
                .toArray(new String[m_properties.size()]));
        settings.addBoolean("includeAllColumns", m_includeAllColumns);
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
        m_titleColumn = settings.getString("titleColumn");
        m_structureColumn = settings.getString("structureColumn");
        // added in v2.1
        m_overwriteOK = settings.getBoolean("overwriteOK", true);

        m_properties.clear();
        for (String s : settings.getStringArray("properties")) {
            m_properties.add(s);
        }
        // added in v2.1
        m_includeAllColumns = settings.getBoolean("includeAllColumns", false);
    }

    /**
     * Loads all settings from the given node settings object, using default
     * values if a setting is missing.
     *
     * @param settings the node settings
     */
    public void loadSettingsForDialog(final NodeSettingsRO settings) {
        m_fileName = settings.getString("filename", "");
        m_titleColumn = settings.getString("titleColumn", null);
        m_structureColumn = settings.getString("structureColumn", null);
        // added in v2.1
        m_overwriteOK = settings.getBoolean("overwriteOK", false);

        m_properties.clear();
        for (String s : settings.getStringArray("properties", new String[0])) {
            m_properties.add(s);
        }
        m_includeAllColumns = settings.getBoolean("includeAllColumns", false);
    }
}
