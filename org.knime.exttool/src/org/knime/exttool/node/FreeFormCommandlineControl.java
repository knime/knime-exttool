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
 *
 * History
 *   Jan 20, 2010 (wiswedel): created
 */
package org.knime.exttool.node;

import java.awt.GridBagConstraints;
import java.awt.Insets;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NotConfigurableException;

/** Controller to {@link FreeFormCommandlineSettings} with just a text area.
 * @author Bernd Wiswedel, KNIME AG, Zurich, Switzerland
 */
public class FreeFormCommandlineControl extends AbstractCommandlineControl {

    private JTextArea m_commandlineTextArea;

    /** {@inheritDoc} */
    @Override
    protected void registerPanel(final JPanel parent) {
        throw new IllegalStateException(
                "Not to be called as second method is overridden");
    }

    /** {@inheritDoc} */
    @Override
    protected void registerPanel(
            final JPanel parent, final GridBagConstraints gbc) {
        StringBuilder helpText = new StringBuilder("<html><body>");
        helpText.append("Enter command line here, use placeholder ");
        helpText.append("<i>%inFile%</i> and <i>%outFile%</i>");
        helpText.append(", which<br />");
        helpText.append("will be replaced by the full path to the in-");
        helpText.append("and output file upon execution</body></html>");
        JLabel helpLabel = new JLabel(helpText.toString());
        Insets oldInsets = gbc.insets;
        gbc.insets = new Insets(15, 5, 5, 5);
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        parent.add(helpLabel, gbc);

        gbc.gridwidth = 1;
        gbc.insets = oldInsets;
        JLabel descLabel = new JLabel("Command line");
        parent.add(descLabel, gbc);

        m_commandlineTextArea = new JTextArea(8, 20);
        gbc.gridx = GridBagConstraints.RELATIVE;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        parent.add(new JScrollPane(m_commandlineTextArea), gbc);
    }

    /** {@inheritDoc} */
    @Override
    protected void loadSettings(final AbstractCommandlineSettings settings,
            final DataTableSpec[] spec) throws NotConfigurableException {
        FreeFormCommandlineSettings set = (FreeFormCommandlineSettings)settings;
        String cmdLine = set.getCommandline();
        m_commandlineTextArea.setText(cmdLine == null ? "" : cmdLine);
    }

    /** {@inheritDoc} */
    @Override
    protected void saveSettings(final AbstractCommandlineSettings settings)
        throws InvalidSettingsException {
        FreeFormCommandlineSettings set = (FreeFormCommandlineSettings)settings;
        set.setCommandline(m_commandlineTextArea.getText());
    }

}
