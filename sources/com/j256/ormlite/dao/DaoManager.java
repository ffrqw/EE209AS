package com.j256.ormlite.dao;

import com.j256.ormlite.logger.Logger;
import com.j256.ormlite.logger.LoggerFactory;
import com.j256.ormlite.misc.SqlExceptionUtil;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.DatabaseTable;
import com.j256.ormlite.table.DatabaseTableConfig;
import java.lang.reflect.Constructor;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class DaoManager {
    private static Map<ClassConnectionSource, Dao<?, ?>> classMap = null;
    private static Map<Class<?>, DatabaseTableConfig<?>> configMap = null;
    private static Logger logger = LoggerFactory.getLogger(DaoManager.class);
    private static Map<TableConfigConnectionSource, Dao<?, ?>> tableConfigMap = null;

    private static class ClassConnectionSource {
        Class<?> clazz;
        ConnectionSource connectionSource;

        public ClassConnectionSource(ConnectionSource connectionSource, Class<?> clazz) {
            this.connectionSource = connectionSource;
            this.clazz = clazz;
        }

        public int hashCode() {
            return ((this.clazz.hashCode() + 31) * 31) + this.connectionSource.hashCode();
        }

        public boolean equals(Object obj) {
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            ClassConnectionSource other = (ClassConnectionSource) obj;
            if (this.clazz.equals(other.clazz) && this.connectionSource.equals(other.connectionSource)) {
                return true;
            }
            return false;
        }
    }

    private static class TableConfigConnectionSource {
        ConnectionSource connectionSource;
        DatabaseTableConfig<?> tableConfig;

        public TableConfigConnectionSource(ConnectionSource connectionSource, DatabaseTableConfig<?> tableConfig) {
            this.connectionSource = connectionSource;
            this.tableConfig = tableConfig;
        }

        public int hashCode() {
            return ((this.tableConfig.hashCode() + 31) * 31) + this.connectionSource.hashCode();
        }

        public boolean equals(Object obj) {
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            TableConfigConnectionSource other = (TableConfigConnectionSource) obj;
            if (this.tableConfig.equals(other.tableConfig) && this.connectionSource.equals(other.connectionSource)) {
                return true;
            }
            return false;
        }
    }

    public static synchronized <D extends Dao<T, ?>, T> D createDao(ConnectionSource connectionSource, Class<T> clazz) throws SQLException {
        Dao<?, ?> dao;
        synchronized (DaoManager.class) {
            if (connectionSource == null) {
                throw new IllegalArgumentException("connectionSource argument cannot be null");
            }
            D dao2 = lookupDao(new ClassConnectionSource(connectionSource, clazz));
            if (dao2 != null) {
                D castDao = dao2;
                dao = dao2;
            } else {
                Dao<?, ?> dao3 = (Dao) createDaoFromConfig(connectionSource, clazz);
                Dao<?, ?> dao4;
                if (dao3 != null) {
                    dao4 = dao3;
                    dao = dao3;
                } else {
                    DatabaseTable databaseTable = (DatabaseTable) clazz.getAnnotation(DatabaseTable.class);
                    if (databaseTable == null || databaseTable.daoClass() == Void.class || databaseTable.daoClass() == BaseDaoImpl.class) {
                        Dao<T, ?> daoTmp;
                        DatabaseTableConfig config = connectionSource.getDatabaseType().extractDatabaseTableConfig(connectionSource, clazz);
                        if (config == null) {
                            daoTmp = BaseDaoImpl.createDao(connectionSource, (Class) clazz);
                        } else {
                            daoTmp = BaseDaoImpl.createDao(connectionSource, config);
                        }
                        dao3 = daoTmp;
                        logger.debug("created dao for class {} with reflection", (Object) clazz);
                    } else {
                        Class<?> daoClass = databaseTable.daoClass();
                        Object[] arguments = new Object[]{connectionSource, clazz};
                        Constructor<?> daoConstructor = findConstructor(daoClass, arguments);
                        if (daoConstructor == null) {
                            arguments = new Object[]{connectionSource};
                            daoConstructor = findConstructor(daoClass, arguments);
                            if (daoConstructor == null) {
                                throw new SQLException("Could not find public constructor with ConnectionSource and optional Class parameters " + daoClass + ".  Missing static on class?");
                            }
                        }
                        try {
                            dao3 = (Dao) daoConstructor.newInstance(arguments);
                            logger.debug("created dao for class {} from constructor", (Object) clazz);
                        } catch (Exception e) {
                            throw SqlExceptionUtil.create("Could not call the constructor in class " + daoClass, e);
                        }
                    }
                    registerDao(connectionSource, dao3);
                    dao4 = dao3;
                    dao = dao3;
                }
            }
        }
        return dao;
    }

    public static synchronized <D extends Dao<T, ?>, T> D lookupDao(ConnectionSource connectionSource, Class<T> clazz) {
        D dao;
        synchronized (DaoManager.class) {
            if (connectionSource == null) {
                throw new IllegalArgumentException("connectionSource argument cannot be null");
            }
            dao = lookupDao(new ClassConnectionSource(connectionSource, clazz));
            D castDao = dao;
        }
        return dao;
    }

    public static synchronized <D extends Dao<T, ?>, T> D createDao(ConnectionSource connectionSource, DatabaseTableConfig<T> tableConfig) throws SQLException {
        D doCreateDao;
        synchronized (DaoManager.class) {
            if (connectionSource == null) {
                throw new IllegalArgumentException("connectionSource argument cannot be null");
            }
            doCreateDao = doCreateDao(connectionSource, tableConfig);
        }
        return doCreateDao;
    }

    public static synchronized <D extends Dao<T, ?>, T> D lookupDao(ConnectionSource connectionSource, DatabaseTableConfig<T> tableConfig) {
        D dao;
        synchronized (DaoManager.class) {
            if (connectionSource == null) {
                throw new IllegalArgumentException("connectionSource argument cannot be null");
            }
            dao = lookupDao(new TableConfigConnectionSource(connectionSource, tableConfig));
            if (dao == null) {
                dao = null;
            } else {
                D castDao = dao;
            }
        }
        return dao;
    }

    public static synchronized void registerDao(ConnectionSource connectionSource, Dao<?, ?> dao) {
        synchronized (DaoManager.class) {
            if (connectionSource == null) {
                throw new IllegalArgumentException("connectionSource argument cannot be null");
            }
            addDaoToClassMap(new ClassConnectionSource(connectionSource, dao.getDataClass()), dao);
        }
    }

    public static synchronized void unregisterDao(ConnectionSource connectionSource, Dao<?, ?> dao) {
        synchronized (DaoManager.class) {
            if (connectionSource == null) {
                throw new IllegalArgumentException("connectionSource argument cannot be null");
            }
            removeDaoToClassMap(new ClassConnectionSource(connectionSource, dao.getDataClass()), dao);
        }
    }

    public static synchronized void registerDaoWithTableConfig(ConnectionSource connectionSource, Dao<?, ?> dao) {
        synchronized (DaoManager.class) {
            if (connectionSource == null) {
                throw new IllegalArgumentException("connectionSource argument cannot be null");
            }
            if (dao instanceof BaseDaoImpl) {
                DatabaseTableConfig<?> tableConfig = ((BaseDaoImpl) dao).getTableConfig();
                if (tableConfig != null) {
                    addDaoToTableMap(new TableConfigConnectionSource(connectionSource, tableConfig), dao);
                }
            }
            addDaoToClassMap(new ClassConnectionSource(connectionSource, dao.getDataClass()), dao);
        }
    }

    public static synchronized void clearCache() {
        synchronized (DaoManager.class) {
            if (configMap != null) {
                configMap.clear();
                configMap = null;
            }
            clearDaoCache();
        }
    }

    public static synchronized void clearDaoCache() {
        synchronized (DaoManager.class) {
            if (classMap != null) {
                classMap.clear();
                classMap = null;
            }
            if (tableConfigMap != null) {
                tableConfigMap.clear();
                tableConfigMap = null;
            }
        }
    }

    public static synchronized void addCachedDatabaseConfigs(Collection<DatabaseTableConfig<?>> configs) {
        synchronized (DaoManager.class) {
            Map<Class<?>, DatabaseTableConfig<?>> newMap;
            if (configMap == null) {
                newMap = new HashMap();
            } else {
                newMap = new HashMap(configMap);
            }
            for (DatabaseTableConfig<?> config : configs) {
                newMap.put(config.getDataClass(), config);
                logger.info("Loaded configuration for {}", config.getDataClass());
            }
            configMap = newMap;
        }
    }

    private static void addDaoToClassMap(ClassConnectionSource key, Dao<?, ?> dao) {
        if (classMap == null) {
            classMap = new HashMap();
        }
        classMap.put(key, dao);
    }

    private static void removeDaoToClassMap(ClassConnectionSource key, Dao<?, ?> dao) {
        if (classMap != null) {
            classMap.remove(key);
        }
    }

    private static void addDaoToTableMap(TableConfigConnectionSource key, Dao<?, ?> dao) {
        if (tableConfigMap == null) {
            tableConfigMap = new HashMap();
        }
        tableConfigMap.put(key, dao);
    }

    private static <T> Dao<?, ?> lookupDao(ClassConnectionSource key) {
        if (classMap == null) {
            classMap = new HashMap();
        }
        Dao<?, ?> dao = (Dao) classMap.get(key);
        if (dao == null) {
            return null;
        }
        return dao;
    }

    private static <T> Dao<?, ?> lookupDao(TableConfigConnectionSource key) {
        if (tableConfigMap == null) {
            tableConfigMap = new HashMap();
        }
        Dao<?, ?> dao = (Dao) tableConfigMap.get(key);
        if (dao == null) {
            return null;
        }
        return dao;
    }

    private static Constructor<?> findConstructor(Class<?> daoClass, Object[] params) {
        for (Constructor<?> constructor : daoClass.getConstructors()) {
            Class<?>[] paramsTypes = constructor.getParameterTypes();
            if (paramsTypes.length == params.length) {
                boolean match = true;
                for (int i = 0; i < paramsTypes.length; i++) {
                    if (!paramsTypes[i].isAssignableFrom(params[i].getClass())) {
                        match = false;
                        break;
                    }
                }
                if (match) {
                    return constructor;
                }
            }
        }
        return null;
    }

    private static <D, T> D createDaoFromConfig(ConnectionSource connectionSource, Class<T> clazz) throws SQLException {
        if (configMap == null) {
            return null;
        }
        DatabaseTableConfig<T> config = (DatabaseTableConfig) configMap.get(clazz);
        if (config == null) {
            return null;
        }
        D configedDao = doCreateDao(connectionSource, config);
        D castDao = configedDao;
        return configedDao;
    }

    private static <D extends Dao<T, ?>, T> D doCreateDao(ConnectionSource connectionSource, DatabaseTableConfig<T> tableConfig) throws SQLException {
        TableConfigConnectionSource tableKey = new TableConfigConnectionSource(connectionSource, tableConfig);
        D dao = lookupDao(tableKey);
        if (dao != null) {
            D castDao = dao;
            return dao;
        }
        Object dataClass = tableConfig.getDataClass();
        ClassConnectionSource classKey = new ClassConnectionSource(connectionSource, dataClass);
        Dao<?, ?> dao2 = lookupDao(classKey);
        if (dao2 != null) {
            addDaoToTableMap(tableKey, dao2);
            Dao<?, ?> dao3 = dao2;
            return dao2;
        }
        DatabaseTable databaseTable = (DatabaseTable) tableConfig.getDataClass().getAnnotation(DatabaseTable.class);
        if (databaseTable == null || databaseTable.daoClass() == Void.class || databaseTable.daoClass() == BaseDaoImpl.class) {
            dao2 = BaseDaoImpl.createDao(connectionSource, (DatabaseTableConfig) tableConfig);
        } else {
            Class<?> daoClass = databaseTable.daoClass();
            Object[] arguments = new Object[]{connectionSource, tableConfig};
            Constructor<?> constructor = findConstructor(daoClass, arguments);
            if (constructor == null) {
                throw new SQLException("Could not find public constructor with ConnectionSource, DatabaseTableConfig parameters in class " + daoClass);
            }
            try {
                dao2 = (Dao) constructor.newInstance(arguments);
            } catch (Exception e) {
                throw SqlExceptionUtil.create("Could not call the constructor in class " + daoClass, e);
            }
        }
        addDaoToTableMap(tableKey, dao2);
        logger.debug("created dao for class {} from table config", dataClass);
        if (lookupDao(classKey) == null) {
            addDaoToClassMap(classKey, dao2);
        }
        dao3 = dao2;
        return dao2;
    }
}
