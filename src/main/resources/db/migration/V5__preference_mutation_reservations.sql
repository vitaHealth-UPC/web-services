-- Technical coordination survives command rollback without retaining unsaved preference values.
CREATE TABLE user_preference_locks (
    id varchar(36) NOT NULL PRIMARY KEY,
    user_id varchar(64) NOT NULL,
    CONSTRAINT uk_preferences_mutation_user UNIQUE (user_id)
);
