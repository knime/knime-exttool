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
 * ------------------------------------------------------------------------
 *
 * History
 *   Mar 9, 2010 (wiswedel): created
 */
package org.knime.exttool.chem.babel;

import java.util.ArrayList;
import java.util.List;

import org.knime.chem.base.node.io.sdf.DefaultSDFReader;
import org.knime.chem.types.Mol2Value;
import org.knime.chem.types.SdfCell;
import org.knime.chem.types.SdfValue;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.exttool.chem.filetype.mol2.Mol2FileTypeFactory;
import org.knime.exttool.chem.filetype.mol2.Mol2Reader;
import org.knime.exttool.chem.filetype.sdf.SdfFileTypeFactory;
import org.knime.exttool.filetype.AbstractFileTypeFactory;
import org.knime.exttool.filetype.AbstractFileTypeWriteConfig;
import org.knime.exttool.filetype.DefaultFileTypeReadConfig;
import org.knime.exttool.filetype.DefaultFileTypeWriteConfig;
import org.knime.exttool.node.AbstractCommandlineSettings;
import org.knime.exttool.node.ExttoolNodeEnvironment;
import org.knime.exttool.node.ExttoolSettings;
import org.knime.exttool.node.ExttoolSettings.PathAndTypeConfigurationInput;
import org.knime.exttool.node.ExttoolSettings.PathAndTypeConfigurationOutput;

/** A command line setting representing the configuration of the babel node.
 *
 * @author Bernd Wiswedel, KNIME AG, Zurich, Switzerland
 */
final class BabelCommandlineSettings
    extends AbstractCommandlineSettings {

    private String m_inputColumn;
    private AbstractFileTypeFactory m_inputFileType;
    private AbstractFileTypeFactory m_outputFileType;

    /** {@inheritDoc} */
    @Override
    protected String[] getCommandlineArgs(
            final ExttoolNodeEnvironment env) throws InvalidSettingsException {
        if (m_inputFileType == null || m_outputFileType == null) {
            throw new InvalidSettingsException("No input/output type selected");
        }
        // check presence of selected types
        List<String> cmds = new ArrayList<String>();
        // e.g. "babel -i sdf %inFile% -o mol2 %outFile%"
        cmds.add("babel");
        cmds.add("-i");
        if (m_inputFileType instanceof SdfFileTypeFactory) {
            cmds.add("sdf");
        } else if (m_inputFileType instanceof Mol2FileTypeFactory) {
            cmds.add("mol2");
        } else {
            throw new InvalidSettingsException(
                    "Unsupported input type: " + m_inputFileType);
        }
        cmds.add("%inFile%");
        cmds.add("-o");
        if (m_outputFileType instanceof SdfFileTypeFactory) {
            cmds.add("sdf");
        } else if (m_outputFileType instanceof Mol2FileTypeFactory) {
            cmds.add("mol2");
        } else {
            throw new InvalidSettingsException(
                    "Unsupported output type: " + m_outputFileType);
        }
        cmds.add("%outFile%");
        return cmds.toArray(new String[cmds.size()]);
    }

    /** {@inheritDoc} */
    @Override
    protected void correctSettingsForSave(
            final ExttoolSettings exttoolSettings) {
        PathAndTypeConfigurationInput in = new PathAndTypeConfigurationInput();
        in.setPath(null); // auto-generated temp
        in.setType(m_inputFileType);
        in.setWriteConfig(createWriteConfig());
        exttoolSettings.setInputConfig(0, in);

        PathAndTypeConfigurationOutput out =
            new PathAndTypeConfigurationOutput();
        out.setPath(null);
        out.setType(m_outputFileType);
        out.setReadConfig(createReadConfig());
        exttoolSettings.setOutputConfig(0, out);
    }

    /** {@inheritDoc} */
    @Override
    protected void saveSettings(final NodeSettingsWO settings) {
        // no individual config here.
    }

    /** {@inheritDoc} */
    @Override
    protected void loadSettingsInModel(final ExttoolSettings
            exttoolSettings, final NodeSettingsRO settings)
            throws InvalidSettingsException {

        PathAndTypeConfigurationInput in = exttoolSettings.getInputConfig(0);
        AbstractFileTypeFactory inType = in.getType();
        setInputFileType(inType); // validates class

        AbstractFileTypeWriteConfig writeConfig = in.getWriteConfig();
        String column;
        // supported types all return the default implementation
        if (writeConfig instanceof DefaultFileTypeWriteConfig) {
            column = ((DefaultFileTypeWriteConfig)writeConfig).getColumn();
        } else {
            throw new InvalidSettingsException("Type " + inType + " does not"
                    + " return expected config implementation");
        }
        if (column == null || column.length() == 0) {
            throw new InvalidSettingsException("Invalid column: " + column);
        }

        PathAndTypeConfigurationOutput out = exttoolSettings.getOutputConfig(0);
        AbstractFileTypeFactory outType = out.getType();
        setOutputType(outType); // validates class
        // config is ignored here -- nothing to set up
    }

    /** {@inheritDoc} */
    @Override
    protected void loadSettingsInDialog(final ExttoolSettings
            exttoolSettings, final NodeSettingsRO settings,
            final DataTableSpec[] inSpecs) throws NotConfigurableException {
        PathAndTypeConfigurationInput in = exttoolSettings.getInputConfig(0);
        // type can be ignored -- automagically determined based on actual
        // column type
        AbstractFileTypeWriteConfig writeConfig = in.getWriteConfig();

        String column = null;
        // supported types all return the default implementation
        if (writeConfig instanceof DefaultFileTypeWriteConfig) {
            column = ((DefaultFileTypeWriteConfig)writeConfig).getColumn();
        }

        DataColumnSpec defColumn = null;
        for (DataColumnSpec cs : inSpecs[0]) {
            DataType type = cs.getType();
            if (type.isCompatible(Mol2Value.class)
                    || type.isCompatible(SdfValue.class)) {
                if (defColumn == null) {
                    defColumn = cs;
                } else if (defColumn.getName().equals(column)) {
                    defColumn = cs;
                } else {
                    // ignore, we have a reasonable default
                }
            }
        }
        if (defColumn == null) {
            throw new NotConfigurableException(
                    "No valid input column in table");
        }
        m_inputColumn = defColumn.getName();

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
    AbstractFileTypeFactory getInputFileType() {
        return m_inputFileType;
    }

    /** @param inputFileType the inputType to set
     * @throws InvalidSettingsException If the argument is invalid
     */
    void setInputFileType(final AbstractFileTypeFactory inputFileType)
        throws InvalidSettingsException {
        if (inputFileType instanceof SdfFileTypeFactory) {
            // accept
        } else if (inputFileType instanceof Mol2FileTypeFactory) {
            // accept
        } else {
            throw new InvalidSettingsException("Unsupported input type: "
                    + inputFileType);
        }
        m_inputFileType = inputFileType;
    }

    /**
     * @return the outputType
     */
    AbstractFileTypeFactory getOutputFileType() {
        return m_outputFileType;
    }

    /**
     * @param outputFileType the outputType to set
     * @throws InvalidSettingsException If argument type is unsupported
     */
    void setOutputType(final AbstractFileTypeFactory outputFileType)
        throws InvalidSettingsException {
        if (outputFileType instanceof SdfFileTypeFactory) {
            // accept
        } else if (outputFileType instanceof Mol2FileTypeFactory) {
            // accept
        } else {
            throw new InvalidSettingsException("Unsupported output type: "
                    + outputFileType);
        }
        m_outputFileType = outputFileType;
    }

    DefaultFileTypeWriteConfig createWriteConfig() {
        DefaultFileTypeWriteConfig writeConfig;
        if (m_inputFileType instanceof SdfFileTypeFactory) {
           writeConfig =
               ((SdfFileTypeFactory)m_inputFileType).createNewWriteConfig();
        } else if (m_inputFileType instanceof Mol2FileTypeFactory) {
            writeConfig =
                ((Mol2FileTypeFactory)m_inputFileType).createNewWriteConfig();
        } else {
            throw new IllegalStateException("Unsupported input type: "
                    + m_inputFileType);
        }
        writeConfig.setColumn(m_inputColumn);
        return writeConfig;
    }

    DefaultFileTypeReadConfig createReadConfig() {
        DefaultFileTypeReadConfig readConfig;
        if (m_outputFileType instanceof SdfFileTypeFactory) {
            readConfig =
                ((SdfFileTypeFactory)m_outputFileType).createNewReadConfig();
        } else if (m_outputFileType instanceof Mol2FileTypeFactory) {
            readConfig =
                ((Mol2FileTypeFactory)m_outputFileType).createNewReadConfig();
        } else {
            throw new IllegalStateException("Unsupported output type: "
                    + m_inputFileType);
        }
        return readConfig;
    }

    /** {@inheritDoc} */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] specs)
            throws InvalidSettingsException {
        DataColumnSpec idColSpec;
        DataColumnSpec molColSpec;
        if (m_outputFileType instanceof SdfFileTypeFactory) {
            idColSpec = new DataColumnSpecCreator(
                DefaultSDFReader.MOLECULE_NAME_COLUMN, StringCell.TYPE).createSpec();
            molColSpec = new DataColumnSpecCreator(
                DefaultSDFReader.MOLECULE_COLUMN, SdfCell.TYPE).createSpec();
        } else if (m_outputFileType instanceof Mol2FileTypeFactory) {
            idColSpec = Mol2Reader.MOLECULE_COLNAME_SPEC;
            molColSpec = Mol2Reader.MOLECULE_COL_SPEC;
        } else {
            throw new IllegalStateException("Unsupported output type: "
                    + m_inputFileType);
        }
        return new DataTableSpec[] {new DataTableSpec(idColSpec, molColSpec)};
    }

}
