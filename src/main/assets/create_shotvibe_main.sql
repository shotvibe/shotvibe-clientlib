-------------------------------------------------------------------------------
-- The photo albums that are available to the app
-------------------------------------------------------------------------------

CREATE TABLE album(
-- The album_id is the value that is returned from the server
album_id INTEGER PRIMARY KEY,

-- The user friendly name of the album, also returned from the server
name TEXT NOT NULL,

creator_id INTEGER REFERENCES username NOT NULL,

date_created DATETIME NOT NULL,

last_updated DATETIME NOT NULL,

-- The value from the HTTP ETag header for the album
last_etag TEXT,

num_new_photos INTEGER,

last_access DATETIME NULL
);

CREATE TABLE user(
-- The id is the value that is returned from the server
user_id INTEGER PRIMARY KEY,

-- The user friendly name of the user, returned from the server
nickname TEXT NOT NULL,

-- The URL of the avatar image of the user, returned from the server
avatar_url TEXT NOT NULL,

-- The score of the user, returned from the server
user_glance_score INTEGER NOT NULL
);

CREATE TABLE photo(
-- The album that this photo belongs to
photo_album INTEGER REFERENCES album,

-- The order of the photos in the album is according to this field
num INTEGER NOT NULL,

-- The name of the image file (without a ".jpg" extension), as returned from the server
photo_id TEXT NOT NULL,

-- The original photo URL
url TEXT NOT NULL,

-- The user who uploaded this photo
author_id INTEGER REFERENCES username,

-- The timestamp this photo was uploaded, also returned from the server
created DATETIME NOT NULL,

UNIQUE(photo_album, photo_id)
);

CREATE TABLE photo_comment(
-- The photo that this comment belongs to
photo_id TEXT REFERENCES photo(photo_id),

-- The date the comment was posted
date_created DATETIME NOT NULL,

-- The user who created this comment
author_id INTEGER REFERENCES username,

client_msg_id INTEGER NOT NULL,

-- The content of the comment
comment_text TEXT NOT NULL,

UNIQUE(photo_id, author_id, client_msg_id)
);

CREATE TABLE photo_glance(
-- The photo that this glances
photo_id TEXT REFERENCES photo(photo_id),

-- The user who created this glance
author_id INTEGER REFERENCES username,

-- The name of the emoticon that the author chose
emoticon_name TEXT NOT NULL,

-- The order of the glances as returned by the server
num INTEGER NOT NULL,

UNIQUE(author_id, photo_id)
);

CREATE TABLE album_member(
-- The user
user_id INTEGER REFERENCES user,

-- The album that this user is a member of
album_id INTEGER REFERENCES album,

-- Is the member an admin of this album
album_admin BOOLEAN NOT NULL,

-- The user that added this user to this album
added_by_user_id INTEGER REFERENCES user,

UNIQUE(user_id, album_id)
);

CREATE TABLE phone_contact(
-- A phone number as it appears in the device phone book
phone_number TEXT NOT NULL,

-- Contact name
last_name TEXT NOT NULL,
first_name TEXT NOT NULL,

-- Is this a mobile phone number or not
is_mobile BOOLEAN NOT NULL,

-- The user of this phone number, or NULL if the number does not belong to a registered user
user_id INTEGER,

-- Avatar url for registered and unregistered users
avatar_url TEXT,

-- Phone number in canonical form, or NULL if the number is invalid
canonical_number TEXT,

-- When this phone number was last queried
query_time DATETIME NOT NULL,

UNIQUE(phone_number, last_name, first_name)
);

-- These indexes are necessary in order to efficiently retrieve all of the
-- photos of a particular album, or all of the members of a particular album

CREATE INDEX photo_index ON photo(photo_album);
CREATE INDEX album_member_index ON album_member(album_id);

-- This index is necessary in order to efficiently retrieve photos sorted by
-- correct order, and also to efficiently retrieve the latest n photos

CREATE INDEX photo_num_index ON photo(num);

CREATE INDEX photo_comment_index ON photo_comment(photo_id);
CREATE INDEX photo_comment_date_created_index ON photo_comment(date_created);
