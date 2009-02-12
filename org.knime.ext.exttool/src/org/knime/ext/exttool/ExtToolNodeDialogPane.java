/* ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2009
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

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.reflect.InvocationTargetException;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.util.FilelistAccessory;
import org.knime.core.util.MutableBoolean;

/**
 * 
 * @author ohl, University of Konstanz
 */
public class ExtToolNodeDialogPane extends NodeDialogPane {

    private JTextField m_inFileName;

    private JTextField m_inSeparator;

    private JCheckBox m_includeColHdr;

    private JCheckBox m_includeRowHdr;

    private JTextField m_toolPath;

    private JTextField m_toolCwd;

    private JTextField m_toolArgs;

    private JTextField m_outFileName;

    private JTextField m_outSeparator;

    private JCheckBox m_containsColHdr;

    private JCheckBox m_containsRowHdr;

    /**
     * Le Constructeur.
     */
    public ExtToolNodeDialogPane() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.add(getInportDataFilePanel());
        panel.add(Box.createVerticalStrut(10));
        panel.add(getExternalToolPanel());
        panel.add(Box.createVerticalStrut(10));
        panel.add(getOutportDataFilePanel());
        panel.add(Box.createVerticalGlue());
        addTab("Launch settings", panel);
    }

    /**
     * @return a panel with file path and other settings for the temp file
     *         transferring data from the input port to the external tool
     */
    private JPanel getInportDataFilePanel() {

        JPanel result = new JPanel();
        result.setLayout(new BoxLayout(result, BoxLayout.Y_AXIS));
        result.setBorder(BorderFactory.createTitledBorder(BorderFactory
                .createEtchedBorder(), "Input Data File"));

        /*
         * the filename selection panel
         */
        m_inFileName = new JTextField(40);
        m_inFileName.setMaximumSize(new Dimension(35000, 25));
        m_inFileName.setMinimumSize(new Dimension(350, 25));
        m_inFileName.setPreferredSize(new Dimension(350, 25));
        JButton browse = new JButton("Browse...");
        browse.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                browseInFileName();
            }
        });

        Box filebox = Box.createHorizontalBox();
        filebox.add(new JLabel("Input file to external tool:"));
        filebox.add(Box.createHorizontalStrut(3));
        filebox.add(m_inFileName);
        filebox.add(Box.createHorizontalStrut(3));
        filebox.add(browse);

        /*
         * a panel with the format settings
         */
        m_inSeparator = new JTextField(1);
        m_inSeparator.setPreferredSize(new Dimension(50, 25));
        m_inSeparator.setMaximumSize(new Dimension(60, 25));
        m_inSeparator.setMinimumSize(new Dimension(30, 25));
        m_inSeparator.setColumns(5);
        m_includeColHdr = new JCheckBox("include column headers");
        m_includeRowHdr = new JCheckBox("include row IDs");

        JPanel format = new JPanel();
        format.setLayout(new BoxLayout(format, BoxLayout.X_AXIS));
        format.setBorder(BorderFactory.createTitledBorder(BorderFactory
                .createEtchedBorder(), "File Format"));
        // the column separator
        Box sepBox = Box.createHorizontalBox();
        sepBox.add(new JLabel("Column separator:"));
        sepBox.add(Box.createHorizontalStrut(2));
        sepBox.add(m_inSeparator);
        Box separatorBox = Box.createVerticalBox();
        separatorBox.add(sepBox);
        separatorBox.add(Box.createVerticalGlue());
        // Headers
        Box headerBox = Box.createVerticalBox();
        // column headers
        Box colHdrBox = Box.createHorizontalBox();
        colHdrBox.add(m_includeColHdr);
        colHdrBox.add(Box.createHorizontalGlue());
        // row headers
        Box rowHdrBox = Box.createHorizontalBox();
        rowHdrBox.add(m_includeRowHdr);
        rowHdrBox.add(Box.createHorizontalGlue());
        headerBox.add(colHdrBox);
        headerBox.add(rowHdrBox);
        headerBox.add(Box.createVerticalGlue());

        format.add(separatorBox);
        format.add(Box.createHorizontalStrut(25));
        format.add(headerBox);
        // center the entire thing
        Box formatBox = Box.createHorizontalBox();
        formatBox.add(Box.createHorizontalGlue());
        formatBox.add(format);
        formatBox.add(Box.createHorizontalGlue());

        result.add(filebox);
        result.add(formatBox);

        return result;
    }

    /**
     * Transfers the stored settings into the input data file panel - or sets
     * default values if the settings do not contain the corresponding key.
     * 
     * @param settings the object to read the settings from
     * @param inSpecs the input specs from the input ports.
     * @throws NotConfigurableException currently not.
     */
    private void loadInportDataFile(final NodeSettingsRO settings) {
        try {
            SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    m_inFileName.setText(settings.getString(
                            ExtToolNodeModel.CFGKEY_INFILENAME, ""));
                    m_inSeparator.setText(settings.getString(
                            ExtToolNodeModel.CFGKEY_INSEPARATOR, ""));
                    m_includeColHdr.setSelected(settings.getBoolean(
                            ExtToolNodeModel.CFGKEY_INCLCOLHDR, false));
                    m_includeRowHdr.setSelected(settings.getBoolean(
                            ExtToolNodeModel.CFGKEY_INCLROWHDR, false));
                }
            });
        } catch (InterruptedException e) {
            // Empty
        } catch (InvocationTargetException e) {
            // Empty
        }

    }

    private void saveInportDataFile(final NodeSettingsWO settings)
            throws InvalidSettingsException {
        final StringBuilder filename = new StringBuilder();
        final StringBuilder separator = new StringBuilder();
        final MutableBoolean inclColHdr = new MutableBoolean(false);
        final MutableBoolean inclRowHdr = new MutableBoolean(false);

        try {
            SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    filename.append(m_inFileName.getText());
                    separator.append(m_inSeparator.getText());
                    inclColHdr.setValue(m_includeColHdr.isSelected());
                    inclRowHdr.setValue(m_includeRowHdr.isSelected());
                }
            });
        } catch (InterruptedException e) {
            throw new InvalidSettingsException(e);
        } catch (InvocationTargetException e) {
            throw new InvalidSettingsException(e);
        }

        if ((filename.toString() == null)
                || (filename.toString().length() == 0)) {
            throw new InvalidSettingsException("Please specify an input "
                    + "filename.");
        }
        if ((separator.toString() == null)
                || (separator.toString().length() == 0)) {
            throw new InvalidSettingsException("Please specify a separator"
                    + " to use for the input file.");
        }

        settings.addString(ExtToolNodeModel.CFGKEY_INFILENAME, filename
                .toString());
        settings.addString(ExtToolNodeModel.CFGKEY_INSEPARATOR, separator
                .toString());
        settings.addBoolean(ExtToolNodeModel.CFGKEY_INCLCOLHDR, inclColHdr
                .booleanValue());
        settings.addBoolean(ExtToolNodeModel.CFGKEY_INCLROWHDR, inclRowHdr
                .booleanValue());

    }

    /**
     * @return the panel for the settings to call the external tool
     */
    private JPanel getExternalToolPanel() {

        JPanel result = new JPanel();
        result.setLayout(new BoxLayout(result, BoxLayout.Y_AXIS));
        result.setBorder(BorderFactory.createTitledBorder(BorderFactory
                .createEtchedBorder(), "External Tool"));

        m_toolPath = new JTextField(40);
        m_toolPath.setPreferredSize(new Dimension(350, 25));
        m_toolPath.setMaximumSize(new Dimension(35000, 25));
        m_toolPath.setMinimumSize(new Dimension(35, 25));
        m_toolCwd = new JTextField(40);
        m_toolCwd.setPreferredSize(new Dimension(350, 25));
        m_toolCwd.setMaximumSize(new Dimension(35000, 25));
        m_toolCwd.setMinimumSize(new Dimension(35, 25));
        m_toolArgs = new JTextField(50);
        m_toolArgs.setPreferredSize(new Dimension(350, 25));
        m_toolArgs.setMaximumSize(new Dimension(35000, 25));
        m_toolArgs.setMinimumSize(new Dimension(35, 25));
        JButton pathButton = new JButton("Browse...");
        pathButton.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                browseExecPath();
            }
        });
        JButton dirButton = new JButton("Browse...");
        dirButton.addActionListener(new ActionListener() {

            public void actionPerformed(final ActionEvent e) {
                browseDirPath();
            }
        });
        Box pathBox = Box.createHorizontalBox();
        pathBox.add(new JLabel("Path to Executable:"));
        pathBox.add(Box.createHorizontalStrut(2));
        pathBox.add(m_toolPath);
        pathBox.add(Box.createHorizontalStrut(2));
        pathBox.add(pathButton);
        pathBox.add(Box.createHorizontalGlue());

        Box dirBox = Box.createHorizontalBox();
        dirBox.add(new JLabel("Execute in directory:"));
        dirBox.add(Box.createHorizontalStrut(2));
        dirBox.add(m_toolCwd);
        dirBox.add(Box.createHorizontalStrut(2));
        dirBox.add(dirButton);
        dirBox.add(Box.createHorizontalGlue());

        Box argBox = Box.createHorizontalBox();
        argBox.add(new JLabel("Commandline Arguments:"));
        argBox.add(Box.createHorizontalStrut(2));
        argBox.add(m_toolArgs);
        argBox.add(Box.createHorizontalGlue());

        result.add(pathBox);
        result.add(Box.createVerticalStrut(3));
        result.add(dirBox);
        result.add(Box.createVerticalStrut(3));
        result.add(argBox);

        return result;
    }

    private void loadExternalTool(final NodeSettingsRO settings) {
        try {
            SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    m_toolPath.setText(settings.getString(
                            ExtToolNodeModel.CFGKEY_EXTTOOLPATH, ""));
                    m_toolCwd.setText(settings.getString(
                            ExtToolNodeModel.CFGKEY_EXTTOOLCWD, ""));
                    m_toolArgs.setText(settings.getString(
                            ExtToolNodeModel.CFGKEY_EXTTOOLARGS, ""));
                }
            });
        } catch (InterruptedException e) {
            // Empty
        } catch (InvocationTargetException e) {
            // Empty
        }

    }

    private void saveExternalTool(final NodeSettingsWO settings)
            throws InvalidSettingsException {
        final StringBuilder filename = new StringBuilder();
        final StringBuilder cwd = new StringBuilder();
        final StringBuilder args = new StringBuilder();

        try {
            SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    filename.append(m_toolPath.getText());
                    cwd.append(m_toolCwd.getText());
                    args.append(m_toolArgs.getText());
                }
            });
        } catch (InterruptedException e) {
            throw new InvalidSettingsException(e);
        } catch (InvocationTargetException e) {
            throw new InvalidSettingsException(e);
        }

        if ((filename.toString() == null)
                || (filename.toString().length() == 0)) {
            throw new InvalidSettingsException("Please specify the path to"
                    + " the external executable.");
        }

        settings.addString(ExtToolNodeModel.CFGKEY_EXTTOOLPATH, filename
                .toString());
        settings.addString(ExtToolNodeModel.CFGKEY_EXTTOOLCWD, cwd.toString());
        settings.addString(ExtToolNodeModel.CFGKEY_EXTTOOLARGS, 
                args.toString());

    }

    private JPanel getOutportDataFilePanel() {
        JPanel result = new JPanel();
        result.setLayout(new BoxLayout(result, BoxLayout.Y_AXIS));
        result.setBorder(BorderFactory.createTitledBorder(BorderFactory
                .createEtchedBorder(), "Output Data File"));

        /*
         * the filename selection panel
         */
        m_outFileName = new JTextField(40);
        m_outFileName.setMaximumSize(new Dimension(35000, 25));
        m_outFileName.setMinimumSize(new Dimension(350, 25));
        m_outFileName.setPreferredSize(new Dimension(350, 25));
        JButton browse = new JButton("Browse...");
        browse.setPreferredSize(new Dimension(100, 25));
        browse.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                browseOutFileName();
            }
        });

        Box filebox = Box.createHorizontalBox();
        filebox.add(new JLabel("Output file from external tool:"));
        filebox.add(Box.createHorizontalStrut(3));
        filebox.add(m_outFileName);
        filebox.add(Box.createHorizontalStrut(3));
        filebox.add(browse);

        /*
         * a panel with the format settings
         */
        m_outSeparator = new JTextField(1);
        m_outSeparator.setPreferredSize(new Dimension(50, 25));
        m_outSeparator.setMaximumSize(new Dimension(60, 25));
        m_outSeparator.setMinimumSize(new Dimension(30, 25));
        m_outSeparator.setColumns(5);
        m_containsColHdr = new JCheckBox("contains column headers");
        m_containsRowHdr = new JCheckBox("contains row IDs");

        JPanel format = new JPanel();
        format.setLayout(new BoxLayout(format, BoxLayout.X_AXIS));
        format.setBorder(BorderFactory.createTitledBorder(BorderFactory
                .createEtchedBorder(), "File Format"));
        // the column separator
        Box sepBox = Box.createHorizontalBox();
        sepBox.add(new JLabel("Column separator:"));
        sepBox.add(Box.createHorizontalStrut(2));
        sepBox.add(m_outSeparator);
        Box separatorBox = Box.createVerticalBox();
        separatorBox.add(sepBox);
        separatorBox.add(Box.createVerticalGlue());
        // Headers
        Box headerBox = Box.createVerticalBox();
        // column headers
        Box colHdrBox = Box.createHorizontalBox();
        colHdrBox.add(m_containsColHdr);
        colHdrBox.add(Box.createHorizontalGlue());
        // row headers
        Box rowHdrBox = Box.createHorizontalBox();
        rowHdrBox.add(m_containsRowHdr);
        rowHdrBox.add(Box.createHorizontalGlue());
        headerBox.add(colHdrBox);
        headerBox.add(rowHdrBox);
        headerBox.add(Box.createVerticalGlue());

        format.add(separatorBox);
        format.add(Box.createHorizontalStrut(25));
        format.add(headerBox);
        // center the entire thing
        Box formatBox = Box.createHorizontalBox();
        formatBox.add(Box.createHorizontalGlue());
        formatBox.add(format);
        formatBox.add(Box.createHorizontalGlue());

        result.add(filebox);
        result.add(formatBox);

        return result;

    }

    private void loadOutportDataFile(final NodeSettingsRO settings) {
        try {
            SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    m_outFileName.setText(settings.getString(
                            ExtToolNodeModel.CFGKEY_OUTFILENAME, ""));
                    m_outSeparator.setText(settings.getString(
                            ExtToolNodeModel.CFGKEY_OUTSEPARATOR, ""));
                    m_containsColHdr.setSelected(settings.getBoolean(
                            ExtToolNodeModel.CFGKEY_HASCOLHDR, false));
                    m_containsRowHdr.setSelected(settings.getBoolean(
                            ExtToolNodeModel.CFGKEY_HASROWHDR, false));
                }
            });
        } catch (InterruptedException e) {
            // Empty
        } catch (InvocationTargetException e) {
            // Empty
        }

    }

    private void saveOutportDataFile(final NodeSettingsWO settings)
            throws InvalidSettingsException {
        final StringBuilder filename = new StringBuilder();
        final StringBuilder separator = new StringBuilder();
        final MutableBoolean hasColHdr = new MutableBoolean(false);
        final MutableBoolean hasRowHdr = new MutableBoolean(false);

        try {
            SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    filename.append(m_outFileName.getText());
                    separator.append(m_outSeparator.getText());
                    hasColHdr.setValue(m_containsColHdr.isSelected());
                    hasRowHdr.setValue(m_containsRowHdr.isSelected());
                }
            });
        } catch (InterruptedException e) {
            throw new InvalidSettingsException(e);
        } catch (InvocationTargetException e) {
            throw new InvalidSettingsException(e);
        }

        if ((filename.toString() == null)
                || (filename.toString().length() == 0)) {
            throw new InvalidSettingsException("Please specify an output "
                    + "filename.");
        }
        if ((separator.toString() == null)
                || (separator.toString().length() == 0)) {
            throw new InvalidSettingsException("Please specify the separator"
                    + " of the output file.");
        }

        settings.addString(ExtToolNodeModel.CFGKEY_OUTFILENAME, filename
                .toString());
        settings.addString(ExtToolNodeModel.CFGKEY_OUTSEPARATOR, separator
                .toString());
        settings.addBoolean(ExtToolNodeModel.CFGKEY_HASCOLHDR, hasColHdr
                .booleanValue());
        settings.addBoolean(ExtToolNodeModel.CFGKEY_HASROWHDR, hasRowHdr
                .booleanValue());

    }

    /**
     * Opens a file chooser to select the inport data file name and sets it in
     * the edit field.
     */
    private void browseInFileName() {
        String newFile = getFileName(m_inFileName.getText(),
        /* filesOnly= */true);
        if (newFile != null) {
            m_inFileName.setText(newFile);
        }
    }

    /**
     * Sets a new path to the executable by opening a file chooser (or leaves it
     * unchanged if the user cancels).
     */
    private void browseExecPath() {
        String newFile =
                getFileName(m_toolPath.getText(), /* filesOnly= */true);
        if (newFile != null) {
            m_toolPath.setText(newFile);
        }
    }

    /**
     * Opens a file chooser to select the output data file name and sets it in
     * the edit field.
     */
    private void browseOutFileName() {
        String newFile = getFileName(m_outFileName.getText(),
        /* filesOnly= */true);
        if (newFile != null) {
            m_outFileName.setText(newFile);
        }
    }

    /**
     * Opens a file chooser and sets a new working directory for the external
     * tool - or leaves it unchanged if the user cancels.
     */
    private void browseDirPath() {
        String newFile =
                getFileName(m_toolCwd.getText(), /* filesOnly= */false);
        if (newFile != null) {
            m_toolCwd.setText(newFile);
        }
    }

    /**
     * Opens the file chooser dialog and returns the selected file - or null if
     * the user canceled.
     * 
     * @param startingDir the directory to start the chooser in.
     * @param filesOnly if true only files are accepted as selection, if false,
     *            only directories are accepted.
     * @return the selected file name or null if user canceled.
     */
    private String getFileName(final String startingDir,
            final boolean filesOnly) {
        JFileChooser chooser;
        chooser = new JFileChooser(startingDir);
        if (filesOnly) {
            chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        } else {
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            chooser.setAccessory(new FilelistAccessory(chooser));
        }

        // make dialog modal
        int returnVal = chooser.showOpenDialog(getPanel().getParent());
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            String path;
            path = chooser.getSelectedFile().getAbsolutePath();
            return path;
        }
        // user canceled - return null
        return null;

    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings,
            final DataTableSpec[] specs) throws NotConfigurableException {
        loadInportDataFile(settings);
        loadExternalTool(settings);
        loadOutportDataFile(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings)
            throws InvalidSettingsException {
        saveInportDataFile(settings);
        saveExternalTool(settings);
        saveOutportDataFile(settings);
    }

}
