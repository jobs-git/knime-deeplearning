/*
 * ------------------------------------------------------------------------
 *
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
 * ---------------------------------------------------------------------
 *
 * History
 *   Jun 12, 2020 (benjamin): created
 */
package org.knime.dl.python.prefs;

import org.knime.python2.Conda.CondaEnvironmentSpec;
import org.knime.python2.PythonVersion;
import org.knime.python2.config.CondaDirectoryConfig;
import org.knime.python2.config.CondaEnvironmentConfig;
import org.knime.python2.config.PythonConfigStorage;
import org.knime.python2.prefs.PythonPreferences;

/**
 * @author Benjain Wilhelm, KNIME GmbH, Konstanz, Germany
 * @author Marcel Wiedenmann, KNIME GmbH, Konstanz, Germany
 */
final class DLCondaEnvironmentsConfig implements DLPythonEnvironmentsConfig {

    private static final String CFG_KEY_KERAS_CONDA_ENV_NAME_DIR = "kerasCondaEnvironmentDirectoryPath";

    private static final String CFG_KEY_TF2_CONDA_ENV_NAME_DIR = "tf2CondaEnvironmentDirectoryPath";

    /** Only used for legacy support. See {@link CondaEnvironmentConfig#loadConfigFrom(PythonConfigStorage)}. */
    private static final String LEGACY_CFG_KEY_KERAS_CONDA_ENV_NAME = "condaEnvironmentName";

    private final CondaDirectoryConfig m_condaDirectoryConfig =
        new CondaDirectoryConfig(PythonPreferences.getCondaInstallationPath());

    private final CondaEnvironmentConfig m_kerasEnvironmentConfig = new CondaEnvironmentConfig(PythonVersion.PYTHON3,
        CFG_KEY_KERAS_CONDA_ENV_NAME_DIR, LEGACY_CFG_KEY_KERAS_CONDA_ENV_NAME, m_condaDirectoryConfig);

    private final CondaEnvironmentConfig m_tf2EnvironmentConfig = new CondaEnvironmentConfig(PythonVersion.PYTHON3,
        CFG_KEY_TF2_CONDA_ENV_NAME_DIR, LEGACY_CFG_KEY_KERAS_CONDA_ENV_NAME, m_condaDirectoryConfig);

    /**
     * @return The configuration for the installation directory of the Conda installation.
     */
    public CondaDirectoryConfig getCondaDirectoryConfig() {
        return m_condaDirectoryConfig;
    }

    @Override
    public CondaEnvironmentConfig getKerasConfig() {
        return m_kerasEnvironmentConfig;
    }

    @Override
    public CondaEnvironmentConfig getTF2Config() {
        return m_tf2EnvironmentConfig;
    }

    @Override
    public void saveDefaultsTo(final PythonConfigStorage storage) {
        m_condaDirectoryConfig.saveDefaultsTo(storage);
        m_kerasEnvironmentConfig.saveDefaultsTo(storage);
        m_tf2EnvironmentConfig.saveDefaultsTo(storage);
    }

    @Override
    public void saveConfigTo(final PythonConfigStorage storage) {
        m_condaDirectoryConfig.saveConfigTo(storage);
        m_kerasEnvironmentConfig.saveConfigTo(storage);
        m_tf2EnvironmentConfig.saveConfigTo(storage);
    }

    @Override
    public void loadConfigFrom(final PythonConfigStorage storage) {
        m_condaDirectoryConfig.loadConfigFrom(storage);
        m_kerasEnvironmentConfig.loadConfigFrom(storage);
        m_tf2EnvironmentConfig.loadConfigFrom(storage);
    }

    /**
     * Load the default configuration
     */
    void loadDefaults() {
        m_condaDirectoryConfig.getCondaDirectoryPath()
            .setStringValue(CondaDirectoryConfig.getDefaultInstallationDirectory());
        m_condaDirectoryConfig.getCondaInstallationError().setStringValue("");
        m_condaDirectoryConfig.getCondaInstallationInfo().setStringValue("");
        loadDefaults(m_kerasEnvironmentConfig);
        loadDefaults(m_tf2EnvironmentConfig);
    }

    private static void loadDefaults(final CondaEnvironmentConfig config) {
        final CondaEnvironmentSpec placeholderEnvironment = CondaEnvironmentConfig.PLACEHOLDER_ENV;
        config.getEnvironmentDirectory().setStringValue(placeholderEnvironment.getDirectoryPath());
        config.getAvailableEnvironments().setValue(new CondaEnvironmentSpec[]{placeholderEnvironment});
    }
}
