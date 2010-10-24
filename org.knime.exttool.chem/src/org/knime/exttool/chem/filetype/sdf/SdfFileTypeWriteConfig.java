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
 *   Aug 10, 2010 (wiswedel): created
 */
package org.knime.exttool.chem.filetype.sdf;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.knime.chem.types.SdfValue;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.util.DataValueColumnFilter;
import org.knime.exttool.filetype.DefaultFileTypeWriteConfig;

/** SDF file write config. Contains settings for target column and included
 * properties. Only the target column can currently be set in the dialog.
 *
 * @author Bernd Wiswedel, KNIME.com, Zurich, Switzerland
 */
public class SdfFileTypeWriteConfig extends DefaultFileTypeWriteConfig {

    private List<String> m_propertiesColumns;

    /**
     */
    @SuppressWarnings("unchecked")
    public SdfFileTypeWriteConfig() {
        super(new DataValueColumnFilter(SdfValue.class));
        m_propertiesColumns = Collections.emptyList();
    }

    /** @return the propertiesColumns */
    public List<String> getPropertiesColumns() {
        return m_propertiesColumns;
    }

    /**
     * @param propertiesColumns the propertiesColumns to set
     */
    public void setPropertiesColumns(final List<String> propertiesColumns) {
        if (propertiesColumns == null) {
            throw new NullPointerException("Argument must not be null.");
        }
        m_propertiesColumns = propertiesColumns;
    }

    /** {@inheritDoc} */
    @Override
    public void loadSettingsInDialog(
            final NodeSettingsRO settings, final DataTableSpec spec)
            throws NotConfigurableException {
        super.loadSettingsInDialog(settings, spec);
        String[] props = settings.getStringArray("properties", new String[0]);
        if (props != null) {
            m_propertiesColumns = Arrays.asList(props);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void loadSettingsInModel(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        super.loadSettingsInModel(settings);
        // field added in later version, do not throw exception
        String[] props = settings.getStringArray("properties", new String[0]);
        if (props != null) {
            m_propertiesColumns = Arrays.asList(props);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void saveSettings(final NodeSettingsWO settings) {
        super.saveSettings(settings);
        settings.addStringArray("properties", m_propertiesColumns.toArray(
                new String[m_propertiesColumns.size()]));
    }

}
