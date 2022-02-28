-- -----------------------------------------------------
-- Table IDN_CONFIG_TYPE
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS IDN_CONFIG_TYPE (
    ID          VARCHAR(255)  NOT NULL,
    NAME        VARCHAR(255)  NOT NULL,
    DESCRIPTION VARCHAR(1023) NULL,
    PRIMARY KEY (ID),
    CONSTRAINT TYPE_NAME_CONSTRAINT UNIQUE (NAME)
    );

-- -----------------------------------------------------
-- Table IDN_CONFIG_RESOURCE
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS IDN_CONFIG_RESOURCE (
    ID            VARCHAR(255) NOT NULL,
    TENANT_ID     INT          NOT NULL,
    NAME          VARCHAR(255) NOT NULL,
    CREATED_TIME  TIMESTAMP    NOT NULL,
    LAST_MODIFIED TIMESTAMP    NOT NULL,
    HAS_FILE      BOOLEAN(1)   NOT NULL,
    HAS_ATTRIBUTE BOOLEAN(1)   NOT NULL,
    TYPE_ID       VARCHAR(255) NOT NULL,
    UNIQUE (NAME, TENANT_ID, TYPE_ID),
    PRIMARY KEY (ID)
    );

ALTER TABLE IDN_CONFIG_RESOURCE
    ADD CONSTRAINT TYPE_ID_FOREIGN_CONSTRAINT FOREIGN KEY (TYPE_ID) REFERENCES IDN_CONFIG_TYPE (ID) ON DELETE CASCADE ON UPDATE CASCADE;

-- -----------------------------------------------------
-- Table IDN_CONFIG_ATTRIBUTE
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS IDN_CONFIG_ATTRIBUTE (
    ID         VARCHAR(255)  NOT NULL,
    RESOURCE_ID  VARCHAR(255)  NOT NULL,
    ATTR_KEY   VARCHAR(1023) NOT NULL,
    ATTR_VALUE VARCHAR(1023) NULL,
    PRIMARY KEY (ID),
    UNIQUE (RESOURCE_ID, ATTR_KEY, ATTR_VALUE)
    );
ALTER TABLE IDN_CONFIG_ATTRIBUTE
    ADD CONSTRAINT RESOURCE_ID_ATTRIBUTE_FOREIGN_CONSTRAINT FOREIGN KEY (RESOURCE_ID) REFERENCES IDN_CONFIG_RESOURCE (ID) ON DELETE CASCADE ON UPDATE CASCADE;

-- -----------------------------------------------------
-- Table IDN_CONFIG_FILE
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS IDN_CONFIG_FILE (
    ID        VARCHAR(255) NOT NULL,
    VALUE     BLOB         NULL,
    NAME        VARCHAR (255) NULL,
    RESOURCE_ID VARCHAR(255) NOT NULL,
    PRIMARY KEY (ID)
    );
ALTER TABLE IDN_CONFIG_FILE
    ADD CONSTRAINT RESOURCE_ID_FILE_FOREIGN_CONSTRAINT FOREIGN KEY (RESOURCE_ID) REFERENCES IDN_CONFIG_RESOURCE (ID) ON DELETE CASCADE ON UPDATE CASCADE;

-- -----------------------------------------------------
-- Add Resource Types to the IDN_CONFIG_TYPE table.
-- -----------------------------------------------------

INSERT INTO IDN_CONFIG_TYPE (ID, NAME, DESCRIPTION) VALUES
('9ab0ef95-13e9-4ed5-afaf-d29bed62f7bd', 'IDP_TEMPLATE', 'Template type to uniquely identify IDP templates'),
('3c4ac3d0-5903-4e3d-aaca-38df65b33bfd', 'APPLICATION_TEMPLATE', 'Template type to uniquely identify Application templates'),
('8ec6dbf1-218a-49bf-bc34-0d2db52d151c', 'CORS_CONFIGURATION', 'A resource type to keep the tenant CORS configurations'),
('669b99ca-cdb0-44a6-8cae-babed3b585df', 'Publisher', 'A resource type to keep the event publisher configurations'),
('73f6d9ca-62f4-4566-bab9-2a930ae51ba8', 'BRANDING_PREFERENCES', 'A resource type to keep the tenant branding preferences');