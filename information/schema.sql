CREATE TABLE users (
  id CHAR(36) NOT NULL,
  email VARCHAR(255) NOT NULL,
  name VARCHAR(100) NOT NULL,
  password_hash VARCHAR(255) NOT NULL,
  auth_provider VARCHAR(20) NOT NULL DEFAULT 'LOCAL',
  provider_user_id VARCHAR(255) NULL,
  role VARCHAR(20) NOT NULL DEFAULT 'USER',
  locked TINYINT(1) NOT NULL DEFAULT 0,
  profile_image_url VARCHAR(2048) NULL,
  temp_password_hash VARCHAR(255) NULL,
  temp_password_expires_at TIMESTAMP(6) NULL,
  created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (id),
  UNIQUE KEY uq_users_email (email),
  UNIQUE KEY uq_users_provider (auth_provider, provider_user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE contents (
  id CHAR(36) NOT NULL,
  type VARCHAR(20) NOT NULL,
  title VARCHAR(255) NOT NULL,
  description TEXT NOT NULL,
  thumbnail_url VARCHAR(2048) NOT NULL,
  average_rating DOUBLE(3,2) NOT NULL DEFAULT 0.00,
  review_count INT NOT NULL DEFAULT 0,
  watcher_count BIGINT NOT NULL DEFAULT 0,
  created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (id),
  CONSTRAINT chk_contents_rating CHECK (average_rating >= 0.0 AND average_rating <= 5.0),
  CONSTRAINT chk_contents_review_count CHECK (review_count >= 0),
  CONSTRAINT chk_contents_watcher_count CHECK (watcher_count >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE tags (
  id CHAR(36) NOT NULL,
  name VARCHAR(100) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uq_tags_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE content_tags (
  content_id CHAR(36) NOT NULL,
  tag_id CHAR(36) NOT NULL,
  PRIMARY KEY (content_id, tag_id),
  CONSTRAINT fk_content_tags_content
    FOREIGN KEY (content_id) REFERENCES contents(id)
    ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT fk_content_tags_tag
    FOREIGN KEY (tag_id) REFERENCES tags(id)
    ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE playlists (
  id CHAR(36) NOT NULL,
  owner_id CHAR(36) NOT NULL,
  title VARCHAR(255) NOT NULL,
  description TEXT NOT NULL,
  subscriber_count BIGINT NOT NULL DEFAULT 0,
  created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (id),
  CONSTRAINT fk_playlists_owner
    FOREIGN KEY (owner_id) REFERENCES users(id)
    ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT chk_playlists_subscriber_count CHECK (subscriber_count >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE playlist_contents (
  playlist_id CHAR(36) NOT NULL,
  content_id CHAR(36) NOT NULL,
  added_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (playlist_id, content_id),
  CONSTRAINT fk_playlist_contents_playlist
    FOREIGN KEY (playlist_id) REFERENCES playlists(id)
    ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT fk_playlist_contents_content
    FOREIGN KEY (content_id) REFERENCES contents(id)
    ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE playlist_subscriptions (
  playlist_id CHAR(36) NOT NULL,
  user_id CHAR(36) NOT NULL,
  created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (playlist_id, user_id),
  CONSTRAINT fk_playlist_subscriptions_playlist
    FOREIGN KEY (playlist_id) REFERENCES playlists(id)
    ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT fk_playlist_subscriptions_user
    FOREIGN KEY (user_id) REFERENCES users(id)
    ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE reviews (
  id CHAR(36) NOT NULL,
  content_id CHAR(36) NOT NULL,
  author_id CHAR(36) NOT NULL,
  text TEXT NOT NULL,
  rating DOUBLE(3,2) NOT NULL,
  created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (id),
  UNIQUE KEY uq_reviews_content_author (content_id, author_id),
  CONSTRAINT fk_reviews_content
    FOREIGN KEY (content_id) REFERENCES contents(id)
    ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT fk_reviews_author
    FOREIGN KEY (author_id) REFERENCES users(id)
    ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT chk_reviews_rating CHECK (rating >= 0.0 AND rating <= 5.0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE follows (
  id CHAR(36) NOT NULL,
  follower_id CHAR(36) NOT NULL,
  followee_id CHAR(36) NOT NULL,
  created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (id),
  UNIQUE KEY uq_follows_pair (follower_id, followee_id),
  CONSTRAINT fk_follows_follower
    FOREIGN KEY (follower_id) REFERENCES users(id)
    ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT fk_follows_followee
    FOREIGN KEY (followee_id) REFERENCES users(id)
    ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT chk_follows_not_self CHECK (follower_id <> followee_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE conversations (
  id CHAR(36) NOT NULL,
  created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE conversation_participants (
  conversation_id CHAR(36) NOT NULL,
  user_id CHAR(36) NOT NULL,
  joined_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  last_read_at TIMESTAMP(6) NULL,
  PRIMARY KEY (conversation_id, user_id),
  CONSTRAINT fk_conversation_participants_conversation
    FOREIGN KEY (conversation_id) REFERENCES conversations(id)
    ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT fk_conversation_participants_user
    FOREIGN KEY (user_id) REFERENCES users(id)
    ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE direct_messages (
  id CHAR(36) NOT NULL,
  conversation_id CHAR(36) NOT NULL,
  sender_id CHAR(36) NOT NULL,
  receiver_id CHAR(36) NOT NULL,
  content TEXT NOT NULL,
  created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  read_at TIMESTAMP(6) NULL,
  PRIMARY KEY (id),
  CONSTRAINT fk_direct_messages_conversation
    FOREIGN KEY (conversation_id) REFERENCES conversations(id)
    ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT fk_direct_messages_sender
    FOREIGN KEY (sender_id) REFERENCES users(id)
    ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT fk_direct_messages_receiver
    FOREIGN KEY (receiver_id) REFERENCES users(id)
    ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT chk_direct_messages_not_self CHECK (sender_id <> receiver_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE notifications (
  id CHAR(36) NOT NULL,
  receiver_id CHAR(36) NOT NULL,
  title VARCHAR(255) NOT NULL,
  content TEXT NOT NULL,
  level VARCHAR(20) NOT NULL,
  created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  read_at TIMESTAMP(6) NULL,
  PRIMARY KEY (id),
  CONSTRAINT fk_notifications_receiver
    FOREIGN KEY (receiver_id) REFERENCES users(id)
    ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE watching_sessions (
  id CHAR(36) NOT NULL,
  content_id CHAR(36) NOT NULL,
  watcher_id CHAR(36) NOT NULL,
  created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  ended_at TIMESTAMP(6) NULL,
  PRIMARY KEY (id),
  CONSTRAINT fk_watching_sessions_content
    FOREIGN KEY (content_id) REFERENCES contents(id)
    ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT fk_watching_sessions_watcher
    FOREIGN KEY (watcher_id) REFERENCES users(id)
    ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT chk_watching_sessions_end_after_start CHECK (ended_at IS NULL OR ended_at >= created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
