-- -----------------------------------------------------
-- Table SP_APP
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS SP_APP (
        ID INTEGER NOT NULL AUTO_INCREMENT,
        TENANT_ID INTEGER NOT NULL,
        APP_NAME VARCHAR (255) NOT NULL ,
        VERSION VARCHAR (255) DEFAULT 'v2.0.0',
        USER_STORE VARCHAR (255) NOT NULL,
        USERNAME VARCHAR (255) NOT NULL ,
        DESCRIPTION VARCHAR (1024),
        ROLE_CLAIM VARCHAR (512),
        AUTH_TYPE VARCHAR (255) NOT NULL,
        PROVISIONING_USERSTORE_DOMAIN VARCHAR (512),
        IS_LOCAL_CLAIM_DIALECT CHAR(1) DEFAULT '1',
        IS_SEND_LOCAL_SUBJECT_ID CHAR(1) DEFAULT '0',
        IS_SEND_AUTH_LIST_OF_IDPS CHAR(1) DEFAULT '0',
        IS_USE_TENANT_DOMAIN_SUBJECT CHAR(1) DEFAULT '1',
        IS_USE_USER_DOMAIN_SUBJECT CHAR(1) DEFAULT '1',
        ENABLE_AUTHORIZATION CHAR(1) DEFAULT '0',
        SUBJECT_CLAIM_URI VARCHAR (512),
        IS_SAAS_APP CHAR(1) DEFAULT '0',
        IS_DUMB_MODE CHAR(1) DEFAULT '0',
        UUID CHAR(36),
        IMAGE_URL VARCHAR(1024),
        ACCESS_URL VARCHAR(1024),
        IS_DISCOVERABLE CHAR(1) DEFAULT '0',

        PRIMARY KEY (ID));

ALTER TABLE SP_APP ADD CONSTRAINT APPLICATION_NAME_CONSTRAINT UNIQUE(APP_NAME, TENANT_ID);
ALTER TABLE SP_APP ADD CONSTRAINT APPLICATION_UUID_CONSTRAINT UNIQUE(UUID);

-- Sample test data for SP_APP table
INSERT INTO SP_APP (TENANT_ID, APP_NAME, VERSION, USER_STORE, USERNAME, DESCRIPTION, AUTH_TYPE, UUID, IMAGE_URL, ACCESS_URL)
VALUES (1, 'test-application-1', 'v1.0.0', 'PRIMARY', 'admin@test.com', 'This is a test application for development purposes', 'oauth2', '550e8400-e29b-41d4-a716-446655440000', 'https://example.com/images/logo.png', 'https://example.com/app');
INSERT INTO SP_APP (TENANT_ID, APP_NAME, VERSION, USER_STORE, USERNAME, DESCRIPTION, AUTH_TYPE, UUID, IMAGE_URL, ACCESS_URL)
VALUES (2, 'test-application-2', 'v1.0.0', 'PRIMARY', 'admin@test.com', 'This is a test application for development purposes', 'oauth2', '550e8400-e29b-41d4-a716-446655440001', 'https://example.com/images/logo.png', 'https://example.com/app');
INSERT INTO SP_APP (TENANT_ID, APP_NAME, VERSION, USER_STORE, USERNAME, DESCRIPTION, AUTH_TYPE, UUID, IMAGE_URL, ACCESS_URL)
VALUES (2, 'test-application-3', 'v1.0.0', 'PRIMARY', 'admin@test.com', 'This is a test application for development purposes', 'oauth2', '550e8400-e29b-41d4-a716-446655440002', 'https://example.com/images/logo.png', 'https://example.com/app');

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
('73f6d9ca-62f4-4566-bab9-2a930ae51ba8', 'BRANDING_PREFERENCES', 'A resource type to keep the tenant branding preferences'),
('1fc809a0-dc0d-4cb2-82f3-58934d389236', 'CUSTOM_TEXT', 'A resource type to keep the tenant custom text preferences'),
('8469a176-3e6c-438a-ba01-71e9077072fa', 'APPLICATION_BRANDING_PREFERENCES', 'A resource type to keep the application branding preferences');

-- -----------------------------------------------------
-- Table IDN_CUSTOM_CONTENT_ORG
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS IDN_CUSTOM_CONTENT_ORG (
    ID INTEGER NOT NULL AUTO_INCREMENT,
    CONTENT BLOB NOT NULL,
    CONTENT_TYPE VARCHAR(50),
    TENANT_ID INTEGER NOT NULL,
    PRIMARY KEY (ID),
    CONSTRAINT ORG_CUSTOM_CONTENT_UNIQUE_CONSTRAINT UNIQUE (CONTENT_TYPE, TENANT_ID)
);

-- -----------------------------------------------------
-- Table IDN_CUSTOM_CONTENT_APP
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS IDN_CUSTOM_CONTENT_APP (
    ID INTEGER NOT NULL AUTO_INCREMENT,
    CONTENT BLOB NOT NULL,
    CONTENT_TYPE VARCHAR(50),
    APP_ID VARCHAR(255) NOT NULL,
    TENANT_ID INTEGER NOT NULL,
    PRIMARY KEY (ID),
    FOREIGN KEY (APP_ID) REFERENCES SP_APP(UUID) ON DELETE CASCADE,
    CONSTRAINT APP_CUSTOM_CONTENT_UNIQUE_CONSTRAINT UNIQUE (CONTENT_TYPE, APP_ID, TENANT_ID)
);
