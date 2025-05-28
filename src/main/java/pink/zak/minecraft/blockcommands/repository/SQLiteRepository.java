package pink.zak.minecraft.blockcommands.repository;

import org.flywaydb.core.Flyway;
import org.jetbrains.annotations.NotNull;
import pink.zak.minecraft.blockcommands.model.BlockCommand;
import pink.zak.minecraft.blockcommands.model.BlockSettings;
import pink.zak.minecraft.blockcommands.model.CustomBlock;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

public class SQLiteRepository implements Repository {
    private static final Path DB_PATH = Path.of("plugins/BlockCommands/db.sqlite");
    private static final String SQLITE_URL = "jdbc:sqlite:" + DB_PATH.toAbsolutePath().toString();

    private final @NotNull Connection connection;

    public SQLiteRepository(ClassLoader classLoader, @NotNull Logger logger) {
        this.createDbFileIfNotExists();
        this.migrate(classLoader);

        try {
            Properties properties = new Properties();
            properties.setProperty("PRAGMA foreign_keys", "ON");
            this.connection = DriverManager.getConnection(SQLITE_URL, properties);
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
    }

    private void createDbFileIfNotExists() {
        if (!Files.exists(DB_PATH)) {
            try {
                Files.createDirectories(DB_PATH.getParent());
                Files.createFile(DB_PATH);
            } catch (IOException e) {
                throw new RuntimeException("Failed to create database file", e);
            }
        }
    }

    private void migrate(ClassLoader classLoader) {
        Flyway flyway = Flyway.configure(classLoader)
                .dataSource(SQLITE_URL, null, null)
                .locations("classpath:db/migrations")
                .failOnMissingLocations(true)
                .validateMigrationNaming(true)
                .load();

        flyway.migrate();
    }

    @Override
    public void shutdown() {
        try {
            //noinspection ConstantValue since it may be called when the plugin is in a broken state
            if (this.connection == null || this.connection.isClosed()) return;
            this.connection.close();
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public CompletableFuture<Void> CreateDefaultCustomBlockIfNotExists(@NotNull CustomBlock block) {
        return CompletableFuture.runAsync(() -> {
            final String query = """
            INSERT INTO block (pos_x, pos_y, pos_z, world_id) VALUES (?, ?, ?, ?)
            ON CONFLICT(pos_x, pos_y, pos_z, world_id) DO NOTHING""";
            try (var preparedStatement = this.connection.prepareStatement(query)) {
                preparedStatement.setInt(1, block.x());
                preparedStatement.setInt(2, block.y());
                preparedStatement.setInt(3, block.z());
                preparedStatement.setString(4, block.worldId().toString());
                preparedStatement.executeUpdate();
            } catch (SQLException ex) {
                throw new RuntimeException("Failed to create custom block", ex);
            }
        });
    }

    @Override
    public CompletableFuture<Void> CreateBlockCommand(@NotNull BlockCommand blockCommand) {
        return CompletableFuture.runAsync(() -> {
            final String query = "INSERT INTO block_command (id, exec_type, click_type, command, world_id, pos_x, pos_y, pos_z) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
            try (var preparedStatement = this.connection.prepareStatement(query)) {
                preparedStatement.setString(1, blockCommand.id().toString());
                preparedStatement.setString(2, blockCommand.execType().toString());
                preparedStatement.setString(3, blockCommand.clickType().toString());
                preparedStatement.setString(4, blockCommand.command());
                preparedStatement.setString(5, blockCommand.worldId().toString());
                preparedStatement.setInt(6, blockCommand.x());
                preparedStatement.setInt(7, blockCommand.y());
                preparedStatement.setInt(8, blockCommand.z());
                preparedStatement.executeUpdate();
            } catch (SQLException ex) {
                throw new RuntimeException("Failed to create block command", ex);
            }
        });
    }

    // Helper record for grouping commands by block location
    private record BlockLocationKey(int x, int y, int z, UUID worldId) {
    }

    @Override
    public CompletableFuture<List<CustomBlock>> ListAllCustomBlocks(int page, int pageSize) {
        if (page < 1 || pageSize < 1) {
            throw new IllegalArgumentException("Page and page size must be greater than 0");
        }

        return CompletableFuture.supplyAsync(() -> {
            final String query = """
                    SELECT bc.id, bc.pos_x, bc.pos_y, bc.pos_z, bc.world_id, bc.command, bc.exec_type, bc.click_type,
                           b.setting_cancel_interact
                    FROM block_command bc
                    INNER JOIN (
                        SELECT DISTINCT pos_x, pos_y, pos_z, world_id
                        FROM block_command
                        ORDER BY world_id, pos_x, pos_y, pos_z
                        LIMIT ? OFFSET ?
                    ) AS paginated
                    ON bc.pos_x = paginated.pos_x
                    AND bc.pos_y = paginated.pos_y
                    AND bc.pos_z = paginated.pos_z
                    AND bc.world_id = paginated.world_id
                    INNER JOIN block b
                    ON bc.pos_x = b.pos_x
                    AND bc.pos_y = b.pos_y
                    AND bc.pos_z = b.pos_z
                    AND bc.world_id = b.world_id
                    ORDER BY bc.world_id, bc.pos_x, bc.pos_y, bc.pos_z""";

            Map<BlockLocationKey, CustomBlock> groupedCommands = new LinkedHashMap<>();

            try (var preparedStatement = this.connection.prepareStatement(query)) {
                preparedStatement.setInt(1, pageSize);
                preparedStatement.setInt(2, (page - 1) * pageSize); // Offset is 0-indexed

                try (var resultSet = preparedStatement.executeQuery()) {
                    while (resultSet.next()) {
                        int x = resultSet.getInt("pos_x");
                        int y = resultSet.getInt("pos_y");
                        int z = resultSet.getInt("pos_z");
                        UUID worldId = UUID.fromString(resultSet.getString("world_id"));
                        boolean settingCancelInteract = resultSet.getBoolean("setting_cancel_interact");

                        BlockLocationKey key = new BlockLocationKey(x, y, z, worldId);

                        BlockCommand blockCommand = new BlockCommand(
                                UUID.fromString(resultSet.getString("id")),
                                x, y, z, worldId,
                                BlockCommand.ExecType.valueOf(resultSet.getString("exec_type")),
                                BlockCommand.BlockClickType.valueOf(resultSet.getString("click_type")),
                                resultSet.getString("command")
                        );

                        groupedCommands.computeIfAbsent(key, k -> new CustomBlock(x, y, z, worldId,
                                new BlockSettings(settingCancelInteract), new ArrayList<>())
                        ).commands().add(blockCommand);
                    }
                }
            } catch (SQLException ex) {
                throw new RuntimeException("Failed to list block commands", ex);
            }

            return new ArrayList<>(groupedCommands.values());
        });
    }

    @Override
    public CompletableFuture<Void> DeleteCustomBlock(int x, int y, int z, UUID worldId) {
        return CompletableFuture.runAsync(() -> {
            final String query = "DELETE FROM block WHERE pos_x = ? AND pos_y = ? AND pos_z = ? AND world_id = ?";
            try (var preparedStatement = this.connection.prepareStatement(query)) {
                preparedStatement.setInt(1, x);
                preparedStatement.setInt(2, y);
                preparedStatement.setInt(3, z);
                preparedStatement.setString(4, worldId.toString());
                preparedStatement.executeUpdate();
            } catch (SQLException ex) {
                throw new RuntimeException("Failed to delete custom block", ex);
            }
        });
    }

    @Override
    public CompletableFuture<CustomBlock> GetCustomBlock(int x, int y, int z, UUID worldId) {
        return CompletableFuture.supplyAsync(() -> {
            final String query = """
                    SELECT bc.id, bc.exec_type, bc.click_type, bc.command, b.setting_cancel_interact
                    FROM block_command bc
                    INNER JOIN block b ON bc.pos_x = b.pos_x AND bc.pos_y = b.pos_y AND bc.pos_z = b.pos_z AND bc.world_id = b.world_id
                    WHERE bc.pos_x = ? AND bc.pos_y = ? AND bc.pos_z = ? AND bc.world_id = ?""";

            try (var preparedStatement = this.connection.prepareStatement(query)) {
                preparedStatement.setInt(1, x);
                preparedStatement.setInt(2, y);
                preparedStatement.setInt(3, z);
                preparedStatement.setString(4, worldId.toString());

                try (var resultSet = preparedStatement.executeQuery()) {
                    List<BlockCommand> commands = new ArrayList<>();
                    boolean settingCancelInteract = false;

                    while (resultSet.next()) {
                        if (resultSet.isFirst()) {
                            settingCancelInteract = resultSet.getBoolean("setting_cancel_interact");
                        }

                        commands.add(new BlockCommand(
                                UUID.fromString(resultSet.getString("id")),
                                x, y, z, worldId,
                                BlockCommand.ExecType.valueOf(resultSet.getString("exec_type")),
                                BlockCommand.BlockClickType.valueOf(resultSet.getString("click_type")),
                                resultSet.getString("command")
                        ));
                    }

                    return new CustomBlock(x, y, z, worldId, new BlockSettings(settingCancelInteract), commands);
                }
            } catch (SQLException ex) {
                throw new RuntimeException("Failed to get custom block", ex);
            }
        });
    }

    @Override
    public CompletableFuture<List<BlockCommand>> ListBlockCommands(int x, int y, int z, UUID worldId) {
        return CompletableFuture.supplyAsync(() -> {
            final String query = "SELECT * FROM block_command WHERE pos_x = ? AND pos_y = ? AND pos_z = ? AND world_id = ?";
            List<BlockCommand> commands = new ArrayList<>();

            try (var preparedStatement = this.connection.prepareStatement(query)) {
                preparedStatement.setInt(1, x);
                preparedStatement.setInt(2, y);
                preparedStatement.setInt(3, z);
                preparedStatement.setString(4, worldId.toString());

                try (var resultSet = preparedStatement.executeQuery()) {
                    while (resultSet.next()) {
                        commands.add(new BlockCommand(
                                UUID.fromString(resultSet.getString("id")),
                                x, y, z, worldId,
                                BlockCommand.ExecType.valueOf(resultSet.getString("exec_type")),
                                BlockCommand.BlockClickType.valueOf(resultSet.getString("click_type")),
                                resultSet.getString("command")
                        ));
                    }
                }
            } catch (SQLException ex) {
                throw new RuntimeException("Failed to list block commands", ex);
            }

            return commands;
        });
    }

    @Override
    public CompletableFuture<Void> DeleteBlockCommand(UUID id) {
        return CompletableFuture.runAsync(() -> {
            final String query = "DELETE FROM block_command WHERE id = ?";
            try (var preparedStatement = this.connection.prepareStatement(query)) {
                preparedStatement.setString(1, id.toString());
                preparedStatement.executeUpdate();
            } catch (SQLException ex) {
                throw new RuntimeException("Failed to delete block command", ex);
            }
        });
    }

    @Override
    public CompletableFuture<Void> DeleteBlockCommands(int x, int y, int z, String worldId) {
        return CompletableFuture.runAsync(() -> {
            final String query = "DELETE FROM block_command WHERE pos_x = ? AND pos_y = ? AND pos_z = ? AND world_id = ?";
            try (var preparedStatement = this.connection.prepareStatement(query)) {
                preparedStatement.setInt(1, x);
                preparedStatement.setInt(2, y);
                preparedStatement.setInt(3, z);
                preparedStatement.setString(4, worldId);
                preparedStatement.executeUpdate();
            } catch (SQLException ex) {
                throw new RuntimeException("Failed to delete block commands", ex);
            }
        });
    }

    @Override
    public CompletableFuture<Void> UpdateBlockSettingCancelInteract(int x, int y, int z, String worldId, boolean cancelInteract) {
        return null;
    }
}
