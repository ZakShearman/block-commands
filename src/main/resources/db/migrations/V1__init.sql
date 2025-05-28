CREATE TABLE block_command
(
    id         TEXT    NOT NULL PRIMARY KEY,
    exec_type  TEXT    NOT NULL,
    click_type TEXT    NOT NULL,
    command    TEXT    NOT NULL,

    world_id TEXT    NOT NULL,
    pos_x      INTEGER NOT NULL,
    pos_y      INTEGER NOT NULL,
    pos_z      INTEGER NOT NULL,

    FOREIGN KEY (pos_x, pos_y, pos_z, world_id) REFERENCES block (pos_x, pos_y, pos_z, world_id) ON DELETE CASCADE
);

-- Note this is not unique because multiple commands can be set for the same block
CREATE INDEX idx_block_command_world_pos ON block_command (pos_x, pos_y, pos_z, world_id);

CREATE TABLE block
(
    pos_x    INTEGER NOT NULL,
    pos_y    INTEGER NOT NULL,
    pos_z    INTEGER NOT NULL,
    world_id TEXT    NOT NULL,

    setting_cancel_interact BOOLEAN NOT NULL DEFAULT false,

    PRIMARY KEY (pos_x, pos_y, pos_z, world_id)
);

