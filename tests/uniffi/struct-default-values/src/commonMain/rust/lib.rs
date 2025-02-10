/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

struct Bookmark {
    guid: Option<String>,
    position: i32,
    last_modified: Option<i32>,
    url: String,
    title: Option<String>,
}

fn create_bookmark(
    guid: Option<String>,
    position: i32,
    last_modified: Option<i32>,
    url: String,
    title: Option<String>,
) -> Bookmark {
    Bookmark {
        guid,
        position,
        last_modified,
        url,
        title,
    }
}

struct Bookmark2 {
    guid: Option<String>,
    position: i32,
    last_modified: Option<i32>,
    url: String,
    title: Option<String>,
}

fn create_bookmark2(
    guid: Option<String>,
    position: i32,
    last_modified: Option<i32>,
    url: String,
    title: Option<String>,
) -> Bookmark2 {
    Bookmark2 {
        guid,
        position,
        last_modified,
        url,
        title,
    }
}

uniffi::include_scaffolding!("struct-default-values");
