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
 *   Jan 19, 2010 (wiswedel): created
 */
package org.knime.exttool.node;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/** Base implementation of the node factory for the external tool node. The
 * default is showing a free-format text area, in which the user can enter his
 * command line. Derived external tool nodes that want to have a custom design
 * need to extend this class and set a pre-configured {@link ExttoolCustomizer}
 * using the {@link #setExttoolCustomizer(ExttoolCustomizer)} method. The
 * configuration needs to be done in the constructor of the derived class. For
 * different extension options see the {@link ExttoolCustomizer} class
 * description and the {@link ExttoolNodeFactory#ExttoolNodeFactory() example}
 * in the constructor documentation.
 * @author Bernd Wiswedel, KNIME.com, Zurich, Switzerland
 */
public class ExttoolNodeFactory extends NodeFactory<ExttoolNodeModel> {

    private ExttoolCustomizer m_exttoolCustomizer;

    /** Default constructor that does not set a customizer yet. Subclasses
     * will override this constructor, create a customizer to their needs and
     * set it using the {@link #setExttoolCustomizer(ExttoolCustomizer)} method.
     * As an example, see the following code snippet, which defines a custom
     * command line settings object and disables some control panels.
     * <pre>
     * ExttoolCustomizer customizer = new ExttoolCustomizer() {
     *     protected AbstractCommandlineSettings createCommandlineSettings() {
     *         return new FooCommandlineSettings();
     *     }
     * };
     * customizer.setColumnFilter(BarValue.class, FooBarValue.class);
     * customizer.setShowPathToExecutableField(false);
     * customizer.setShowTabInputFile(false);
     * customizer.setShowTabOutputFile(false);
     * setExttoolCustomizer(customizer);
     * </pre>
     */
    public ExttoolNodeFactory() {
        // must not set default customizer here.
    }

    /** One time setter for the external tool customizer. This method can
     * be called at most once and must be called before any of factory methods
     * are used by the framework.
     * @param exttoolCustomizer the pre-configured customizer.
     * @see #ExttoolNodeFactory()
     */
    protected final void setExttoolCustomizer(
            final ExttoolCustomizer exttoolCustomizer) {
        if (exttoolCustomizer == null) {
            throw new NullPointerException("Argument must not be null");
        }
        if (m_exttoolCustomizer != null) {
            throw new IllegalStateException("Customizer already assigned");
        }
        m_exttoolCustomizer = exttoolCustomizer;
    }

    /** Get the customizer, create a default if non has been set. */
    private ExttoolCustomizer getExttoolCustomizerNonNull() {
        if (m_exttoolCustomizer == null) {
            m_exttoolCustomizer = new ExttoolCustomizer();
        }
        return m_exttoolCustomizer;
    }

    /** {@inheritDoc} */
    @Override
    protected NodeDialogPane createNodeDialogPane() {
        ExttoolCustomizer customizer = getExttoolCustomizerNonNull();
        ExttoolNodeDialogPane d = new ExttoolNodeDialogPane(customizer);
        d.initLayout();
        return d;
    }

    /** {@inheritDoc} */
    @Override
    public ExttoolNodeModel createNodeModel() {
        ExttoolCustomizer customizer = getExttoolCustomizerNonNull();
        return new ExttoolNodeModel(customizer);
    }

    /** {@inheritDoc} */
    @Override
    public NodeView<ExttoolNodeModel> createNodeView(
            final int viewIndex, final ExttoolNodeModel nodeModel) {
        throw new IndexOutOfBoundsException("invalid index: " + viewIndex);
    }

    /** {@inheritDoc} */
    @Override
    protected int getNrNodeViews() {
        return 0;
    }

    /** {@inheritDoc} */
    @Override
    protected boolean hasDialog() {
        return true;
    }

}
