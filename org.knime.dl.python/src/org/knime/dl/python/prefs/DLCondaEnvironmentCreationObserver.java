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
 *   Feb 26, 2019 (marcel): created
 */
package org.knime.dl.python.prefs;

import org.apache.commons.lang3.SystemUtils;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.util.Version;
import org.knime.python2.PythonVersion;
import org.knime.python2.config.AbstractCondaEnvironmentCreationObserver;
import org.knime.python2.envconfigs.CondaEnvironments;

/**
 * Initiates, observes, and {@link #cancelEnvironmentCreation(CondaEnvironmentCreationStatus) cancels} Conda environment
 * creation processes for a specific Conda installation and Python version. Allows clients to subscribe to changes in
 * the status of such creation processes.
 * <P>
 * Note: The current implementation only allows one active creation process at a time.
 *
 * @author Benjamin Wilhelm, KNIME GmbH, Konstanz, Germany
 * @author Marcel Wiedenmann, KNIME GmbH, Konstanz, Germany
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 */
public final class DLCondaEnvironmentCreationObserver extends AbstractCondaEnvironmentCreationObserver {

    private final DLPythonLibrarySelection m_library;

    /**
     * The created instance is {@link #getIsEnvironmentCreationEnabled() disabled by default}.
     *
     * @param condaDirectoryPath The Conda directory path. Changes in the model are reflected by this instance.
     * @param library the DL library the created environment should contain
     */
    public DLCondaEnvironmentCreationObserver(final SettingsModelString condaDirectoryPath,
        final DLPythonLibrarySelection library) {
        super(PythonVersion.PYTHON3, condaDirectoryPath);
        m_library = library;
    }

    /**
     * @return The default environment name for the next environment created by this instance. Returns an empty string
     *         in case calling Conda failed.<br>
     *         Note that this method makes no guarantees about the uniqueness of the returned name if invoked in
     *         parallel to an ongoing environment creation process.
     */
    public String getDefaultEnvironmentName() {
        if (m_library == DLPythonLibrarySelection.TF2) {
            return getDefaultEnvironmentName("tf2");
        }
        return getDefaultEnvironmentName("dl");
    }

    /** @return the DL library the created environment will contain */
    DLPythonLibrarySelection getLibrary() {
        return m_library;
    }

    /**
     * Initiates the a new Conda environment creation process. Only allowed if this instance is
     * {@link #getIsEnvironmentCreationEnabled() enabled}.
     *
     * @param environmentName The name of the environment. Must not already exist in the local Conda installation. May
     *            be {@code null} or empty in which case a unique default name is used.
     * @param status The status object that is will be notified about changes in the state of the initiated creation
     *            process. Can also be used to {@link #cancelEnvironmentCreation(CondaEnvironmentCreationStatus) cancel}
     *            the creation process. A new status object must be used for each new creation process.
     * @param gpu if the GPU configuration should be used
     */
    public void startEnvironmentCreation(final String environmentName, final CondaEnvironmentCreationStatus status,
        final boolean gpu) {
        final String libraryPrefix = m_library == DLPythonLibrarySelection.TF2 ? "tf2_" : "dl_";
        final String tag = libraryPrefix + (gpu && !SystemUtils.IS_OS_MAC ? "gpu" : "cpu");
        final String envFile = CondaEnvironments.getPathToCondaConfigFile(new Version(3, 6, 0), tag);
        startEnvironmentCreation(environmentName, envFile, null, status);
    }
}
