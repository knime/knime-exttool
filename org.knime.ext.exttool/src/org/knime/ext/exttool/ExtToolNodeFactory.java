/* ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2008
 * University of Konstanz, Germany.
 * Chair for Bioinformatics and Information Mining
 * Prof. Dr. Michael R. Berthold
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any questions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
 * --------------------------------------------------------------------- *
 *
 * History
 *   12.10.2006 (ohl): created
 */
package org.knime.ext.exttool;

import org.knime.base.node.util.exttool.ExtToolOutputNodeView;
import org.knime.base.node.util.exttool.ExtToolStderrNodeView;
import org.knime.base.node.util.exttool.ExtToolStdoutNodeView;
import org.knime.core.node.GenericNodeFactory;
import org.knime.core.node.NodeDialogPane;

/**
 * Factory for the node that launches an external executable. It reuses the
 * views from the {@link org.knime.base.node.util.exttool} package.
 *
 * @author ohl, University of Konstanz
 */
public class ExtToolNodeFactory extends
        GenericNodeFactory<ExtToolNodeModel> {

    /**
     * {@inheritDoc}
     */
    @Override
    protected NodeDialogPane createNodeDialogPane() {
        return new ExtToolNodeDialogPane();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ExtToolNodeModel createNodeModel() {
        return new ExtToolNodeModel();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ExtToolOutputNodeView<ExtToolNodeModel> createNodeView(
            final int viewIndex, final ExtToolNodeModel nodeModel) {
        if (viewIndex == 0) {
            return new ExtToolStdoutNodeView<ExtToolNodeModel>(nodeModel);
        } else if (viewIndex == 1) {
            return new ExtToolStderrNodeView<ExtToolNodeModel>(nodeModel);
        } else {
            return null;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected int getNrNodeViews() {
        return 2;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean hasDialog() {
        return true;
    }

}
