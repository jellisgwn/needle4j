package de.akquinet.jbosscc.needle.junit;

import org.junit.rules.MethodRule;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;

import de.akquinet.jbosscc.needle.db.DatabaseTestcase;

public class DatabaseRule extends DatabaseTestcase implements MethodRule {

	public DatabaseRule() {
		super();
	}

	public DatabaseRule(Class<?>[] clazzes) {
		super(clazzes);
	}

	public DatabaseRule(String puName) {
		super(puName);
	}

	@Override
	public final Statement apply(final Statement base, FrameworkMethod method, Object target) {
		return new Statement() {
			@Override
			public void evaluate() throws Throwable {
				try {
					base.evaluate();
				} finally {
					after();
				}
			}
		};
	}

}