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
 *   Jan 20, 2010 (wiswedel): created
 */
package org.knime.exttool.node;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.config.Config;
import org.knime.exttool.executor.AbstractExttoolExecutorFactory;
import org.knime.exttool.executor.DefaultExttoolExecutorFactory;
import org.knime.exttool.filetype.AbstractFileTypeFactory;
import org.knime.exttool.filetype.AbstractFileTypeRead;
import org.knime.exttool.filetype.AbstractFileTypeWrite;
import org.knime.exttool.filetype.csv.CSVFileTypeFactory;

/**
 * Settings tree containing a node configuration. Typical use case is that prior
 * to saving the node settings (either in the {@link ExttoolNodeModel} or the
 * {@link ExttoolNodeDialogPane} a new object of this class is instantiated
 * (using the {@link ExttoolCustomizer#createExttoolSettings()} method, the
 * properties are set using the different set-methods and finally the
 * then-actual configuration is saved using
 * {@link #saveSettingsTo(NodeSettingsWO)}. The loading of settings works in a
 * similar manner.
 *
 * Objects of this class should only be instantiated using the factory method
 * in the customizer.
 *
 * @author Bernd Wiswedel, KNIME.com, Zurich, Switzerland
 */
public class ExttoolSettings {

    private final ExttoolCustomizer m_customizer;
    private final AbstractCommandlineSettings m_commandlineSettings;
    private final PathAndTypeConfiguration[] m_inputConfigs;
    private final PathAndTypeConfiguration[] m_outputConfigs;
    private String m_executorClassName;
    private String m_pathToExecutable;
    private String m_targetColumn;
    private int m_chunkSize;

    /** Create a new settings object from the given customizer.
     * @param customizer The corresponding customizer.
     */
    protected ExttoolSettings(final ExttoolCustomizer customizer) {
        if (customizer == null) {
            throw new NullPointerException("Argument must not be null");
        }
        m_customizer = customizer;
        m_commandlineSettings = customizer.createCommandlineSettings();
        if (m_commandlineSettings == null) {
            throw new NullPointerException(customizer.getClass().getSimpleName()
                    + " must not create null command line settings");
        }
        int inCount = m_customizer.getNrInputs();
        int outCount = m_customizer.getNrOutputs();
        m_inputConfigs = new PathAndTypeConfiguration[inCount];
        for (int i = 0; i < inCount; i++) {
            m_inputConfigs[i] = new PathAndTypeConfiguration(true);
        }
        m_outputConfigs = new PathAndTypeConfiguration[outCount];
        for (int o = 0; o < outCount; o++) {
            m_outputConfigs[o] = new PathAndTypeConfiguration(false);
        }
        m_executorClassName = DefaultExttoolExecutorFactory.class.getName();
    }

    /** Get the name of the {@link AbstractExttoolExecutorFactory
     * executor factory}.
     * @return the executor class name (may be invalid).
     * @see #getExecutorFactory()
     */
    public String getExecutorClassName() {
        return m_executorClassName;
    }

    /** Look-up the executor factory.
     * @return The execution factory.
     * @throws InvalidSettingsException If the factory is invalid.
     * @see #getExecutorClassName()
     */
    public AbstractExttoolExecutorFactory getExecutorFactory()
        throws InvalidSettingsException {
        return AbstractExttoolExecutorFactory.get(m_executorClassName);
    }

    /** Set (class) name of executor.
     * @param executorClassName the executorClassName to set
     * @throws InvalidSettingsException If the class name is invalid according
     * to {@link AbstractExttoolExecutorFactory#get(String)}
     */
    public void setExecutorClassName(final String executorClassName)
        throws InvalidSettingsException {
        if (m_executorClassName == null) {
            throw new InvalidSettingsException("No executor class name set");
        }
        // validate class name
        AbstractExttoolExecutorFactory.get(executorClassName);
        m_executorClassName = executorClassName;
    }

    /**
     * @return the pathToExecutable
     */
    public String getPathToExecutable() {
        return m_pathToExecutable;
    }
    /**
     * @param pathToExecutable the pathToExecutable to set
     */
    public void setPathToExecutable(final String pathToExecutable) {
        m_pathToExecutable = pathToExecutable;
    }
    /**
     * @return the targetColumn
     */
    public String getTargetColumn() {
        return m_targetColumn;
    }
    /**
     * @param targetColumn the targetColumn to set
     */
    public void setTargetColumn(final String targetColumn) {
        m_targetColumn = targetColumn;
    }
    /** Get the chunk size, see {@link #setChunkSize(int)} for conventions.
     * @return the chunkSize
     */
    public int getChunkSize() {
        return m_chunkSize;
    }
    /** Set the number of rows to be processed in a chunk. This includes:
     * <dl>
     *   <dt>chunkSize &lt;= 0</dt>
     *   <dd>Process entire table in one chunk</dd>
     *   <dt>chunkSize = 1</dt>
     *   <dd>Process each row individually</dd>
     *   <dt>chunkSize &gt; 1</dt>
     *   <dd>Process chunks of the given size</dd>
     * </dl>
     * @param chunkSize the chunkSize to set
     */
    public void setChunkSize(final int chunkSize) {
        if (chunkSize <= 0) {
            m_chunkSize = -1;
        } else {
            m_chunkSize = chunkSize;
        }
    }

    /** Get commandline settings object. This object is created using the
     * {@link ExttoolCustomizer#createCommandlineSettings() factory method} in
     * the {@link ExttoolCustomizer customizer}.
     * @return the commandlineSettings, never null.
     */
    public AbstractCommandlineSettings getCommandlineSettings() {
        return m_commandlineSettings;
    }

    /** Get the current command line settings from the
     * {@link AbstractCommandlineSettings}. The command line args potentially
     * contain place holders.
     * @return The command line arguments
     * @throws InvalidSettingsException If the get-method on the
     * {@link AbstractCommandlineSettings#getCommandlineArgs() get-method} fails
     */
    public String[] getCommandlineArgs() throws InvalidSettingsException {
        return m_commandlineSettings.getCommandlineArgs();
    }

    /** Get the current input configuration for an input port.
     * @param inPort Port of interest.
     * @return The input configuration. */
    public PathAndTypeConfiguration getInputConfig(final int inPort) {
        return m_inputConfigs[inPort];
    }

    /** Set the input configuration for a port.
     * @param inPort Port of interest.
     * @param config The new config.
     * @throws IllegalArgumentException If the config argument is null
     *         or invalid (no input).
     */
    public void setInputConfig(final int inPort,
            final PathAndTypeConfiguration config) {
        if (config == null || !config.isInput()) {
            throw new IllegalArgumentException("Illegal argument: " + config);
        }
        m_inputConfigs[inPort] = config;
    }

    /** Get the current output configuration for an output port.
     * @param outPort Port of interest.
     * @return The output configuration. */
    public PathAndTypeConfiguration getOutputConfig(final int outPort) {
        return m_outputConfigs[outPort];
    }

    /** Set the output configuration for a port.
     * @param outPort Port of interest.
     * @param config The new config.
     * @throws IllegalArgumentException If the config argument is null
     *         or invalid (no output).
     */
    public void setOutputConfig(final int outPort,
            final PathAndTypeConfiguration config) {
        if (config == null || config.isInput()) {
            throw new IllegalArgumentException("Illegal argument: " + config);
        }
        m_outputConfigs[outPort] = config;
    }

    /** Create a {@link AbstractFileTypeWrite} instance for the given input
     * port. It will never return null, the returned object is fully
     * initialized, including the call to
     * {@link AbstractFileTypeWrite#loadSettings(NodeSettingsRO)}.
     * @param port The port of interest.
     * @return The initialized file type, never null.
     * @throws InvalidSettingsException If the settings are incomplete.
     */
    public AbstractFileTypeWrite createInputFileType(final int port)
        throws InvalidSettingsException {
        if (port < 0 || port >= m_customizer.getNrInputs()) {
            throw new IndexOutOfBoundsException("Invalid port index: " + port);
        }
        PathAndTypeConfiguration config = getInputConfig(port);
        if (config == null || config.getType() == null) {
            throw new InvalidSettingsException("No input file type associated"
                    + " with port " + port);
        }
        String type = config.getType();
        NodeSettings settings = config.getTypeSettings();
        if (settings == null) {
            throw new InvalidSettingsException("No settings associated with "
                    + "file type \"" + type + "\" at port " + port);
        }

        AbstractFileTypeFactory fac = AbstractFileTypeFactory.get(type);
        AbstractFileTypeWrite instance = fac.createNewWriteInstance();
        instance.loadSettings(settings);
        return instance;
    }

    /** Create a {@link AbstractFileTypeRead} instance for the given output
     * port. It will never return null, the returned object is fully
     * initialized, including the call to
     * {@link AbstractFileTypeRead#loadSettings(NodeSettingsRO)}.
     * @param port The port of interest.
     * @return The initialized file type, never null.
     * @throws InvalidSettingsException If the settings are incomplete.
     */
    public AbstractFileTypeRead createOutputFileType(final int port)
    throws InvalidSettingsException {
        if (port < 0 || port >= m_customizer.getNrOutputs()) {
            throw new IndexOutOfBoundsException("Invalid port index: " + port);
        }
        PathAndTypeConfiguration config = getOutputConfig(port);
        if (config == null || config.getType() == null) {
            throw new InvalidSettingsException("No output file type associated"
                    + " with port " + port);
        }
        String type = config.getType();
        NodeSettings settings = config.getTypeSettings();
        if (settings == null) {
            throw new InvalidSettingsException("No settings associated with "
                    + "file type \"" + type + "\" at port " + port);
        }

        AbstractFileTypeFactory fac = AbstractFileTypeFactory.get(type);
        AbstractFileTypeRead instance = fac.createNewReadInstance();
        instance.loadSettings(settings);
        return instance;
    }

    /** Saves the current configuration.
     * @param settings To save to. */
    public void saveSettingsTo(final NodeSettingsWO settings) {
        settings.addString("executorClassName", m_executorClassName);
        settings.addString("pathToExecutable", m_pathToExecutable);
        settings.addString("targetColumn", m_targetColumn);
        settings.addInt("chunkSize", m_chunkSize);
        NodeSettingsWO inSettings = settings.addNodeSettings("inports");
        final int inCount = m_customizer.getNrInputs();
        for (int i = 0; i < inCount; i++) {
            PathAndTypeConfiguration config = getInputConfig(i);
            NodeSettingsWO sub = inSettings.addNodeSettings("inport_" + i);
            if (config != null) {
                config.save(sub);
            }
        }

        NodeSettingsWO outSettings = settings.addNodeSettings("outports");
        final int outCount = m_customizer.getNrOutputs();
        for (int o = 0; o < outCount; o++) {
            NodeSettingsWO sub = outSettings.addNodeSettings("outport_" + o);
            PathAndTypeConfiguration config = getOutputConfig(o);
            if (config != null) {
                config.save(sub);
            }
        }
        NodeSettingsWO commandSub = settings.addNodeSettings("commandLine");
        m_commandlineSettings.saveSettings(commandSub);
    }

    /** Loads a configuration, which was previously stored using the
     * {@link #saveSettingsTo(NodeSettingsWO) save method}. This method is
     * used in the model.
     * @param settings To load from.
     * @throws InvalidSettingsException If settings are incomplete. */
    public void loadSettingsFrom(final NodeSettingsRO settings)
        throws InvalidSettingsException {
        String executorClassName = settings.getString("executorClassName");
        setExecutorClassName(executorClassName);

        m_pathToExecutable = settings.getString("pathToExecutable");
        m_targetColumn = settings.getString("targetColumn");
        m_chunkSize = settings.getInt("chunkSize");

        /* Load inport related settings, including file path, type, ... */
        NodeSettingsRO inSettings = settings.getNodeSettings("inports");
        final int inCount = m_customizer.getNrInputs();
        for (int i = 0; i < inCount; i++) {
            NodeSettingsRO sub = inSettings.getNodeSettings("inport_" + i);
            PathAndTypeConfiguration config =
                new PathAndTypeConfiguration(true);
            config.load(sub);
            m_inputConfigs[i] = config;
        }

        /* Load outport related settings, including file path, type, ... */
        NodeSettingsRO outSettings = settings.getNodeSettings("outports");
        final int outCount = m_customizer.getNrOutputs();
        for (int o = 0; o < outCount; o++) {
            NodeSettingsRO sub = outSettings.getNodeSettings("outport_" + o);
            PathAndTypeConfiguration config =
                new PathAndTypeConfiguration(false);
            config.load(sub);
            m_outputConfigs[o] = config;
        }

        NodeSettingsRO commandSub = settings.getNodeSettings("commandLine");
        m_commandlineSettings.loadSettingsInModel(commandSub);
    }

    /** Loads the configuration, does not throw exception in case of problems.
     * This method is used in the dialog.
     * @param settings To load from.
     * @param inSpecs The input table specs (to init defaults).
     * @throws NotConfigurableException
     *         If no appropriate input column is available.
     */
    public void loadSettingsFrom(final NodeSettingsRO settings,
            final DataTableSpec[] inSpecs) throws NotConfigurableException {
        String executorClassName = settings.getString("executorClassName",
                DefaultExttoolExecutorFactory.class.getName());
        try {
            // also validates ID
            setExecutorClassName(executorClassName);
        } catch (InvalidSettingsException ise) {
            // the default executor must work (locally registered)
            m_executorClassName = DefaultExttoolExecutorFactory.class.getName();
        }
        m_pathToExecutable = settings.getString("pathToExecutable", "");
        m_targetColumn = settings.getString("targetColumn", "");
        m_chunkSize = settings.getInt("chunkSize", -1);

        /* Load inport related settings, including file path, type, ... */
        NodeSettingsRO inportSettings;
        try {
            inportSettings = settings.getNodeSettings("inports");
        } catch (InvalidSettingsException e1) {
            inportSettings = new NodeSettings("empty");
        }
        final int inCount = m_customizer.getNrInputs();
        for (int i = 0; i < inCount; i++) {
            NodeSettingsRO sub;
            try {
                sub = inportSettings.getNodeSettings("inport_" + i);
            } catch (InvalidSettingsException e) {
                sub = new NodeSettings("empty");
            }

            PathAndTypeConfiguration config =
                new PathAndTypeConfiguration(true);
            config.loadNoFail(sub);
            m_inputConfigs[i] = config;
        }

        /* Load outport related settings, including file path, type, ... */
        NodeSettingsRO outportSettings;
        try {
            outportSettings = settings.getNodeSettings("outports");
        } catch (InvalidSettingsException e1) {
            outportSettings = new NodeSettings("empty");
        }
        final int outCount = m_customizer.getNrOutputs();
        for (int o = 0; o < outCount; o++) {
            NodeSettingsRO sub;
            try {
                sub = outportSettings.getNodeSettings("outport_" + o);
            } catch (InvalidSettingsException e) {
                sub = new NodeSettings("empty");
            }

            PathAndTypeConfiguration config =
                new PathAndTypeConfiguration(false);
            config.loadNoFail(sub);
            m_outputConfigs[o] = config;
        }

        NodeSettingsRO commandSub;
        try {
            commandSub = settings.getNodeSettings("commandLine");
        } catch (InvalidSettingsException e) {
            commandSub = new NodeSettings("empty");
        }
        m_commandlineSettings.loadSettingsInDialog(commandSub, inSpecs);
    }

    /** Config object representing path, type and type settings to an in- or
     * outport. The path may be null to indicate that a temp file path should
     * be created. Type is an identifier (class name)
     * (see {@link AbstractFileTypeFactory#getID()}) */
    public static final class PathAndTypeConfiguration {

        private final boolean m_isInput;
        private String m_path;
        private String m_type;
        private NodeSettings m_typeSettings;

        /** Create new config.
         * @param isInput <code>true</code> if this represent an input port,
         *        <code>false</code> for output port.
         */
        public PathAndTypeConfiguration(final boolean isInput) {
            m_isInput = isInput;
        }

        /**@return the isInput (constructor argument). */
        public boolean isInput() {
            return m_isInput;
        }

        /** @return the path being set (null for temp file creation) */
        public String getPath() {
            return m_path;
        }
        /** @param path the path to set (or null) */
        public void setPath(final String path) {
            m_path = path;
        }

        /** @return the type (see {@link AbstractFileTypeFactory#getID()})
         */
        public String getType() {
            return m_type;
        }
        /** Set type to use, see {@link AbstractFileTypeFactory#getID()}.
         * @param type the type to set
         */
        public void setType(final String type) {
            m_type = type;
        }
        /** @return the settings to the selected type. */
        public NodeSettings getTypeSettings() {
            return m_typeSettings;
        }
        /** @param typeSettings the new type settings. */
        public void setTypeSettings(final NodeSettings typeSettings) {
            m_typeSettings = typeSettings;
        }

        /** Saves the current configuration.
         * @param settings To save to. */
        public void save(final NodeSettingsWO settings) {
            settings.addString("path", m_path);
            settings.addString("type", m_type);
            if (m_typeSettings != null) {
                NodeSettingsWO conf =
                    settings.addNodeSettings("fileTypeConfig");
                m_typeSettings.copyTo(conf);
            }
        }

        /** Loads the configuration.
         * @param settings To load from.
         * @throws InvalidSettingsException If settings are invalid. */
        public void load(final NodeSettingsRO settings)
            throws InvalidSettingsException {
            m_path = settings.getString("path");
            m_type = settings.getString("type");
            AbstractFileTypeFactory fac = AbstractFileTypeFactory.get(m_type);
            NodeSettingsRO conf = settings.getNodeSettings("fileTypeConfig");
            NodeSettings confClone = new NodeSettings("fileTypeConfig");
            if (conf instanceof Config) {
                ((Config)conf).copyTo(confClone);
            } else {
                throw new IllegalStateException("Config objects of type "
                        + conf.getClass().getSimpleName()
                        + " not supported");
            }
            if (m_isInput) {
                fac.createNewWriteInstance().loadSettings(confClone);
            } else { // output
                fac.createNewReadInstance().loadSettings(confClone);
            }
            m_typeSettings = confClone;
        }

        /** Loads settings but does not through exception (using defaults then).
         * @param settings To load from. */
        public void loadNoFail(final NodeSettingsRO settings) {
            try {
                load(settings);
            } catch (InvalidSettingsException e) {
                m_path = null;
                m_type = CSVFileTypeFactory.class.getName();
                m_typeSettings = new NodeSettings("empty_settings");
            }
        }
    }

}
