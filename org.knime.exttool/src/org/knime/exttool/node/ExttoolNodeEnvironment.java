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
 */
package org.knime.exttool.node;

import java.util.NoSuchElementException;

/** Represents properties that are available at node execution time. This
 * includes so far only getters for the current flow variables. Future versions
 * may provide more (e.g. credentials).
 *
 * @author Bernd Wiswedel, KNIME.com, Zurich, Switzerland
 */
public class ExttoolNodeEnvironment {

    private final ExttoolNodeModel m_model;

    /** Creates new environment, which reads the property from the argument
     * model.
     * @param model The node model of the execution.
     */
    ExttoolNodeEnvironment(final ExttoolNodeModel model) {
        m_model = model;
    }

    /** Read string flow variable in current execution.
     * @param name Name of variable.
     * @return Value of variable
     * @throws NoSuchElementException If variable is not available
     */
    public String readFlowVariableString(final String name) {
        return m_model.readFlowVariableString(name);
    }

    /** Read double flow variable in current execution.
     * @param name Name of variable.
     * @return Value of variable
     * @throws NoSuchElementException If variable is not available
     */
    public double readFlowVariableDouble(final String name) {
        return m_model.readFlowVariableDouble(name);
    }

    /** Read int flow variable in current execution.
     * @param name Name of variable.
     * @return Value of variable
     * @throws NoSuchElementException If variable is not available
     */
    public int readFlowVariableInt(final String name) {
        return m_model.readFlowVariableInt(name);
    }

}
