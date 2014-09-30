CREATE TABLE uploading_photo(
-- Maintain the order of the uploading photos. This should not be set explicitly, SQLite will
-- automatically set a value during INSERT.
-- See: <http://www.sqlite.org/autoinc.html>
num INTEGER PRIMARY KEY,

-- The album that this photo is being uploaded to
album_id INTEGER REFERENCES album,

-- The location of the temporary file on the device filesystem
tmp_filename TEXT NOT NULL UNIQUE,

-- Matches an enum ordinal value of: UploadingPhoto.UploadStrategy
upload_strategy INTEGER NOT NULL,

-- Matches an enum ordinal value of: UploadingPhoto.UploadState
upload_state INTEGER NOT NULL,

-- The photo_id that has this photo has already been uploaded as.
-- May be NULL
photo_id TEXT UNIQUE
);

-- Efficiently update/delete an uploading_photo by tmp_filename
CREATE INDEX uploading_photo_index ON uploading_photo(tmp_filename);
