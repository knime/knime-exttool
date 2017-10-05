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
 *   Jun 23, 2010 (wiswedel): created
 */
package org.knime.exttool.executor;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;

/** A default configuration class for executors. It allows setting the number
 * of threads for the execution.
 * @author Bernd Wiswedel, KNIME.com, Zurich, Switzerland
 */
public class DefaultExttoolExecutorConfig extends
    AbstractExttoolExecutorConfig {

    private int m_maxThreads;
    private boolean m_isAutoThreadCount;

    /**
     * @return the maxThreads
     */
    public int getMaxThreads() {
        return m_maxThreads;
    }

    /**
     * @param maxThreads the maxThreads to set
     * @throws InvalidSettingsException If argument &lt; 1
     */
    public void setMaxThreads(final int maxThreads)
        throws InvalidSettingsException {
        if (maxThreads < 1) {
            throw new InvalidSettingsException(
                    "thread count < 1: " + maxThreads);
        }
        m_maxThreads = maxThreads;
    }

    /**
     * @return the isAutoThreadCount
     */
    public boolean isAutoThreadCount() {
        return m_isAutoThreadCount;
    }

    /**
     * @param isAutoThreadCount the isAutoThreadCount to set
     */
    public void setAutoThreadCount(final boolean isAutoThreadCount) {
        m_isAutoThreadCount = isAutoThreadCount;
    }

    /** {@inheritDoc} */
    @Override
    public AbstractExttoolExecutorConfigPanel createConfigPanel() {
        return new DefaultExttoolExecutorConfigPanel();
    }

    /** {@inheritDoc} */
    @Override
    public void loadSettingsInDialog(final NodeSettingsRO settings)
            throws NotConfigurableException {
        m_isAutoThreadCount = settings.getBoolean("isAutoThreadCount", true);
        int autoThreadCount = getAutoThreadCount();
        int maxThreads = settings.getInt("maxThreads", autoThreadCount);
        if (maxThreads < 1) {
            maxThreads = autoThreadCount;
        }
        m_maxThreads = maxThreads;
    }

    /** {@inheritDoc} */
    @Override
    public void loadSettingsInModel(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_isAutoThreadCount = settings.getBoolean("isAutoThreadCount");
        if (m_isAutoThreadCount) {
            m_maxThreads = getAutoThreadCount();
        } else {
            m_maxThreads = settings.getInt("maxThreads");
            if (m_maxThreads < 1) {
                throw new InvalidSettingsException(
                        "Invalid thread count: " + m_maxThreads);
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public void saveSettings(final NodeSettingsWO settings) {
        settings.addBoolean("isAutoThreadCount", m_isAutoThreadCount);
        if (m_isAutoThreadCount) {
            // do not save max threads (auto-generated value could be different
            // on different systems
        } else {
            settings.addInt("maxThreads", m_maxThreads);
        }
    }

    /** Get a thread count suitable for the current system (a number slightly
     * larger than the system's core count.
     * @return the default thread count for the system.
     */
    protected static int getAutoThreadCount() {
        int cores = Runtime.getRuntime().availableProcessors();
        if (cores <= 2) {
            return cores + 1;
        } else if (cores <= 4) {
            return cores + 2;
        } else {
            return cores + 4;
        }
    }

    /** Id provider for threads. */
    private static final AtomicInteger THREAD_COUNTER = new AtomicInteger();

    /** {@inheritDoc} */
    @Override
    public ExecutorService createExecutorService() {
        int threadcount = m_isAutoThreadCount ? getAutoThreadCount()
                : m_maxThreads;
        return new ThreadPoolExecutor(threadcount, threadcount, 60L,
            TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(), new ThreadFactory() {
                /** {@inheritDoc} */
                @Override
                public Thread newThread(final Runnable r) {
                    return new Thread(r, "KNIME-Exttool-" + THREAD_COUNTER.incrementAndGet());
                }
            });
    }

}
