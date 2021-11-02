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
 */
package org.knime.dl.python.core;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.knime.dl.core.DLCancelable;
import org.knime.dl.core.DLCanceledExecutionException;
import org.knime.dl.core.DLInvalidEnvironmentException;
import org.knime.dl.core.DLUncheckedException;
import org.knime.dl.python.prefs.DLPythonPreferences;
import org.knime.dl.python.util.DLPythonUtils;
import org.knime.python.typeextension.PythonModuleExtensions;
import org.knime.python2.PythonCommand;
import org.knime.python2.extensions.serializationlibrary.SerializationOptions;
import org.knime.python2.extensions.serializationlibrary.interfaces.TableChunker;
import org.knime.python2.extensions.serializationlibrary.interfaces.TableCreator;
import org.knime.python2.extensions.serializationlibrary.interfaces.TableCreatorFactory;
import org.knime.python2.kernel.Python2KernelBackend;
import org.knime.python2.kernel.PythonCancelable;
import org.knime.python2.kernel.PythonCanceledExecutionException;
import org.knime.python2.kernel.PythonException;
import org.knime.python2.kernel.PythonKernel;
import org.knime.python2.kernel.PythonKernelCleanupException;
import org.knime.python2.kernel.PythonKernelOptions;
import org.knime.python2.kernel.PythonKernelQueue;

import com.google.common.base.Strings;

/**
 * @author Marcel Wiedenmann, KNIME GmbH, Konstanz, Germany
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 * @author Benjamin Wilhelm, KNIME GmbH, Konstanz, Germany
 */
public abstract class DLPythonAbstractContext implements DLPythonContext {

    private PythonKernel m_kernel;

    /**
     * Create a {@link DLPythonContext} without a Python kernel. The Python kernel will be created on demand by calling
     * {@link #getKernel()}.
     */
    public DLPythonAbstractContext() {
        // kernel will be created on demand
    }

    /**
     * Create a {@link DLPythonContext} with the given Python kernel.
     *
     * @param kernel the Python kernel which is used by this context. Closed on {@link #close()}.
     */
    public DLPythonAbstractContext(final PythonKernel kernel) {
        m_kernel = checkNotNull(kernel);
    }

    /**
     * @return the Python command for this {@link DLPythonContext}. Used for executing scripts.
     */
    protected abstract PythonCommand getPythonCommand();

    @Override
    public boolean isKernelOpen() {
        return m_kernel != null;
    }

    @Override
    public PythonKernel getKernel() throws DLInvalidEnvironmentException {
        if (m_kernel == null) {
            m_kernel = createKernel();
        }
        return m_kernel;
    }

    @SuppressWarnings("resource") // Kernel and back end are closed when this instance is closed.
    static Python2KernelBackend getLegacyKernelBackend(final PythonKernel kernel) throws DLInvalidEnvironmentException {
        if (!(kernel.getBackend() instanceof Python2KernelBackend)) {
            throw new DLInvalidEnvironmentException("The KNIME Deep Learning integration currently only supports the " +
                "legacy back end of the Python kernel. Please change your settings accordingly.");
        }
        return (Python2KernelBackend)kernel.getBackend();
    }

    @Override
    public String[] execute(final DLCancelable cancelable, final File script, final String... args) throws IOException {
        final ProcessBuilder pb = getPythonCommand().createProcessBuilder();
        final List<String> pbCommand = pb.command();
        pbCommand.add(script.getAbsolutePath());
        Collections.addAll(pbCommand, args);
        // Add all python modules to PYTHONPATH variable
        String existingPath = pb.environment().get("PYTHONPATH");
        existingPath = existingPath == null ? "" : existingPath;
        final String externalPythonPath = PythonModuleExtensions.getPythonPath();
        if ((externalPythonPath != null) && !externalPythonPath.isEmpty()) {
            if (existingPath.isEmpty()) {
                existingPath = externalPythonPath;
            } else {
                existingPath = existingPath + File.pathSeparator + externalPythonPath;
            }
        }
        existingPath = existingPath + File.pathSeparator;
        pb.environment().put("PYTHONPATH", existingPath);
        // TODO: Check if canceled.
        final Process p = pb.start();
        try (final OutputStream outStream = p.getOutputStream();
                final InputStream inStream = p.getInputStream();
                final InputStream errStream = p.getInputStream()) {
            final StringWriter stdout = new StringWriter();
            IOUtils.copy(inStream, stdout, StandardCharsets.UTF_8);

            final StringWriter stderr = new StringWriter();
            IOUtils.copy(errStream, stderr, StandardCharsets.UTF_8);

            return new String[]{stdout.toString(), stderr.toString()};
        } finally {
            p.destroyForcibly();
        }
    }

    @Override
    @SuppressWarnings("resource") // Kernel is closed in #close
    public String[] executeInKernel(final String code, final DLCancelable cancelable)
        throws DLCanceledExecutionException, DLInvalidEnvironmentException, IOException {
        try {
            return getKernel().execute(code, new DLCancelableWrappingPythonCancelable(cancelable));
        } catch (final Exception ex) {
            throwNarrowedPythonException(ex);
        }
        // This cannot happen.
        return null;
    }

    @Override
    @SuppressWarnings("resource") // Kernel is closed in #close
    public String[] executeAsyncInKernel(final String code, final DLCancelable cancelable)
        throws DLCanceledExecutionException, DLInvalidEnvironmentException, IOException {
        try {
            return getKernel().executeAsync(code, new DLCancelableWrappingPythonCancelable(cancelable));
        } catch (final Exception ex) {
            throwNarrowedPythonException(ex);
        }
        // This cannot happen.
        return null;
    }

    @Override
    @SuppressWarnings("resource") // Kernel is closed in #close
    public void putDataInKernel(final String name, final TableChunker tableChunker, final int rowsPerChunk,
        final DLCancelable cancelable) throws IOException, DLCanceledExecutionException, DLInvalidEnvironmentException {
        try {
            getLegacyKernelBackend(getKernel()).putData(name, tableChunker, rowsPerChunk,
                new DLCancelableWrappingPythonCancelable(cancelable));
        } catch (final Exception ex) {
            throwNarrowedPythonException(ex);
        }
    }

    @Override
    @SuppressWarnings("resource") // Kernel is closed in #close
    public TableCreator<?> getDataFromKernel(final String name, final TableCreatorFactory tcf,
        final DLCancelable cancelable) throws IOException, DLCanceledExecutionException, DLInvalidEnvironmentException {
        try {
            return getLegacyKernelBackend(getKernel()).getData(name, tcf,
                new DLCancelableWrappingPythonCancelable(cancelable));
        } catch (final Exception ex) {
            throwNarrowedPythonException(ex);
        }
        // This cannot happen.
        return null;
    }

    @Override
    public void setEnvironmentVariable(final String name, final String value, final DLCancelable cancelable)
        throws DLCanceledExecutionException, DLInvalidEnvironmentException, IOException {
        final String code = DLPythonUtils.createSourceCodeBuilder() //
            .a("import os") //
            .n("os.environ[").as(name).a("] = ").as(value)//
            .toString();
        executeInKernel(code, cancelable);
    }

    @Override
    public void close() {
        if (isKernelOpen()) {
            try {
                m_kernel.close();
            } catch (PythonKernelCleanupException e) {
                throw new DLUncheckedException(
                    "An exception occured while cleaning up Python. Cause: " + e.getMessage(), e);
            }
        }
    }

    /**
     * Create the Python kernel. Overwrite this method to take control of the kernel creation. The default
     * implementation takes the next {@link PythonKernel} from the {@link PythonKernelQueue} with the
     * {@link PythonCommand} from {@link #getPythonCommand()}.
     *
     * @return a new Python kernel for this {@link DLPythonContext} using the appropriate preferences
     * @throws DLInvalidEnvironmentException if the Python kernel could not be started
     */
    protected PythonKernel createKernel() throws DLInvalidEnvironmentException {
        try {
            final PythonKernelOptions options = getKernelOptions();
            final PythonCommand command = getPythonCommand();
            return PythonKernelQueue.getNextKernel(command, Collections.emptySet(), Collections.emptySet(), options,
                PythonCancelable.NOT_CANCELABLE);
        } catch (final IOException e) {
            final String msg = !Strings.isNullOrEmpty(e.getMessage())
                ? "An error occurred while trying to launch Python: " + e.getMessage()
                : "An unknown error occurred while trying to launch Python. See log for details.";
            throw new DLInvalidEnvironmentException(msg, e);
        } catch (final PythonCanceledExecutionException ex) {
            // Cannot happen. We pass a non-cancelable above.
            throw new IllegalStateException("Implementation error.", ex);
        }
    }

    private static PythonKernelOptions getKernelOptions() {
        final SerializationOptions serializerOptions =
            new SerializationOptions().forSerializerId(DLPythonPreferences.getSerializerPreference());
        return new PythonKernelOptions().forSerializationOptions(serializerOptions);
    }

    private static void throwNarrowedPythonException(final Exception e)
        throws DLCanceledExecutionException, DLInvalidEnvironmentException, IOException {
        if (e instanceof DLCanceledExecutionException) {
            throw (DLCanceledExecutionException)e;
        } else if (e instanceof DLInvalidEnvironmentException) {
            throw (DLInvalidEnvironmentException)e;
        } else if (e instanceof IOException) {
            throw (IOException)e;
        } else if (e instanceof PythonException) {
            if (e instanceof PythonCanceledExecutionException) {
                throw new DLCanceledExecutionException(e.getMessage());
            }
            throw new DLInvalidEnvironmentException("An error occurred while interacting with Python."
                + (e.getMessage() != null ? " Cause: " + e.getMessage() : ""), e);
        } else {
            throw new IOException(e);
        }
    }

    private static final class DLCancelableWrappingPythonCancelable implements PythonCancelable {

        private final DLCancelable m_delegate;

        public DLCancelableWrappingPythonCancelable(final DLCancelable cancelable) {
            m_delegate = cancelable;
        }

        @Override
        public void checkCanceled() throws PythonCanceledExecutionException {
            try {
                m_delegate.checkCanceled();
            } catch (final DLCanceledExecutionException e) {
                throw new PythonCanceledExecutionException(e.getMessage());
            }
        }
    }
}
