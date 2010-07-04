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
 *   Mar 8, 2010 (wiswedel): created
 */
package org.knime.exttool.filetype.csv;

import java.io.File;

import org.knime.base.node.io.filereader.FileAnalyzer;
import org.knime.base.node.io.filereader.FileReaderNodeSettings;
import org.knime.base.node.io.filereader.FileTable;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.util.tokenizer.SettingsStatus;
import org.knime.exttool.executor.OutputDataHandle;
import org.knime.exttool.filetype.AbstractFileTypeRead;
import org.knime.exttool.filetype.AbstractFileTypeReadConfig;

/** Read support for CSV.
 * @author Bernd Wiswedel, KNIME.com, Zurich, Switzerland
 */
class CSVFileTypeRead extends AbstractFileTypeRead {

    private CSVFileTypeReadConfig m_csvReadConfig;

    /** Create instance, associating it with its factory.
     * @param factory Factory that creates this instance.
     */
    public CSVFileTypeRead(final CSVFileTypeFactory factory) {
        super(factory);
    }

    /** {@inheritDoc} */
    @Override
    public void prepare(final AbstractFileTypeReadConfig config) {
        m_csvReadConfig = (CSVFileTypeReadConfig)config;
    }

    /** {@inheritDoc} */
    @Override
    public BufferedDataTable readTable(final OutputDataHandle in,
            final ExecutionContext exec) throws Exception {

        File input;
        if (in instanceof OutputDataHandle.FileOutputDataHandle) {
            input = ((OutputDataHandle.FileOutputDataHandle)in).getOutFile();
        } else {
            String error = "CSV output file parsing currently only "
                + "supports local files, expected "
                + OutputDataHandle.FileOutputDataHandle.class.getSimpleName()
                + "; got " + in.getClass().getSimpleName();
            throw new Exception(error);
        }

        // prepare the settings for the file analyzer
        FileReaderNodeSettings settings = new FileReaderNodeSettings();
        settings.addDelimiterPattern(m_csvReadConfig.getColDelimiter(),
                false, false, false);
        settings.setDelimiterUserSet(true);

        settings.setDataFileLocationAndUpdateTableName(input.toURI().toURL());
        settings.setTableName("exttool input");

        settings.addRowDelimiter(m_csvReadConfig.getRowDelimiter(), true);

        String quoteChar = m_csvReadConfig.getQuoteChar();
        if (quoteChar != null && quoteChar.length() > 0) {
            settings.addQuotePattern(quoteChar, quoteChar);
        }
        settings.setQuoteUserSet(true);

        settings.setFileHasColumnHeaders(m_csvReadConfig.hasColHeader());
        settings.setFileHasColumnHeadersUserSet(true);

        settings.setFileHasRowHeaders(false);
        settings.setFileHasRowHeadersUserSet(true);

        settings.setCommentUserSet(true);
        settings.setWhiteSpaceUserSet(true);

        settings = FileAnalyzer.analyze(settings, null);
        SettingsStatus status = settings.getStatusOfSettings();
        if (status.getNumOfErrors() > 0) {
            throw new IllegalStateException(status.getErrorMessage(0));
        }
        FileTable fTable = new FileTable(settings.createDataTableSpec(),
                settings, null);
        return exec.createBufferedDataTable(fTable, exec);
    }

}
