/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME GmbH, Konstanz, Germany
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
 * ------------------------------------------------------------------------
 *
 * History
 *   Jan 20, 2010 (wiswedel): created
 */
package org.knime.exttool.node;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;

/**
 * Default command line settings, representing a command line string.
 * @author Bernd Wiswedel, KNIME.com, Zurich, Switzerland
 */
public class FreeFormCommandlineSettings extends AbstractCommandlineSettings {

    private static final String CFG_COMMANDLINE = "command_text";

    private String m_commandline;

    /** {@inheritDoc} */
    @Override
    protected void loadSettingsInModel(final ExttoolSettings
            exttoolSettings, final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_commandline = settings.getString(CFG_COMMANDLINE);
        if (m_commandline == null || m_commandline.isEmpty()) {
            throw new InvalidSettingsException("No commandline specified");
        }
    }

    /** {@inheritDoc} */
    @Override
    protected void loadSettingsInDialog(final ExttoolSettings
            exttoolSettings, final NodeSettingsRO settings,
            final DataTableSpec[] inSpecs) throws NotConfigurableException {
        m_commandline = settings.getString(CFG_COMMANDLINE, "");
    }

    /** {@inheritDoc} */
    @Override
    protected void correctSettingsForSave(
            final ExttoolSettings exttoolSettings) {
        // nothing to do here
    }

    /** {@inheritDoc} */
    @Override
    protected void saveSettings(final NodeSettingsWO settings) {
        if (m_commandline != null) {
            settings.addString(CFG_COMMANDLINE, m_commandline);
        }
    }

    /** {@inheritDoc} */
    @Override
    protected FreeFormCommandlineControl createControl() {
        return new FreeFormCommandlineControl();
    }

    /**
     * @return the commandline
     */
    protected String getCommandline() {
        return m_commandline;
    }

    /**
     * @param commandline the commandline to set
     */
    protected void setCommandline(final String commandline) {
        m_commandline = commandline;
    }

    /** {@inheritDoc} */
    @Override
    protected String[] getCommandlineArgs(
            final ExttoolNodeEnvironment env) throws InvalidSettingsException {
        if (m_commandline == null) {
            return null;
        }
        // parse commandline, put space-separated tokens in individual array
        // elements, respect quotes and backslashes
        List<String> list = new ArrayList<String>();
        StringBuilder b = new StringBuilder();
        boolean inQuotes = false;
        boolean escaped = false;
        for (int i = 0; i < m_commandline.length(); i++) {
            char c = m_commandline.charAt(i);
            switch (c) {
            case '\\':
                if (escaped) { // backslash followed by backslash
                    b.append(c);
                }
                escaped = !escaped;
                break;
            case '$': // start of variable?
                if (escaped) {
                    b.append(c);
                    escaped = false;
                } else {
                    // variable is "$(name-of-var)"
                    int start = m_commandline.indexOf('(', i);
                    if (start != i + 1) {
                        throw new InvalidSettingsException(
                                "Invalid variable placeholder at position " + i
                                + "(near \""
                                + getSubStringAroundPosition(m_commandline, i)
                                + "\") -- no starting '('");
                    }
                    int end = m_commandline.indexOf(')', i);
                    if (end < 0) {
                        throw new InvalidSettingsException(
                                "Invalid variable placeholder at position " + i
                                + "(near \""
                                + getSubStringAroundPosition(m_commandline, i)
                                + "\") -- no closing ')'");
                    }
                    String varName = m_commandline.substring(start + 1, end);
                    b.append(readVariable(varName, env));
                    i = end;
                }
                break;
            case '\"':
                if (escaped) {
                    b.append(c);
                } else {
                    inQuotes = !inQuotes;
                }
                escaped = false;
                break;
            case ' ':
                if (escaped) {
                    onInvalidEscapePosition(i, c);
                }
                if (inQuotes) {
                    b.append(c);
                } else {
                    String arg = b.toString().trim();
                    b.setLength(0);
                    if (arg.length() > 0) { // ignore sequence of spaces
                        list.add(arg);
                    }
                }
                break;
            default:
                if (escaped) {
                    onInvalidEscapePosition(i, c);
                }
                b.append(c);
            }
        }
        if (inQuotes) {
            throw new InvalidSettingsException("Expression is not properly "
                    + "closed by quote (\") character");
        }
        String arg = b.toString().trim();
        b.setLength(0);
        if (arg.length() > 0) { // ignore sequence of spaces
            list.add(arg);
        }
        return list.toArray(new String[list.size()]);
    }

    private void onInvalidEscapePosition(final int position,
            final char character) throws InvalidSettingsException {
        throw new InvalidSettingsException("Unable to parse "
                + "commandline: character at position " + position
                + "(' " + character + "') does not need to be escaped, "
                + "use two backslashes (\\\\) to insert a single"
                + "backslash in the expression.");
    }

    private String getSubStringAroundPosition(final String str,
            final int pos) {
        int start = Math.max(0, pos - 10);
        int end = Math.min(str.length(), pos + 10);
        return str.substring(start, end);
    }

    private String readVariable(final String varName,
            final ExttoolNodeEnvironment env) throws InvalidSettingsException {
        try {
            return Double.toString(env.readFlowVariableDouble(varName));
        } catch (NoSuchElementException ne) {
            // ignore
        }
        try {
            return Integer.toString(env.readFlowVariableInt(varName));
        } catch (NoSuchElementException ne) {
            // ignore
        }
        try {
            return env.readFlowVariableString(varName);
        } catch (NoSuchElementException ne) {
            // ignore
        }
        throw new InvalidSettingsException("Unable to read variable \""
                + varName + "\"");
    }

}
