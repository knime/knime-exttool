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
 *   Jan 20, 2010 (wiswedel): created
 */
package org.knime.exttool.node;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.config.Config;
import org.knime.exttool.executor.AbstractExttoolExecutorConfig;
import org.knime.exttool.executor.AbstractExttoolExecutorFactory;
import org.knime.exttool.executor.DefaultExttoolExecutorFactory;
import org.knime.exttool.filetype.AbstractFileTypeFactory;
import org.knime.exttool.filetype.AbstractFileTypeRead;
import org.knime.exttool.filetype.AbstractFileTypeReadConfig;
import org.knime.exttool.filetype.AbstractFileTypeWrite;
import org.knime.exttool.filetype.AbstractFileTypeWriteConfig;
import org.knime.exttool.filetype.csv.CSVFileTypeFactory;
import org.knime.exttool.node.ExttoolCustomizer.Chunking;
import org.knime.exttool.node.ExttoolCustomizer.DeleteTempFilePolicy;

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
 * @author Bernd Wiswedel, KNIME AG, Zurich, Switzerland
 */
public class ExttoolSettings {

    private static final NodeLogger LOGGER =
        NodeLogger.getLogger(ExttoolSettings.class);

    /** An empty settings object, used to init defaults. */
    public static final NodeSettingsRO EMPTY_SETTINGS =
        new NodeSettings("empty");

    private final ExttoolCustomizer m_customizer;
    private final AbstractCommandlineSettings m_commandlineSettings;
    private final PathAndTypeConfigurationInput[] m_inputConfigs;
    private final PathAndTypeConfigurationOutput[] m_outputConfigs;
    private DeleteTempFilePolicy m_deleteTempFilePolicy;
    private AbstractExttoolExecutorFactory m_executorFactory;
    private AbstractExttoolExecutorConfig m_executorConfig;
    private String m_pathToExecutable;
    private Chunking m_chunking = Chunking.EntireTable;
    private int m_chunkValue;
    private String m_multipleResultRowKeySeparator = "_";

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
        m_inputConfigs = new PathAndTypeConfigurationInput[inCount];
        for (int i = 0; i < inCount; i++) {
            m_inputConfigs[i] = new PathAndTypeConfigurationInput();
        }
        m_outputConfigs = new PathAndTypeConfigurationOutput[outCount];
        for (int o = 0; o < outCount; o++) {
            m_outputConfigs[o] = new PathAndTypeConfigurationOutput();
        }
        try {
            m_executorFactory =
                AbstractExttoolExecutorFactory.getDefault(m_customizer);
            m_executorConfig = m_executorFactory.createConfig();
        } catch (InvalidSettingsException e) {
            LOGGER.error("No local executor available", e);
        }
        m_deleteTempFilePolicy = customizer.getDefaultDeleteTempFilePolicy();
    }

    /** Get current executor factory.
     * @return The execution factory.
     */
    public AbstractExttoolExecutorFactory getExecutorFactory() {
        return m_executorFactory;
    }

    /** Get configuration to the current executor factory.
     * @return Current executor config.
     */
    public AbstractExttoolExecutorConfig getExecutorConfig() {
        return m_executorConfig;
    }

    /** Sets executor factory and corresponding configuration.
     * @param factory The factory to set.
     * @param config Its configuration, this can be as simply as
     *        {@link AbstractExttoolExecutorFactory#createConfig()
     *        factory.createConfig()}.
     * @throws InvalidSettingsException If either argument is null or the config
     *         class is not of the expected type
     */
    public void setExecutor(final AbstractExttoolExecutorFactory factory,
            final AbstractExttoolExecutorConfig config)
    throws InvalidSettingsException {
        if (factory == null || config == null) {
            throw new InvalidSettingsException("No executor factory set");
        }
        AbstractExttoolExecutorConfig refConfig = factory.createConfig();
        if (!refConfig.getClass().equals(config.getClass())) {
            throw new InvalidSettingsException("Invalid config class type,"
                    + " got \"" + config.getClass().getName()
                    + "\", expected \"" + refConfig.getClass().getName()
                    + "\"");
        }
        m_executorFactory = factory;
        m_executorConfig = config;
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

    /** Get the chunking policy.
     * @return The chunking policy.
     * @see #getChunkValue()
     */
    public Chunking getChunking() {
        return m_chunking;
    }

    /** Get the value for the chunking. For {@link Chunking#ChunksOfSize} this
     * represents the number of rows per chunk and for {@link Chunking#NrChunks}
     * this is the number of chunks. For all other, the returned value has
     * no meaning.
     * @return The chunking value, interpretation depends on
     * {@link #getChunking()}
     */
    public int getChunkValue() {
        return m_chunkValue;
    }

    /** Sets chunking parameters. If chunking is {@link Chunking#ChunksOfSize}
     * or {@link Chunking#NrChunks} the <code>value</code> parameter is
     * interpreted as number of rows or number of chunks. It must not be &lt 1
     * in this case. In all other cases the value is ignored.
     * @param chunking The chunking to set (not null)
     * @param value The value (see above)
     * @throws InvalidSettingsException If arguments are invalid.
     */
    public void setChunking(final Chunking chunking, final int value)
        throws InvalidSettingsException {
        if (chunking == null) {
            throw new InvalidSettingsException("Chunking must not be null.");
        }
        int newValue;
        switch (chunking) {
        case ChunksOfSize:
        case NrChunks:
            if (value < 1) {
                throw new InvalidSettingsException(
                        "Invalid chunk value: " + value);
            }
            newValue = value;
            break;
        default:
            newValue = -1;
        }
        m_chunking = chunking;
        m_chunkValue = newValue;
    }

    /** The string to include in new row keys when more than one result is
     * returned, default is '_', e.g. Row1_1, Row1_2, etc.
     * @param value the multipleResultRowKeySeparator to set
     * @throws NullPointerException If argument is null
     * @throws IllegalArgumentException If argument is an empty string
     * @noreference This method is currently not intended to be referenced
     * by clients (pending API).
     */
    public void setMultipleResultRowKeySeparator(
            final String value) {
        if (value == null) {
            throw new NullPointerException("Argument must not be null.");
        }
        if (value.isEmpty()) {
            throw new IllegalArgumentException("Invalid (empty) separator");
        }
        m_multipleResultRowKeySeparator = value;
    }

    /** See {@link #setMultipleResultRowKeySeparator(String)} for details.
     * @return the multipleResultRowKeySeparator
     * @noreference This method is not intended to be referenced by clients
     * (pending API). */
    public String getMultipleResultRowKeySeparator() {
        return m_multipleResultRowKeySeparator;
    }

    /**
     * @param deleteTempFilePolicy the deleteTempFilePolicy to set
     * @throws InvalidSettingsException If argument is null
     */
    public void setDeleteTempFilePolicy(final DeleteTempFilePolicy
            deleteTempFilePolicy) throws InvalidSettingsException {
        if (deleteTempFilePolicy == null) {
            throw new InvalidSettingsException("Argument must not be null");
        }
        m_deleteTempFilePolicy = deleteTempFilePolicy;
    }

    /**
     * @return the deleteTempFilePolicy
     */
    public DeleteTempFilePolicy getDeleteTempFilePolicy() {
        return m_deleteTempFilePolicy;
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
     * @param env The current execution environment (providing flow vars, e.g.)
     * @return The command line arguments
     * @throws InvalidSettingsException If the get-method on the
     * {@link AbstractCommandlineSettings#
     *  getCommandlineArgs(ExttoolNodeEnvironment) get-method} fails
     */
    public String[] getCommandlineArgs(final ExttoolNodeEnvironment env)
        throws InvalidSettingsException {
        return m_commandlineSettings.getCommandlineArgs(env);
    }

    /** Get the current input configuration for an input port.
     * @param inPort Port of interest.
     * @return The input configuration. */
    public PathAndTypeConfigurationInput getInputConfig(final int inPort) {
        return m_inputConfigs[inPort];
    }

    /** Set the input configuration for a port.
     * @param inPort Port of interest.
     * @param config The new config.
     * @throws IllegalArgumentException If the config argument is null
     *         or invalid (no input).
     */
    public void setInputConfig(final int inPort,
            final PathAndTypeConfigurationInput config) {
        if (config == null) {
            throw new IllegalArgumentException("Illegal argument: " + config);
        }
        m_inputConfigs[inPort] = config;
    }

    /** Get the current output configuration for an output port.
     * @param outPort Port of interest.
     * @return The output configuration. */
    public PathAndTypeConfigurationOutput getOutputConfig(final int outPort) {
        return m_outputConfigs[outPort];
    }

    /** Set the output configuration for a port.
     * @param outPort Port of interest.
     * @param config The new config.
     * @throws IllegalArgumentException If the config argument is null
     *         or invalid (no output).
     */
    public void setOutputConfig(final int outPort,
            final PathAndTypeConfigurationOutput config) {
        if (config == null) {
            throw new IllegalArgumentException("Illegal argument: " + config);
        }
        m_outputConfigs[outPort] = config;
    }

    /** Create a {@link AbstractFileTypeWrite} instance for the given input
     * port. It will never return null, the returned object is fully
     * initialized, including the call to
     * {@link AbstractFileTypeWrite#prepare(AbstractFileTypeWriteConfig)}.
     * @param port The port of interest.
     * @return The initialized file type, never null.
     * @throws InvalidSettingsException If the settings are incomplete.
     */
    public AbstractFileTypeWrite createInputFileType(final int port)
        throws InvalidSettingsException {
        if (port < 0 || port >= m_customizer.getNrInputs()) {
            throw new IndexOutOfBoundsException("Invalid port index: " + port);
        }
        PathAndTypeConfigurationInput config = getInputConfig(port);
        if (config == null || config.getType() == null) {
            throw new InvalidSettingsException("No input file type associated"
                    + " with port " + port);
        }
        AbstractFileTypeFactory fac = config.getType();
        if (fac == null) {
            throw new InvalidSettingsException(
                    "No input type for port " + port);
        }
        AbstractFileTypeWriteConfig writeConfig = config.getWriteConfig();
        if (writeConfig == null) {
            throw new InvalidSettingsException("No settings associated with "
                    + "file type \"" + fac.getID() + "\" at port " + port);
        }

        AbstractFileTypeWrite instance = fac.createNewWriteInstance();
        instance.prepare(writeConfig);
        return instance;
    }

    /** Create a {@link AbstractFileTypeRead} instance for the given output
     * port. It will never return null, the returned object is fully
     * initialized, including the call to
     * {@link AbstractFileTypeRead#prepare(AbstractFileTypeReadConfig)}.
     * @param port The port of interest.
     * @return The initialized file type, never null.
     * @throws InvalidSettingsException If the settings are incomplete.
     */
    public AbstractFileTypeRead createOutputFileType(final int port)
    throws InvalidSettingsException {
        if (port < 0 || port >= m_customizer.getNrOutputs()) {
            throw new IndexOutOfBoundsException("Invalid port index: " + port);
        }
        PathAndTypeConfigurationOutput config = getOutputConfig(port);
        if (config == null || config.getType() == null) {
            throw new InvalidSettingsException("No output file type associated"
                    + " with port " + port);
        }
        AbstractFileTypeFactory fac = config.getType();
        if (fac == null) {
            throw new InvalidSettingsException(
                    "No input type for port " + port);
        }
        AbstractFileTypeReadConfig readConfig = config.getReadConfig();
        if (readConfig == null) {
            throw new InvalidSettingsException("No settings associated with "
                    + "file type \"" + fac.getID() + "\" at port " + port);
        }

        AbstractFileTypeRead instance = fac.createNewReadInstance();
        instance.prepare(readConfig);
        return instance;
    }

    /** Saves the current configuration, used in dialog. It allows custom
     * commandline settings to correct in/output configuration.
     * @param settings To save to. */
    public void saveSettingsToInDialog(final NodeSettingsWO settings) {
        m_commandlineSettings.correctSettingsForSave(this);
        saveSettingsTo(settings);
    }

    /** Saves the current configuration, used in model.
     * @param settings To save to. */
    public void saveSettingsTo(final NodeSettingsWO settings) {
        settings.addString("executor", m_executorFactory.getClass().getName());
        NodeSettingsWO execSets = settings.addNodeSettings("executorSettings");
        m_executorConfig.saveSettings(execSets);
        settings.addString("pathToExecutable", m_pathToExecutable);

        settings.addString("chunking", m_chunking.name());
        switch (m_chunking) {
        case ChunksOfSize:
        case NrChunks:
            settings.addInt("chunkingValue", m_chunkValue);
            break;
        default:
            // ignore, value has no meaning
        }

        settings.addString("deleteTempFilePolicy",
                m_deleteTempFilePolicy.name());
        NodeSettingsWO inSettings = settings.addNodeSettings("inports");
        final int inCount = m_customizer.getNrInputs();
        for (int i = 0; i < inCount; i++) {
            AbstractPathAndTypeConfiguration config = getInputConfig(i);
            NodeSettingsWO sub = inSettings.addNodeSettings("inport_" + i);
            if (config != null) {
                config.save(sub);
            }
        }

        NodeSettingsWO outSettings = settings.addNodeSettings("outports");
        final int outCount = m_customizer.getNrOutputs();
        for (int o = 0; o < outCount; o++) {
            NodeSettingsWO sub = outSettings.addNodeSettings("outport_" + o);
            AbstractPathAndTypeConfiguration config = getOutputConfig(o);
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
        String executorClassName = settings.getString("executor");
        AbstractExttoolExecutorFactory execFac =
            AbstractExttoolExecutorFactory.get(executorClassName);
        if (execFac == null) {
            throw new InvalidSettingsException("No executor defined");
        }
        NodeSettingsRO execSet = settings.getNodeSettings("executorSettings");
        AbstractExttoolExecutorConfig execConf = execFac.createConfig();
        execConf.loadSettingsInModel(execSet);
        m_executorFactory = execFac;
        m_executorConfig = execConf;

        m_pathToExecutable = settings.getString("pathToExecutable");

        String chunkingS = settings.getString("chunking");
        m_chunking = Chunking.read(chunkingS);
        switch (m_chunking) {
        case ChunksOfSize:
        case NrChunks:
            m_chunkValue = settings.getInt("chunkingValue");
            if (m_chunkValue < 1) {
                throw new InvalidSettingsException(
                        "Invalid chunking value: " + m_chunkValue);
            }
            break;
        default:
            m_chunkValue = -1;
        }

        String deleteTempFilePolicyS =
            settings.getString("deleteTempFilePolicy");
        m_deleteTempFilePolicy =
            DeleteTempFilePolicy.read(deleteTempFilePolicyS);

        /* Load inport related settings, including file path, type, ... */
        NodeSettingsRO inSettings = settings.getNodeSettings("inports");
        final int inCount = m_customizer.getNrInputs();
        for (int i = 0; i < inCount; i++) {
            NodeSettingsRO sub = inSettings.getNodeSettings("inport_" + i);
            PathAndTypeConfigurationInput config =
                new PathAndTypeConfigurationInput();
            config.load(sub);
            m_inputConfigs[i] = config;
        }

        /* Load outport related settings, including file path, type, ... */
        NodeSettingsRO outSettings = settings.getNodeSettings("outports");
        final int outCount = m_customizer.getNrOutputs();
        for (int o = 0; o < outCount; o++) {
            NodeSettingsRO sub = outSettings.getNodeSettings("outport_" + o);
            PathAndTypeConfigurationOutput config =
                new PathAndTypeConfigurationOutput();
            config.load(sub);
            m_outputConfigs[o] = config;
        }

        NodeSettingsRO commandSub = settings.getNodeSettings("commandLine");
        m_commandlineSettings.loadSettingsInModel(this, commandSub);
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
        AbstractExttoolExecutorFactory fallbackExecutor;
        if (m_executorFactory != null) {
            fallbackExecutor = m_executorFactory;
        } else {
            LOGGER.error("Could not load default executor");
            fallbackExecutor = new DefaultExttoolExecutorFactory();
        }

        String executorClassName = settings.getString("executor",
                fallbackExecutor.getClass().getName());

        AbstractExttoolExecutorFactory execFac;
        try {
            execFac = AbstractExttoolExecutorFactory.get(executorClassName);
        } catch (InvalidSettingsException ise) {
            execFac = fallbackExecutor;
        }
        AbstractExttoolExecutorConfig execConf = execFac.createConfig();
        NodeSettingsRO execSet;
        try {
            execSet = settings.getNodeSettings("executorSettings");
        } catch (InvalidSettingsException ise) {
            execSet = new NodeSettings("empty");
        }
        execConf.loadSettingsInDialog(execSet);
        m_executorFactory = execFac;
        m_executorConfig = execConf;

        m_pathToExecutable = settings.getString("pathToExecutable", "");

        String chunkingS = settings.getString("chunking",
                Chunking.EntireTable.name());
        try {
            m_chunking = Chunking.read(chunkingS);
        } catch (InvalidSettingsException ise) {
            m_chunking = Chunking.EntireTable;
        }
        switch (m_chunking) {
        case ChunksOfSize:
        case NrChunks:
            m_chunkValue = settings.getInt("chunkingValue", 20);
            if (m_chunkValue < 1) {
                m_chunkValue = 20;
            }
            break;
        default:
            m_chunkValue = -1;
        }

        DeleteTempFilePolicy defDeleteTempFilePolicy =
            m_customizer.getDefaultDeleteTempFilePolicy();

        String deleteTempFilePolicyS = settings.getString(
                "deleteTempFilePolicy", defDeleteTempFilePolicy.name());
        try {
            m_deleteTempFilePolicy =
                DeleteTempFilePolicy.read(deleteTempFilePolicyS);
        } catch (InvalidSettingsException ise) {
            m_deleteTempFilePolicy = defDeleteTempFilePolicy;
        }

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

            PathAndTypeConfigurationInput config =
                new PathAndTypeConfigurationInput();
            config.loadNoFail(sub, inSpecs[i]);
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

            PathAndTypeConfigurationOutput config =
                new PathAndTypeConfigurationOutput();
            config.loadNoFail(sub);
            m_outputConfigs[o] = config;
        }

        NodeSettingsRO commandSub;
        try {
            commandSub = settings.getNodeSettings("commandLine");
        } catch (InvalidSettingsException e) {
            commandSub = new NodeSettings("empty");
        }
        m_commandlineSettings.loadSettingsInDialog(this, commandSub, inSpecs);
    }


    /** Called from the node during its configure step. This method validates
     * the assigned executor and the input & output file types.
     * @param inSpecs The input specs to validate against.
     * @return The output spec if possible. Returning null will cause the
     * node to be configured without returning the output spec (analog to
     * configure method in NodeModel). This default implementation will delegate
     * to the commandline settings'
     * {@link AbstractCommandlineSettings#configure(DataTableSpec[]) configure}
     * method.
     * @throws InvalidSettingsException If any configuration is invalid.
     */
    public DataTableSpec[] configure(final DataTableSpec[] inSpecs)
        throws InvalidSettingsException {
        for (int i = 0; i < inSpecs.length; i++) {
            AbstractFileTypeWrite writer = createInputFileType(i);
            writer.validateInput(inSpecs[i]);
        }
        for (int o = 0; o < m_customizer.getNrOutputs(); o++) {
            createOutputFileType(o);
        }
        return m_commandlineSettings.configure(inSpecs);
    }


    /** Config object representing path, type and type settings to an in- or
     * outport. The path may be null to indicate that a temp file path should
     * be created. */
    private abstract static class AbstractPathAndTypeConfiguration {

        private String m_path;
        private AbstractFileTypeFactory m_type;

        /** @return fallback factory if settings are invalid. */
        static CSVFileTypeFactory getCSVTypeFactory() {
            try {
                return (CSVFileTypeFactory)AbstractFileTypeFactory.get(
                        CSVFileTypeFactory.class.getName());
            } catch (InvalidSettingsException e1) {
                LOGGER.error("Can't load CSV type factory", e1);
            }
            return new CSVFileTypeFactory();
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
        public AbstractFileTypeFactory getType() {
            return m_type;
        }
        /** Set type to use.
         * @param type the type to set
         */
        public void setType(final AbstractFileTypeFactory type) {
            m_type = type;
        }

        /** Saves the current configuration.
         * @param settings To save to. */
        protected void save(final NodeSettingsWO settings) {
            settings.addString("path", m_path);
            String typeS = m_type == null ? null : m_type.getID();
            settings.addString("type", typeS);
        }

        /** Loads the configuration.
         * @param settings To load from.
         * @throws InvalidSettingsException If settings are invalid. */
        protected void load(final NodeSettingsRO settings)
            throws InvalidSettingsException {
            m_path = settings.getString("path");
            String typeS = settings.getString("type");
            m_type = AbstractFileTypeFactory.get(typeS);
            NodeSettingsRO conf = settings.getNodeSettings("fileTypeConfig");
            NodeSettings confClone = new NodeSettings("fileTypeConfig");
            if (conf instanceof Config) {
                ((Config)conf).copyTo(confClone);
            } else {
                throw new IllegalStateException("Config objects of type "
                        + conf.getClass().getSimpleName()
                        + " not supported");
            }
        }

        /** Sets default settings, null path and csv file type factory, used
         * in dialog when settings are invalid.*/
        protected void initWithDefaults() {
            m_path = null;
            m_type = getCSVTypeFactory();
        }

    }

    /** Configuration for an output port, contains path, type and
     * type configuration. */
    public static final class PathAndTypeConfigurationOutput
        extends AbstractPathAndTypeConfiguration {

        private AbstractFileTypeReadConfig m_readConfig;

        /**
         * @return the readConfig
         */
        public AbstractFileTypeReadConfig getReadConfig() {
            return m_readConfig;
        }

        /**
         * @param readConfig the readConfig to set
         */
        public void setReadConfig(final AbstractFileTypeReadConfig readConfig) {
            m_readConfig = readConfig;
        }

        /** Saves the current configuration.
         * @param settings To save to. */
        @Override
        public void save(final NodeSettingsWO settings) {
            super.save(settings);
            if (m_readConfig != null) {
                NodeSettingsWO conf =
                    settings.addNodeSettings("fileTypeConfig");
                m_readConfig.saveSettings(conf);
            }
        }

        /** Loads the configuration.
         * @param settings To load from.
         * @throws InvalidSettingsException If settings are invalid. */
        @Override
        public void load(final NodeSettingsRO settings)
        throws InvalidSettingsException {
            super.load(settings);
            AbstractFileTypeFactory type = getType();
            if (type == null || !type.canRead()) {
                throw new InvalidSettingsException("No output file type");
            }
            NodeSettingsRO conf = settings.getNodeSettings("fileTypeConfig");
            AbstractFileTypeReadConfig config = type.createNewReadConfig();
            config.loadSettingsInModel(conf);
            m_readConfig = config;
        }

        /** Loads settings but does not throw exception (using defaults then).
         * @param settings To load from. */
        protected void loadNoFail(final NodeSettingsRO settings) {
            try {
                load(settings);
            } catch (InvalidSettingsException e) {
                initWithDefaults();
            }
        }

        /** {@inheritDoc} */
        @Override
        protected void initWithDefaults() {
            super.initWithDefaults();
            AbstractFileTypeFactory type = getType();
            if (type == null || !type.canRead()) {
                type = getCSVTypeFactory();
                setType(type);
            }
            m_readConfig = type.createNewReadConfig();
            m_readConfig.loadSettingsInDialog(new NodeSettings(""));
        }
    }

    /** Configuration for an input port, contains path, type and
     * type configuration. */
    public static final class PathAndTypeConfigurationInput
    extends AbstractPathAndTypeConfiguration {

        private AbstractFileTypeWriteConfig m_writeConfig;

        /**
         * @return the m_readConfig
         */
        public AbstractFileTypeWriteConfig getWriteConfig() {
            return m_writeConfig;
        }

        /**
         * @param writeConfig the m_readConfig to set
         */
        public void setWriteConfig(
                final AbstractFileTypeWriteConfig writeConfig) {
            m_writeConfig = writeConfig;
        }

        /** Saves the current configuration.
         * @param settings To save to. */
        @Override
        public void save(final NodeSettingsWO settings) {
            super.save(settings);
            if (m_writeConfig != null) {
                NodeSettingsWO conf =
                    settings.addNodeSettings("fileTypeConfig");
                m_writeConfig.saveSettings(conf);
            }
        }

        /** Loads the configuration.
         * @param settings To load from.
         * @throws InvalidSettingsException If settings are invalid. */
        @Override
        public void load(final NodeSettingsRO settings)
        throws InvalidSettingsException {
            super.load(settings);
            AbstractFileTypeFactory type = getType();
            if (type == null || !type.canWrite()) {
                throw new InvalidSettingsException("No input file type");
            }
            NodeSettingsRO conf = settings.getNodeSettings("fileTypeConfig");
            AbstractFileTypeWriteConfig config = type.createNewWriteConfig();
            config.loadSettingsInModel(conf);
            m_writeConfig = config;
        }

        /** Loads settings but does not throw exception (using defaults then).
         * @param settings To load from.
         * @param spec Input spec, used to init defaults. */
        protected void loadNoFail(final NodeSettingsRO settings,
                final DataTableSpec spec) {
            try {
                load(settings);
            } catch (InvalidSettingsException e) {
                initWithDefaults(spec);
            }
        }

        /** Sets default settings, null path and csv file type factory, used
         * in dialog when settings are invalid.
         * @param spec Input spec, used to init defaults. Ignore */
        protected void initWithDefaults(final DataTableSpec spec) {
            super.initWithDefaults();
            AbstractFileTypeFactory type = getType();
            if (type == null || !type.canWrite()) {
                type = getCSVTypeFactory();
                setType(type);
            }
            m_writeConfig = type.createNewWriteConfig();
            try {
                m_writeConfig.loadSettingsInDialog(
                        new NodeSettings("empty"), spec);
            } catch (NotConfigurableException e) {
                // ignore here.
            }
        }
    }

}
