package io.mopl.seeder;

import com.github.f4b6a3.uuid.UuidCreator;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class StagingSeeder {
  private static final int DEFAULT_USERS = 50000;
  private static final int DEFAULT_CONTENTS = 100000;
  private static final int DEFAULT_TAGS = 300;
  private static final int DEFAULT_TAGS_PER_CONTENT = 3;
  private static final int DEFAULT_PLAYLISTS = 50000;
  private static final int DEFAULT_CONTENTS_PER_PLAYLIST = 20;
  private static final int DEFAULT_SUBS_PER_PLAYLIST = 10;
  private static final int DEFAULT_CONVERSATIONS = 20000;
  private static final int DEFAULT_DM_PER_CONVERSATION = 20;
  private static final int DEFAULT_NOTIFICATIONS = 500000;
  private static final int DEFAULT_BATCH_SIZE = 1000;

  public static void main(String[] args) throws Exception {
    String url = getRequiredEnv("DB_URL");
    String user = getRequiredEnv("DB_USER");
    String password = getRequiredEnv("DB_PASSWORD");

    int scalePercent = getIntEnv("SEED_SCALE_PERCENT", 100);
    int users = getIntEnv("SEED_USERS", scaled(DEFAULT_USERS, scalePercent));
    int contents = getIntEnv("SEED_CONTENTS", scaled(DEFAULT_CONTENTS, scalePercent));
    int tags = getIntEnv("SEED_TAGS", scaled(DEFAULT_TAGS, scalePercent));
    int tagsPerContent =
        getIntEnv("SEED_TAGS_PER_CONTENT", scaled(DEFAULT_TAGS_PER_CONTENT, scalePercent));
    int playlists = getIntEnv("SEED_PLAYLISTS", scaled(DEFAULT_PLAYLISTS, scalePercent));
    int contentsPerPlaylist =
        getIntEnv(
            "SEED_CONTENTS_PER_PLAYLIST", scaled(DEFAULT_CONTENTS_PER_PLAYLIST, scalePercent));
    int subsPerPlaylist =
        getIntEnv("SEED_SUBS_PER_PLAYLIST", scaled(DEFAULT_SUBS_PER_PLAYLIST, scalePercent));
    int conversations =
        getIntEnv("SEED_CONVERSATIONS", scaled(DEFAULT_CONVERSATIONS, scalePercent));
    int dmPerConversation =
        getIntEnv("SEED_DM_PER_CONVERSATION", scaled(DEFAULT_DM_PER_CONVERSATION, scalePercent));
    int notifications =
        getIntEnv("SEED_NOTIFICATIONS", scaled(DEFAULT_NOTIFICATIONS, scalePercent));
    int batchSize = getIntEnv("SEED_BATCH_SIZE", DEFAULT_BATCH_SIZE);
    boolean truncate = getBooleanEnv("SEED_TRUNCATE", false);
    boolean resume = getBooleanEnv("SEED_RESUME", false);
    String seedFrom = getStringEnv("SEED_FROM", "");
    int testUsers = getIntEnv("SEED_TEST_USERS", 0);
    String testUserPassword = getStringEnv("SEED_TEST_USER_PASSWORD", "1234");
    String testUserPrefix = getStringEnv("SEED_TEST_USER_PREFIX", "loadtest-");
    String testUserDomain = getStringEnv("SEED_TEST_USER_DOMAIN", "mopl.io");

    Random random = new Random(42);

    try (Connection connection = DriverManager.getConnection(url, user, password)) {
      connection.setAutoCommit(false);

      logConfig(
          scalePercent,
          users,
          contents,
          tags,
          tagsPerContent,
          playlists,
          contentsPerPlaylist,
          subsPerPlaylist,
          conversations,
          dmPerConversation,
          notifications,
          testUsers,
          batchSize);

      if (truncate) {
        truncateAll(connection);
      }

      if (!seedFrom.isBlank()) {
        if (!seedFrom.equalsIgnoreCase("notifications")) {
          throw new IllegalArgumentException(
              "SEED_FROM currently supports only 'notifications' for resume.");
        }
        List<String> userIds = loadIds(connection, "users");
        int target = notifications;
        int remaining = target;
        if (resume) {
          int existing = countTable(connection, "notifications");
          remaining = Math.max(0, target - existing);
          System.out.printf(
              ">> notifications: existing=%d target=%d remaining=%d%n",
              existing, target, remaining);
        }
        if (remaining > 0) {
          seedNotifications(connection, userIds, remaining, batchSize, random);
        } else {
          System.out.println("<< notifications: already at target, skipping");
        }
        connection.commit();
        return;
      }

      List<String> userIds = seedUsers(connection, users, batchSize, random);
      if (testUsers > 0) {
        List<String> testUserIds =
            seedTestUsers(
                connection,
                testUsers,
                testUserPrefix,
                testUserDomain,
                testUserPassword,
                batchSize,
                random);
        userIds.addAll(testUserIds);
      }
      List<String> tagIds = seedTags(connection, tags, batchSize);
      List<String> contentIds = seedContents(connection, contents, batchSize, random);
      seedContentTags(connection, contentIds, tagIds, tagsPerContent, batchSize, random);
      List<String> playlistIds = seedPlaylists(connection, playlists, userIds, batchSize, random);
      seedPlaylistContents(
          connection, playlistIds, contentIds, contentsPerPlaylist, batchSize, random);
      seedPlaylistSubscriptions(
          connection, playlistIds, userIds, subsPerPlaylist, batchSize, random);
      List<String> conversationIds = seedConversations(connection, conversations, batchSize);
      List<String[]> conversationPairs =
          seedConversationParticipants(connection, conversationIds, userIds, batchSize, random);
      seedDirectMessages(connection, conversationPairs, dmPerConversation, batchSize, random);
      seedNotifications(connection, userIds, notifications, batchSize, random);

      connection.commit();
    }
  }

  private static List<String> seedUsers(
      Connection connection, int count, int batchSize, Random random) throws Exception {
    String sql =
        "INSERT INTO users (id, email, name, password_hash, auth_provider, provider_user_id, role, locked, profile_image_key, created_at, updated_at) "
            + "VALUES (?, ?, ?, ?, 'LOCAL', NULL, 'USER', 0, NULL, ?, ?)";
    List<String> ids = new ArrayList<>(count);
    logStart("users", count);
    try (PreparedStatement statement = connection.prepareStatement(sql)) {
      int batch = 0;
      for (int i = 0; i < count; i++) {
        String id = UuidCreator.getTimeOrderedEpoch().toString();
        ids.add(id);
        statement.setString(1, id);
        statement.setString(2, "seed-" + i + "@mopl.io");
        statement.setString(3, "SeedUser" + i);
        statement.setString(4, "seeded_password_hash");
        Timestamp createdAt = randomPastTimestamp(random);
        statement.setTimestamp(5, createdAt);
        statement.setTimestamp(6, createdAt);
        statement.addBatch();
        if (++batch % batchSize == 0) {
          statement.executeBatch();
          connectionCommit(statement);
          logProgress("users", batch, count);
        }
      }
      statement.executeBatch();
    }
    logDone("users");
    return ids;
  }

  private static List<String> seedTestUsers(
      Connection connection,
      int count,
      String prefix,
      String domain,
      String password,
      int batchSize,
      Random random)
      throws Exception {
    if (count <= 0) {
      return new ArrayList<>();
    }
    String sql =
        "INSERT IGNORE INTO users (id, email, name, password_hash, auth_provider, provider_user_id, role, locked, profile_image_key, created_at, updated_at) "
            + "VALUES (?, ?, ?, ?, 'LOCAL', NULL, 'USER', 0, NULL, ?, ?)";
    String passwordHash = new BCryptPasswordEncoder().encode(password);
    List<String> ids = new ArrayList<>(count);
    logStart("test_users", count);
    try (PreparedStatement statement = connection.prepareStatement(sql)) {
      int batch = 0;
      for (int i = 0; i < count; i++) {
        String email = String.format("%s%05d@%s", prefix, i, domain);
        String id = UuidCreator.getTimeOrderedEpoch().toString();
        Timestamp createdAt = randomPastTimestamp(random);

        statement.setString(1, id);
        statement.setString(2, email);
        statement.setString(3, "LoadTest" + i);
        statement.setString(4, passwordHash);
        statement.setTimestamp(5, createdAt);
        statement.setTimestamp(6, createdAt);
        statement.addBatch();

        if (++batch % batchSize == 0) {
          statement.executeBatch();
          connectionCommit(statement);
          logProgress("test_users", batch, count);
        }
      }
      statement.executeBatch();
      connectionCommit(statement);
    }

    try (PreparedStatement statement =
        connection.prepareStatement("SELECT id FROM users WHERE email = ?")) {
      for (int i = 0; i < count; i++) {
        String email = String.format("%s%05d@%s", prefix, i, domain);
        statement.setString(1, email);
        try (var rs = statement.executeQuery()) {
          if (rs.next()) {
            ids.add(rs.getString(1));
          }
        }
      }
    }

    logDone("test_users");
    return ids;
  }

  private static List<String> seedTags(Connection connection, int count, int batchSize)
      throws Exception {
    String sql = "INSERT INTO tags (id, name) VALUES (?, ?)";
    List<String> ids = new ArrayList<>(count);
    logStart("tags", count);
    try (PreparedStatement statement = connection.prepareStatement(sql)) {
      int batch = 0;
      for (int i = 0; i < count; i++) {
        String id = UuidCreator.getTimeOrderedEpoch().toString();
        ids.add(id);
        statement.setString(1, id);
        statement.setString(2, "tag-" + i);
        statement.addBatch();
        if (++batch % batchSize == 0) {
          statement.executeBatch();
          logProgress("tags", batch, count);
        }
      }
      statement.executeBatch();
    }
    logDone("tags");
    return ids;
  }

  private static List<String> seedContents(
      Connection connection, int count, int batchSize, Random random) throws Exception {
    String sql =
        "INSERT INTO contents (id, type, external_id, title, description, thumbnail_image_key, average_rating, review_count, watcher_count, created_at, updated_at) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    List<String> ids = new ArrayList<>(count);
    String[] types = {"MOVIE", "TV_SERIES", "SPORT"};
    logStart("contents", count);
    try (PreparedStatement statement = connection.prepareStatement(sql)) {
      int batch = 0;
      for (int i = 0; i < count; i++) {
        String id = UuidCreator.getTimeOrderedEpoch().toString();
        ids.add(id);
        statement.setString(1, id);
        statement.setString(2, types[i % types.length]);
        statement.setString(3, "seed-" + i);
        statement.setString(4, "Seed Content " + i);
        statement.setString(5, "Seed content description " + i);
        statement.setString(6, "seed/thumbnail/" + i + ".jpg");
        statement.setDouble(7, random.nextDouble() * 5.0);
        statement.setInt(8, random.nextInt(1000));
        statement.setLong(9, Math.abs(random.nextLong()) % 100000);
        Timestamp createdAt = randomPastTimestamp(random);
        statement.setTimestamp(10, createdAt);
        statement.setTimestamp(11, createdAt);
        statement.addBatch();
        if (++batch % batchSize == 0) {
          statement.executeBatch();
          connectionCommit(statement);
          logProgress("contents", batch, count);
        }
      }
      statement.executeBatch();
    }
    logDone("contents");
    return ids;
  }

  private static void seedContentTags(
      Connection connection,
      List<String> contentIds,
      List<String> tagIds,
      int tagsPerContent,
      int batchSize,
      Random random)
      throws Exception {
    String sql = "INSERT INTO content_tags (content_id, tag_id) VALUES (?, ?)";
    int tagCount = tagIds.size();
    int perContent = Math.min(tagsPerContent, tagCount);
    int total = contentIds.size() * Math.max(1, perContent);
    logStart("content_tags", total);
    try (PreparedStatement statement = connection.prepareStatement(sql)) {
      int batch = 0;
      for (String contentId : contentIds) {
        HashSet<Integer> seen = new HashSet<>(perContent * 2);
        for (int i = 0; i < perContent; i++) {
          int index = nextUniqueIndex(random, tagCount, seen);
          String tagId = tagIds.get(index);
          statement.setString(1, contentId);
          statement.setString(2, tagId);
          statement.addBatch();
          if (++batch % batchSize == 0) {
            statement.executeBatch();
            connectionCommit(statement);
            logProgress("content_tags", batch, total);
          }
        }
      }
      statement.executeBatch();
      connectionCommit(statement);
    }
    logDone("content_tags");
  }

  private static List<String> seedPlaylists(
      Connection connection, int count, List<String> userIds, int batchSize, Random random)
      throws Exception {
    String sql =
        "INSERT INTO playlists (id, owner_id, title, description, subscriber_count, created_at, updated_at) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?)";
    List<String> ids = new ArrayList<>(count);
    int userCount = userIds.size();
    logStart("playlists", count);
    try (PreparedStatement statement = connection.prepareStatement(sql)) {
      int batch = 0;
      for (int i = 0; i < count; i++) {
        String id = UuidCreator.getTimeOrderedEpoch().toString();
        ids.add(id);
        statement.setString(1, id);
        statement.setString(2, userIds.get(random.nextInt(userCount)));
        statement.setString(3, "Seed Playlist " + i);
        statement.setString(4, "Seed playlist description " + i);
        statement.setLong(5, random.nextInt(1000));
        Timestamp createdAt = randomPastTimestamp(random);
        statement.setTimestamp(6, createdAt);
        statement.setTimestamp(7, createdAt);
        statement.addBatch();
        if (++batch % batchSize == 0) {
          statement.executeBatch();
          connectionCommit(statement);
          logProgress("playlists", batch, count);
        }
      }
      statement.executeBatch();
    }
    logDone("playlists");
    return ids;
  }

  private static void seedPlaylistContents(
      Connection connection,
      List<String> playlistIds,
      List<String> contentIds,
      int contentsPerPlaylist,
      int batchSize,
      Random random)
      throws Exception {
    String sql =
        "INSERT INTO playlist_contents (playlist_id, content_id, added_at) VALUES (?, ?, ?)";
    int contentCount = contentIds.size();
    int perPlaylist = Math.min(contentsPerPlaylist, contentCount);
    int total = playlistIds.size() * Math.max(1, perPlaylist);
    logStart("playlist_contents", total);
    try (PreparedStatement statement = connection.prepareStatement(sql)) {
      int batch = 0;
      for (String playlistId : playlistIds) {
        HashSet<Integer> seen = new HashSet<>(perPlaylist * 2);
        for (int i = 0; i < perPlaylist; i++) {
          int index = nextUniqueIndex(random, contentCount, seen);
          String contentId = contentIds.get(index);
          statement.setString(1, playlistId);
          statement.setString(2, contentId);
          statement.setTimestamp(3, randomPastTimestamp(random));
          statement.addBatch();
          if (++batch % batchSize == 0) {
            statement.executeBatch();
            connectionCommit(statement);
            logProgress("playlist_contents", batch, total);
          }
        }
      }
      statement.executeBatch();
      connectionCommit(statement);
    }
    logDone("playlist_contents");
  }

  private static void seedPlaylistSubscriptions(
      Connection connection,
      List<String> playlistIds,
      List<String> userIds,
      int subsPerPlaylist,
      int batchSize,
      Random random)
      throws Exception {
    String sql =
        "INSERT INTO playlist_subscriptions (playlist_id, user_id, created_at) VALUES (?, ?, ?)";
    int userCount = userIds.size();
    int perPlaylist = Math.min(subsPerPlaylist, userCount);
    int total = playlistIds.size() * Math.max(1, perPlaylist);
    logStart("playlist_subscriptions", total);
    try (PreparedStatement statement = connection.prepareStatement(sql)) {
      int batch = 0;
      for (String playlistId : playlistIds) {
        HashSet<Integer> seen = new HashSet<>(perPlaylist * 2);
        for (int i = 0; i < perPlaylist; i++) {
          int index = nextUniqueIndex(random, userCount, seen);
          String userId = userIds.get(index);
          statement.setString(1, playlistId);
          statement.setString(2, userId);
          statement.setTimestamp(3, randomPastTimestamp(random));
          statement.addBatch();
          if (++batch % batchSize == 0) {
            statement.executeBatch();
            connectionCommit(statement);
            logProgress("playlist_subscriptions", batch, total);
          }
        }
      }
      statement.executeBatch();
      connectionCommit(statement);
    }
    logDone("playlist_subscriptions");
  }

  private static List<String> seedConversations(Connection connection, int count, int batchSize)
      throws Exception {
    String sql = "INSERT INTO conversations (id, created_at, updated_at) VALUES (?, ?, ?)";
    List<String> ids = new ArrayList<>(count);
    logStart("conversations", count);
    try (PreparedStatement statement = connection.prepareStatement(sql)) {
      int batch = 0;
      for (int i = 0; i < count; i++) {
        String id = UuidCreator.getTimeOrderedEpoch().toString();
        ids.add(id);
        Timestamp createdAt = Timestamp.from(Instant.now());
        statement.setString(1, id);
        statement.setTimestamp(2, createdAt);
        statement.setTimestamp(3, createdAt);
        statement.addBatch();
        if (++batch % batchSize == 0) {
          statement.executeBatch();
          logProgress("conversations", batch, count);
        }
      }
      statement.executeBatch();
    }
    logDone("conversations");
    return ids;
  }

  private static List<String[]> seedConversationParticipants(
      Connection connection,
      List<String> conversationIds,
      List<String> userIds,
      int batchSize,
      Random random)
      throws Exception {
    String sql =
        "INSERT INTO conversation_participants (conversation_id, user_id, joined_at, last_read_at) "
            + "VALUES (?, ?, ?, ?)";
    List<String[]> pairs = new ArrayList<>(conversationIds.size());
    int userCount = userIds.size();
    int total = conversationIds.size() * 2;
    logStart("conversation_participants", total);
    try (PreparedStatement statement = connection.prepareStatement(sql)) {
      int batch = 0;
      for (String conversationId : conversationIds) {
        int u1 = random.nextInt(userCount);
        int u2 = random.nextInt(userCount - 1);
        if (u2 >= u1) {
          u2++;
        }
        String user1 = userIds.get(u1);
        String user2 = userIds.get(u2);
        pairs.add(new String[] {conversationId, user1, user2});

        Timestamp joined = randomPastTimestamp(random);
        statement.setString(1, conversationId);
        statement.setString(2, user1);
        statement.setTimestamp(3, joined);
        statement.setTimestamp(4, joined);
        statement.addBatch();

        statement.setString(1, conversationId);
        statement.setString(2, user2);
        statement.setTimestamp(3, joined);
        statement.setTimestamp(4, joined);
        statement.addBatch();

        batch += 2;
        if (batch % batchSize == 0) {
          statement.executeBatch();
          connectionCommit(statement);
          logProgress("conversation_participants", batch, total);
        }
      }
      statement.executeBatch();
      connectionCommit(statement);
    }
    logDone("conversation_participants");
    return pairs;
  }

  private static void seedDirectMessages(
      Connection connection,
      List<String[]> conversationPairs,
      int dmPerConversation,
      int batchSize,
      Random random)
      throws Exception {
    String sql =
        "INSERT INTO direct_messages (id, conversation_id, sender_id, receiver_id, content, status, created_at, read_at) "
            + "VALUES (?, ?, ?, ?, ?, 'PENDING', ?, ?)";
    int total = conversationPairs.size() * Math.max(1, dmPerConversation);
    logStart("direct_messages", total);
    try (PreparedStatement statement = connection.prepareStatement(sql)) {
      int batch = 0;
      for (String[] pair : conversationPairs) {
        String conversationId = pair[0];
        String user1 = pair[1];
        String user2 = pair[2];
        for (int i = 0; i < dmPerConversation; i++) {
          boolean flip = random.nextBoolean();
          String sender = flip ? user1 : user2;
          String receiver = flip ? user2 : user1;
          String id = UuidCreator.getTimeOrderedEpoch().toString();
          statement.setString(1, id);
          statement.setString(2, conversationId);
          statement.setString(3, sender);
          statement.setString(4, receiver);
          statement.setString(5, "Seed DM " + i);
          Timestamp createdAt = randomPastTimestamp(random);
          statement.setTimestamp(6, createdAt);
          statement.setTimestamp(7, createdAt);
          statement.addBatch();
          if (++batch % batchSize == 0) {
            statement.executeBatch();
            connectionCommit(statement);
            logProgress("direct_messages", batch, total);
          }
        }
      }
      statement.executeBatch();
      connectionCommit(statement);
    }
    logDone("direct_messages");
  }

  private static void seedNotifications(
      Connection connection, List<String> userIds, int count, int batchSize, Random random)
      throws Exception {
    String sql =
        "INSERT INTO notifications (id, event_id, receiver_id, title, content, level, created_at, read_at) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
    int userCount = userIds.size();
    String[] levels = {"INFO", "WARNING", "ERROR"};
    logStart("notifications", count);
    try (PreparedStatement statement = connection.prepareStatement(sql)) {
      int batch = 0;
      for (int i = 0; i < count; i++) {
        String id = UuidCreator.getTimeOrderedEpoch().toString();
        statement.setString(1, id);
        statement.setString(2, UuidCreator.getTimeOrderedEpoch().toString());
        statement.setString(3, userIds.get(random.nextInt(userCount)));
        statement.setString(4, "Seed Notification " + i);
        statement.setString(5, "Seed notification content " + i);
        statement.setString(6, levels[i % levels.length]);
        Timestamp createdAt = randomPastTimestamp(random);
        statement.setTimestamp(7, createdAt);
        statement.setTimestamp(8, random.nextBoolean() ? createdAt : null);
        statement.addBatch();
        if (++batch % batchSize == 0) {
          statement.executeBatch();
          connectionCommit(statement);
          logProgress("notifications", batch, count);
        }
      }
      statement.executeBatch();
      connectionCommit(statement);
    }
    logDone("notifications");
  }

  private static Timestamp randomPastTimestamp(Random random) {
    long days = 365L * 2;
    long seconds = Math.abs(random.nextLong()) % (days * 24 * 60 * 60);
    LocalDateTime time = LocalDateTime.now(ZoneOffset.UTC).minusSeconds(seconds);
    return Timestamp.from(time.toInstant(ZoneOffset.UTC));
  }

  private static int nextUniqueIndex(Random random, int bound, HashSet<Integer> seen) {
    if (bound <= 0) {
      return 0;
    }
    int candidate;
    do {
      candidate = random.nextInt(bound);
    } while (!seen.add(candidate));
    return candidate;
  }

  private static String getRequiredEnv(String key) {
    String value = System.getenv(key);
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException("Missing required env: " + key);
    }
    return value;
  }

  private static int getIntEnv(String key, int defaultValue) {
    String value = System.getenv(key);
    if (value == null || value.isBlank()) {
      return defaultValue;
    }
    return Integer.parseInt(value);
  }

  private static String getStringEnv(String key, String defaultValue) {
    String value = System.getenv(key);
    if (value == null || value.isBlank()) {
      return defaultValue;
    }
    return value;
  }

  private static boolean getBooleanEnv(String key, boolean defaultValue) {
    String value = System.getenv(key);
    if (value == null || value.isBlank()) {
      return defaultValue;
    }
    return value.equalsIgnoreCase("true")
        || value.equalsIgnoreCase("1")
        || value.equalsIgnoreCase("yes");
  }

  private static int scaled(int base, int percent) {
    if (percent <= 0) {
      return 0;
    }
    long value = Math.round(base * (percent / 100.0));
    return (int) Math.max(1, value);
  }

  private static int countTable(Connection connection, String table) throws Exception {
    String sql = "SELECT COUNT(*) FROM " + table;
    try (var statement = connection.createStatement();
        var rs = statement.executeQuery(sql)) {
      if (rs.next()) {
        return rs.getInt(1);
      }
    }
    return 0;
  }

  private static List<String> loadIds(Connection connection, String table) throws Exception {
    String sql = "SELECT id FROM " + table;
    List<String> ids = new ArrayList<>();
    try (var statement = connection.createStatement();
        var rs = statement.executeQuery(sql)) {
      while (rs.next()) {
        ids.add(rs.getString(1));
      }
    }
    return ids;
  }

  private static void logConfig(
      int scalePercent,
      int users,
      int contents,
      int tags,
      int tagsPerContent,
      int playlists,
      int contentsPerPlaylist,
      int subsPerPlaylist,
      int conversations,
      int dmPerConversation,
      int notifications,
      int testUsers,
      int batchSize) {
    System.out.println("=== StagingSeeder config ===");
    System.out.printf("scalePercent=%d%%%n", scalePercent);
    System.out.printf("users=%d%n", users);
    System.out.printf("contents=%d%n", contents);
    System.out.printf("tags=%d%n", tags);
    System.out.printf("tagsPerContent=%d%n", tagsPerContent);
    System.out.printf("playlists=%d%n", playlists);
    System.out.printf("contentsPerPlaylist=%d%n", contentsPerPlaylist);
    System.out.printf("subsPerPlaylist=%d%n", subsPerPlaylist);
    System.out.printf("conversations=%d%n", conversations);
    System.out.printf("dmPerConversation=%d%n", dmPerConversation);
    System.out.printf("notifications=%d%n", notifications);
    System.out.printf("testUsers=%d%n", testUsers);
    System.out.printf("batchSize=%d%n", batchSize);
    System.out.println("============================");
  }

  private static void logStart(String name, int total) {
    System.out.printf(">> %s: start (total=%d)%n", name, total);
  }

  private static void logProgress(String name, int current, int total) {
    int pct = (int) Math.min(100, Math.round(current * 100.0 / Math.max(1, total)));
    System.out.printf(".. %s: %d/%d (%d%%)%n", name, current, total, pct);
  }

  private static void logDone(String name) {
    System.out.printf("<< %s: done%n", name);
  }

  private static void truncateAll(Connection connection) throws Exception {
    String[] tables = {
      "direct_messages",
      "conversation_participants",
      "conversations",
      "notifications",
      "playlist_subscriptions",
      "playlist_contents",
      "playlists",
      "content_tags",
      "tags",
      "reviews",
      "follows",
      "watching_sessions",
      "contents",
      "users"
    };
    System.out.println(">> truncating tables");
    try (var statement = connection.createStatement()) {
      statement.execute("SET FOREIGN_KEY_CHECKS=0");
      for (String table : tables) {
        statement.execute("TRUNCATE TABLE " + table);
        System.out.printf(".. truncated %s%n", table);
      }
      statement.execute("SET FOREIGN_KEY_CHECKS=1");
    }
    connection.commit();
    System.out.println("<< truncating tables: done");
  }

  private static void connectionCommit(PreparedStatement statement) throws Exception {
    statement.getConnection().commit();
  }
}
