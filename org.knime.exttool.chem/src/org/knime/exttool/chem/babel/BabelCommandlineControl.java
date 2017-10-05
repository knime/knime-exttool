/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME GmbH, Konstanz, Germany
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
 *   Mar 10, 2010 (wiswedel): created
 */
package org.knime.exttool.chem.babel;

import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.Enumeration;

import javax.swing.AbstractButton;
import javax.swing.ButtonGroup;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.border.Border;

import org.knime.chem.types.Mol2Value;
import org.knime.chem.types.SdfValue;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.util.ColumnSelectionComboxBox;
import org.knime.exttool.chem.filetype.mol2.Mol2FileTypeFactory;
import org.knime.exttool.chem.filetype.sdf.SdfFileTypeFactory;
import org.knime.exttool.filetype.AbstractFileTypeFactory;
import org.knime.exttool.node.AbstractCommandlineControl;
import org.knime.exttool.node.AbstractCommandlineSettings;

/** GUI control elements for the Babel control panel. It has a column selection
 * combo box (choosing Mol2 and Sdf type columns) and button group, in which
 * the user can choose the output format.
 * @author Bernd Wiswedel, KNIME.com, Zurich, Switzerland
 */
final class BabelCommandlineControl extends AbstractCommandlineControl {

    private ColumnSelectionComboxBox m_colSelCombo;
    private ButtonGroup m_outputButtonGroup;

    /** {@inheritDoc} */
    @Override
    protected void loadSettings(final AbstractCommandlineSettings settings,
            final DataTableSpec[] inSpecs) throws NotConfigurableException {
        BabelCommandlineSettings babelSet = (BabelCommandlineSettings)settings;
        DataTableSpec spec = inSpecs[0];
        boolean containsSDF = spec.containsCompatibleType(SdfValue.class);
        boolean containsMol2 = spec.containsCompatibleType(Mol2Value.class);
        if (!(containsSDF || containsMol2)) {
            throw new NotConfigurableException(
                    "Input contains no appropriate type");
        }
        m_colSelCombo.update(spec, babelSet.getInputColumn());
        AbstractFileTypeFactory outputType = babelSet.getOutputFileType();
        String outputTypeName = outputType == null ? null
                : outputType.getClass().getName();
        for (Enumeration<AbstractButton> enu
                = m_outputButtonGroup.getElements(); enu.hasMoreElements();) {
            AbstractButton b = enu.nextElement();
            if (b.getActionCommand().equals(outputTypeName)) {
                b.doClick();
            }
        }
        // can ignore input type here, it's magically determined based upon
        // the selected input column (no need to match the settings as we
        // are in the dialog, not the model).
    }

    /** {@inheritDoc} */
    @Override
    protected void saveSettings(final AbstractCommandlineSettings settings)
            throws InvalidSettingsException {
        BabelCommandlineSettings babelSet = (BabelCommandlineSettings)settings;
        babelSet.setInputColumn(m_colSelCombo.getSelectedColumn());
        babelSet.setInputFileType(getInputFileType());
        babelSet.setOutputType(getOutputFileType());
    }

    private AbstractFileTypeFactory getInputFileType()
        throws InvalidSettingsException {
        DataColumnSpec selCol = (DataColumnSpec)m_colSelCombo.getSelectedItem();
        if (selCol.getType().isCompatible(SdfValue.class)) {
            return AbstractFileTypeFactory.get(
                    SdfFileTypeFactory.class.getName());
        } else if (selCol.getType().isCompatible(Mol2Value.class)) {
            return AbstractFileTypeFactory.get(
                    Mol2FileTypeFactory.class.getName());
        } else {
            // can't happen, combo box filters appropriate types
            throw new InvalidSettingsException(
                    "Invalid input type selected: " + selCol);
        }
    }

    private AbstractFileTypeFactory getOutputFileType()
        throws InvalidSettingsException {
        return AbstractFileTypeFactory.get(
                m_outputButtonGroup.getSelection().getActionCommand());
    }

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    @Override
    protected void registerPanel(final JPanel parent) {
        m_colSelCombo = new ColumnSelectionComboxBox((Border)null,
                Mol2Value.class, SdfValue.class);
        m_outputButtonGroup = new ButtonGroup();
        JPanel panel = new JPanel(new GridLayout(0, 2));
        panel.add(inFlowLayout(new JLabel("Input column: ")));
        panel.add(inFlowLayout(m_colSelCombo));

        panel.add(inFlowLayout(new JLabel("Output type: ")));
        JRadioButton sdfButton = new JRadioButton("SDF");
        sdfButton.setActionCommand(SdfFileTypeFactory.class.getName());
        JRadioButton mol2Button = new JRadioButton("Mol2");
        mol2Button.setActionCommand(Mol2FileTypeFactory.class.getName());
        m_outputButtonGroup.add(sdfButton);
        m_outputButtonGroup.add(mol2Button);
        sdfButton.doClick();
        panel.add(inFlowLayout(sdfButton, mol2Button));
        parent.add(panel);
    }

    private static JPanel inFlowLayout(final JComponent... comps) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT));
        for (JComponent c : comps) {
            p.add(c);
        }
        return p;
    }

}
