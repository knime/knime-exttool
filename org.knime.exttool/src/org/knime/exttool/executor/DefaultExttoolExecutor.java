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
 *   Feb 3, 2010 (wiswedel): created
 */
package org.knime.exttool.executor;

import java.io.File;
import java.util.Observable;
import java.util.Observer;

import org.knime.base.node.util.exttool.CommandExecution;
import org.knime.base.node.util.exttool.ViewUpdateNotice;
import org.knime.core.node.ExecutionMonitor;
import org.knime.exttool.executor.InputDataHandle.FileInputDataHandle;
import org.knime.exttool.executor.OutputDataHandle.FileOutputDataHandle;

/**
 * Default (local) execution by means of temporary files.
 * @author Bernd Wiswedel, KNIME AG, Zurich, Switzerland
 */
public class DefaultExttoolExecutor extends AbstractExttoolExecutor {

    /** {@inheritDoc} */
    @Override
    public int execute(final ExecutionMonitor monitor) throws Exception {
        String[] cmds = getCommandlineArgs();
        final Observer commandObserver = new Observer() {
            /** {@inheritDoc} */
            @Override
            public void update(final Observable o, final Object arg) {
                if (arg instanceof ViewUpdateNotice) {
                    DefaultExttoolExecutor.this.setChanged();
                    DefaultExttoolExecutor.this.notifyObservers(arg);
                }
            }
        };
        CommandExecution commandExecution = new CommandExecution(cmds);
        commandExecution.addObserver(commandObserver);
        // find first in/output file and set working dir to parent of that file
        File workingDir = null;
        InputDataHandle[] inputs = getInputHandles();
        for (InputDataHandle inPath : inputs) {
            FileInputDataHandle inFilePath = (FileInputDataHandle)inPath;
            if (workingDir != null) {
                break;
            }
            File temp = inFilePath.getInFile();
            workingDir = temp.getParentFile();
        }
        OutputDataHandle[] outputs = getOutputHandles();
        for (OutputDataHandle outPath : outputs) {
            FileOutputDataHandle outFilePath = (FileOutputDataHandle)outPath;
            if (workingDir != null) {
                break;
            }
            File temp = outFilePath.getOutFile();
            workingDir = temp.getParentFile();
        }
        if (workingDir != null) {
            commandExecution.setExecutionDir(workingDir);
        }
        return commandExecution.execute(monitor);
    }

}
