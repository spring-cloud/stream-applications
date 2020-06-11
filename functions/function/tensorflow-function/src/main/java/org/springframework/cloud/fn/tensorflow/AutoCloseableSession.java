package org.springframework.cloud.fn.tensorflow;

import org.tensorflow.Graph;
import org.tensorflow.Session;
import org.tensorflow.op.Ops;

/**
 * @author Christian Tzolov
 */
class AutoCloseableSession implements AutoCloseable {

	private Session session;

	/**
	 * Note: don't call this method inside the constructor.
	 */
	protected void init() {
		Graph graph = this.doCreateGraph();
		this.doGraphDefinition(Ops.create(graph));
		this.session = new Session(graph);
	}

	protected Graph doCreateGraph() {
		return new Graph();
	}

	protected void doGraphDefinition(Ops tf) {
	}

	protected Session getSession() {
		if (this.session == null) {
			init();
		}
		return this.session;
	}

	@Override
	public void close() {
		this.doClose();
		if (this.session != null) {
			this.session.close();
		}
	}

	protected void doClose() {
	}
}
