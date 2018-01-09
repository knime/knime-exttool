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
 *   Mar 2, 2010 (wiswedel): created
 */
package org.knime.exttool.executor;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.knime.core.node.NodeLogger;

/** Output handle of the external tool. It represents the data that is written
 * by external process and which is then read by KNIME. The
 * {@link DataHandle#getLocation() location} returned by objects of this
 * interface replaces the stub %outFile% in the commandline.
 *
 * <p><b>Warning:</b> API needs review, subclassing outside this package
 * is currently not encouraged.
 * @author Bernd Wiswedel, KNIME AG, Zurich, Switzerland
 */
public interface OutputDataHandle extends DataHandle {

    /** Open a new stream, from which the framework reads the results.
     * @return A new output stream, which is used by the framework to write
     *         the data.
     * @throws IOException In case of I/O problems.
     */
    public abstract InputStream openOutputFileInStream() throws IOException;

    /** Default implementation using local files. */
    public static class FileOutputDataHandle implements OutputDataHandle {

        private final File m_outFile;

        /** New output handle for a given file.
         * @param outFile The output file, must not be null. */
        public FileOutputDataHandle(final File outFile) {
            if (outFile == null) {
                throw new NullPointerException("Argument must not be null.");
            }
            m_outFile = outFile;
        }

        /** @return the outFile as passed in constructor. */
        public File getOutFile() {
            return m_outFile;
        }

        /** {@inheritDoc} */
        @Override
        public void cleanUp() {
            if (m_outFile.exists() && !m_outFile.delete()) {
                NodeLogger.getLogger(getClass()).warn("Could not delete file \""
                        + m_outFile.getAbsolutePath() + "\"");
            }
        }

        /** {@inheritDoc} */
        @Override
        public String getLocation() {
            return m_outFile.getAbsolutePath();
        }

        /** {@inheritDoc} */
        @Override
        public InputStream openOutputFileInStream() throws IOException {
            return new BufferedInputStream(new FileInputStream(m_outFile));
        }
    }


}
