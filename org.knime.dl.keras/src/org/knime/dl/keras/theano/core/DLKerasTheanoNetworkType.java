/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME GmbH, Konstanz, Germany
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
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ---------------------------------------------------------------------
 *
 */
package org.knime.dl.keras.theano.core;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.net.URL;

import org.knime.dl.core.DLAbstractNetworkSpecSerializer;
import org.knime.dl.core.DLInvalidSourceException;
import org.knime.dl.core.DLNetworkSerializer;
import org.knime.dl.core.DLNetworkSpecSerializer;
import org.knime.dl.keras.core.DLKerasAbstractNetworkType;
import org.knime.dl.python.core.DLPythonAbstractNetworkSerializer;

/**
 * @author Marcel Wiedenmann, KNIME, Konstanz, Germany
 * @author Christian Dietz, KNIME, Konstanz, Germany
 */
public final class DLKerasTheanoNetworkType
		extends DLKerasAbstractNetworkType<DLKerasTheanoNetwork, DLKerasTheanoNetworkSpec> {

	public static final DLKerasTheanoNetworkType INSTANCE = new DLKerasTheanoNetworkType();

	private static final long serialVersionUID = 1L;

	private static final String IDENTIFIER = "org.knime.dl.keras.theano.core.DLKerasTheanoNetworkType";

	private static final String NAME = "Keras (Theano)";

	public DLKerasTheanoNetworkType() {
		super(IDENTIFIER);
	}

	@Override
	public String getName() {
		return NAME;
	}

	@Override
	public String getPythonModuleName() {
		return "DLKerasTheanoNetworkType";
	}

	@Override
	public DLKerasTheanoNetworkLoader getLoader() {
		return new DLKerasTheanoNetworkLoader();
	}

	@Override
	public DLNetworkSerializer<DLKerasTheanoNetwork, DLKerasTheanoNetworkSpec> getNetworkSerializer() {
		return new DLPythonAbstractNetworkSerializer<DLKerasTheanoNetwork, DLKerasTheanoNetworkSpec>() {

			@Override
			public DLKerasTheanoNetwork deserialize(final InputStream in, final DLKerasTheanoNetworkSpec spec)
					throws IOException {
				final ObjectInputStream objIn = new ObjectInputStream(in);
				try {
					return new DLKerasTheanoNetwork(spec, (URL) objIn.readObject());
				} catch (final ClassNotFoundException e) {
					throw new IOException("Error during deserialization of a " + NAME + " network.");
				}
			}
		};
	}

	@Override
	public DLNetworkSpecSerializer<DLKerasTheanoNetworkSpec> getNetworkSpecSerializer() {
		return new DLAbstractNetworkSpecSerializer<DLKerasTheanoNetworkSpec>() {

			@Override
			public DLKerasTheanoNetworkSpec deserialize(final InputStream in) throws IOException {
				final ObjectInputStream objIn = new ObjectInputStream(in);
				try {
					return (DLKerasTheanoNetworkSpec) objIn.readObject();
				} catch (final ClassNotFoundException e) {
					throw new IOException("Error during deserialization of a " + NAME + " network spec.");
				}
			}
		};
	}

	@Override
	public DLKerasTheanoNetwork wrap(final DLKerasTheanoNetworkSpec spec, final URL source)
			throws DLInvalidSourceException, IllegalArgumentException {
		getLoader().validateSource(source);
		return new DLKerasTheanoNetwork(checkNotNull(spec), source);
	}
}
