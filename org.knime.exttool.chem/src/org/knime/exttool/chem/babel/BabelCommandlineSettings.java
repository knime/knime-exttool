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
 *   Mar 9, 2010 (wiswedel): created
 */
package org.knime.exttool.chem.babel;

import java.util.ArrayList;
import java.util.List;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.exttool.chem.filetype.mol2.Mol2FileTypeFactory;
import org.knime.exttool.chem.filetype.sdf.SdfFileTypeFactory;
import org.knime.exttool.filetype.AbstractFileTypeFactory;
import org.knime.exttool.node.base.AbstractCommandlineSettings;

/**
 * @author Bernd Wiswedel, KNIME.com, Zurich, Switzerland
 */
final class BabelCommandlineSettings
    extends AbstractCommandlineSettings {

    private String m_inputColumn;
    private String m_inputType;
    private String m_outputType;

    /** {@inheritDoc} */
    @Override
    protected String[] getCommandlineArgs() throws InvalidSettingsException {
        // check presence of selected types
        AbstractFileTypeFactory inFactory =
            AbstractFileTypeFactory.get(m_inputType);
        AbstractFileTypeFactory outFactory =
            AbstractFileTypeFactory.get(m_outputType);
        List<String> cmds = new ArrayList<String>();
        // e.g. "babel -i sdf %inFile% -o mol2 %outFile%"
        cmds.add("babel");
        cmds.add("-i");
        if (inFactory instanceof SdfFileTypeFactory) {
            cmds.add("sdf");
        } else if (inFactory instanceof Mol2FileTypeFactory) {
            cmds.add("mol2");
        } else {
            throw new InvalidSettingsException(
                    "Unsupported input type: " + m_inputType);
        }
        cmds.add("%inFile%");
        cmds.add("-o");
        if (outFactory instanceof SdfFileTypeFactory) {
            cmds.add("sdf");
        } else if (outFactory instanceof Mol2FileTypeFactory) {
            cmds.add("mol2");
        } else {
            throw new InvalidSettingsException(
                    "Unsupported output type: " + m_outputType);
        }
        cmds.add("%outFile%");
        return cmds.toArray(new String[cmds.size()]);
    }

    /** {@inheritDoc} */
    @Override
    protected void saveSettings(final NodeSettingsWO settings) {
        settings.addString("inputColumn", m_inputColumn);
        settings.addString("inputType", m_inputType);
        settings.addString("outputType", m_outputType);
    }

    /** {@inheritDoc} */
    @Override
    protected void loadSettingsInModel(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_inputColumn = settings.getString("inputColumn");
        if (m_inputColumn == null || m_inputColumn.length() == 0) {
            throw new InvalidSettingsException("Invalid column: "
                    + m_inputColumn);
        }
        String inTypeS = settings.getString("inputType");
        String outTypeS = settings.getString("outputType");
        AbstractFileTypeFactory.get(inTypeS); // validates
        AbstractFileTypeFactory.get(outTypeS);
        m_inputType = inTypeS;
        m_outputType = outTypeS;
    }

    /** {@inheritDoc} */
    @Override
    protected void loadSettingsInDialog(final NodeSettingsRO settings,
            final DataTableSpec[] inSpecs) throws NotConfigurableException {
        m_inputColumn = settings.getString("inputColumn", "");
        m_inputType = settings.getString("inputType",
                SdfFileTypeFactory.class.getName());
        m_outputType = settings.getString("outputType",
                SdfFileTypeFactory.class.getName());
    }

    /** {@inheritDoc} */
    @Override
    protected BabelCommandlineControl createControl() {
        return new BabelCommandlineControl();
    }

    /**
     * @return the inputColumn
     */
    String getInputColumn() {
        return m_inputColumn;
    }

    /**
     * @param inputColumn the inputColumn to set
     */
    void setInputColumn(final String inputColumn) {
        m_inputColumn = inputColumn;
    }

    /**
     * @return the inputType
     */
    String getInputType() {
        return m_inputType;
    }

    /**
     * @param inputType the inputType to set
     */
    void setInputType(final String inputType) {
        m_inputType = inputType;
    }

    /**
     * @return the outputType
     */
    String getOutputType() {
        return m_outputType;
    }

    /**
     * @param outputType the outputType to set
     */
    void setOutputType(final String outputType) {
        m_outputType = outputType;
    }

}
