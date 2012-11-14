package liquibase.diff.output.changelog.core;

import liquibase.change.Change;
import liquibase.change.ColumnConfig;
import liquibase.change.ConstraintsConfig;
import liquibase.change.core.AddColumnChange;
import liquibase.database.Database;
import liquibase.datatype.DataTypeFactory;
import liquibase.diff.output.DiffOutputControl;
import liquibase.diff.output.changelog.ChangeGeneratorChain;
import liquibase.diff.output.changelog.MissingObjectChangeGenerator;
import liquibase.structure.DatabaseObject;
import liquibase.structure.core.Column;
import liquibase.structure.core.Table;

public class MissingColumnChangeGenerator implements MissingObjectChangeGenerator {

    public int getPriority(Class<? extends DatabaseObject> objectType, Database database) {
        if (Column.class.isAssignableFrom(objectType)) {
            return PRIORITY_DEFAULT;
        }
        return PRIORITY_NONE;

    }

    public Change[] fixMissing(DatabaseObject missingObject, DiffOutputControl control, Database referenceDatabase, Database comparisionDatabase, ChangeGeneratorChain chain) {
        Column column = (Column) missingObject;
//        if (!shouldModifyColumn(column)) {
//            continue;
//        }

        AddColumnChange change = new AddColumnChange();
        change.setTableName(column.getRelation().getName());
        if (control.isIncludeCatalog()) {
            change.setCatalogName(column.getRelation().getSchema().getCatalog().getName());
        }
        if (control.isIncludeSchema()) {
            change.setSchemaName(column.getRelation().getSchema().getName());
        }

        ColumnConfig columnConfig = new ColumnConfig();
        columnConfig.setName(column.getName());

        String dataType = column.getType().toString();

        columnConfig.setType(dataType);

        Object defaultValue = column.getDefaultValue();
        if (defaultValue != null) {
            String defaultValueString = null;
            try {
                defaultValueString = DataTypeFactory.getInstance().from(column.getType()).objectToSql(defaultValue, referenceDatabase);
            } catch (NullPointerException e) {
                throw e;
            }
            if (defaultValueString != null) {
                defaultValueString = defaultValueString.replaceFirst("'",
                        "").replaceAll("'$", "");
            }
            columnConfig.setDefaultValue(defaultValueString);
        }

        if (column.getRemarks() != null) {
            columnConfig.setRemarks(column.getRemarks());
        }
        ConstraintsConfig constraintsConfig = columnConfig.getConstraints();
        if (column.isNullable() != null && !column.isNullable()) {
            if (constraintsConfig == null) {
                constraintsConfig = new ConstraintsConfig();
            }
            constraintsConfig.setNullable(false);
        }
        if (column.isUnique()) {
            if (constraintsConfig == null) {
                constraintsConfig = new ConstraintsConfig();
            }
            constraintsConfig.setUnique(true);
        }
        if (constraintsConfig != null) {
            columnConfig.setConstraints(constraintsConfig);
        }

        change.addColumn(columnConfig);

        return new Change[] { change };
    }
}