/* ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2008 - 2009
 * KNIME.com, Zurich, Switzerland
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
 * ---------------------------------------------------------------------
 *
 * History
 *   Jul 13, 2009 (ohl): created
 */
package org.knime.ext.ssh.node;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 *
 * @author Peter Ohl, KNIME.com, Zurich, Switzerland
 */
public class ExtSSHToolNodeFactory extends NodeFactory<ExtSSHToolNodeModel> {

    /**
     * {@inheritDoc}
     */
    @Override
    protected NodeDialogPane createNodeDialogPane() {
        return new ExtSSHToolNodeDialogPane();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ExtSSHToolNodeModel createNodeModel() {
        return new ExtSSHToolNodeModel();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NodeView<ExtSSHToolNodeModel> createNodeView(final int viewIndex,
            final ExtSSHToolNodeModel nodeModel) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected int getNrNodeViews() {
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean hasDialog() {
        return true;
    }

}
