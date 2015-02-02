package org.needle4j.db;

import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

import org.needle4j.configuration.NeedleConfiguration;
import org.needle4j.db.configuration.PersistenceConfigurationFactory;
import org.needle4j.db.operation.AbstractDBOperation;
import org.needle4j.db.operation.DBOperation;
import org.needle4j.db.operation.JdbcConfiguration;
import org.needle4j.reflection.ReflectionUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class DatabaseTestcaseConfiguration {

    private static final Logger LOG = LoggerFactory.getLogger(DatabaseTestcaseConfiguration.class);

    /**
     * The name of a JDBC driver key to use to connect to the database.
     */
    private static final String JDBC_DRIVER_KEY = "javax.persistence.jdbc.driver";

    /**
     * The JDBC connection url key to use to connect to the database.
     */
    private static final String JDBC_URL_KEY = "javax.persistence.jdbc.url";

    /**
     * The JDBC connection user name key.
     */
    private static final String JDBC_USER_KEY = "javax.persistence.jdbc.user";

    /**
     * The JDBC connection password key.
     */
    private static final String JDBC_PASSWORD_KEY = "javax.persistence.jdbc.password";

    /**
     * The override JDBC connection password key.
     *
     * Can be used to override the property name from which the JDBC password
     * will be retrieved. In Hibernate 4.3+ the value provided in
     * <code>javax.persistence.jdbc.password</code> is obscured.
     */
    private static final String OVERIDE_PASSWORD_KEY = "needle4j.jdbc.password";

    private final DBOperation dbOperation;

    private final PersistenceConfigurationFactory configuration;

    private NeedleConfiguration needleConfiguration;

    private DatabaseTestcaseConfiguration(final NeedleConfiguration needleConfiguration,
            final PersistenceConfigurationFactory configuratiorn) {
        this.needleConfiguration = needleConfiguration;
        this.configuration = configuratiorn;
        this.dbOperation = createDBOperation(lookupDBOperationClass(needleConfiguration.getDBOperationClassName()));
    }

    @Deprecated
    DatabaseTestcaseConfiguration(final NeedleConfiguration needleConfiguration, final Class<?>... clazzes) {
        this(needleConfiguration, new PersistenceConfigurationFactory(clazzes));
    }

    DatabaseTestcaseConfiguration(final NeedleConfiguration needleConfiguration) {
        this(needleConfiguration, new PersistenceConfigurationFactory(needleConfiguration.getPersistenceunitName()));
    }

    DatabaseTestcaseConfiguration(final NeedleConfiguration needleConfiguration, final String persistenceUnitName) {
        this(needleConfiguration, new PersistenceConfigurationFactory(persistenceUnitName));
    }

    EntityManager getEntityManager() {
        return configuration.getEntityManager();
    }

    EntityManagerFactory getEntityManagerFactory() {
        return configuration.getEntityManagerFactory();
    }

    DBOperation getDBOperation() {
        return dbOperation;
    }

    AbstractDBOperation createDBOperation(final Class<? extends AbstractDBOperation> dbOperationClass) {

        if (dbOperationClass != null) {
            try {
                return ReflectionUtil.createInstance(dbOperationClass, getJdbcComfiguration());
            } catch (final Exception e) {
                LOG.warn("could not create a new instance of configured db operation {}, {}", dbOperationClass,
                        e.getMessage());
                LOG.error(e.getMessage(), e);
            }
        } else {
            LOG.info("no db operation configured");
        }

        return null;
    }

    private JdbcConfiguration getJdbcComfiguration() throws Exception {
        if (needleConfiguration.getJdbcDriver() != null && needleConfiguration.getJdbcUrl() != null) {
            return new JdbcConfiguration(needleConfiguration.getJdbcUrl(), needleConfiguration.getJdbcDriver(),
                    needleConfiguration.getJdbcUser(), needleConfiguration.getJdbcPassword());
        }

        return getEntityManagerFactoryProperties();

    }

    private JdbcConfiguration getEntityManagerFactoryProperties() throws Exception {
        try {
            final Map<String, Object> properties = getEntityManagerFactory().getProperties();

            String password;
            if (properties.containsKey(OVERIDE_PASSWORD_KEY)) {
                password = (String) properties.get(OVERIDE_PASSWORD_KEY);
            } else {
                password = (String) properties.get(JDBC_PASSWORD_KEY);
            }

            return new JdbcConfiguration((String) properties.get(JDBC_URL_KEY),
                    (String) properties.get(JDBC_DRIVER_KEY), (String) properties.get(JDBC_USER_KEY),
                    password);
        } catch (final Exception e) {
            throw new Exception("error while loading jdbc configuration properties form EntityManagerFactory", e);
        }
    }

    static Class<? extends AbstractDBOperation> lookupDBOperationClass(final String dbOperation) {
        try {

            return ReflectionUtil.lookupClass(AbstractDBOperation.class, dbOperation);

        } catch (final Exception e) {
            LOG.warn("error while loading db operation class {}, {}", dbOperation, e.getMessage());
        }

        return null;
    }

}
