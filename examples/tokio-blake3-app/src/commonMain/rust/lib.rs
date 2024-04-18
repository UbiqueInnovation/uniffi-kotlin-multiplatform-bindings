/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

#[uniffi::export(async_runtime = "tokio")]
async fn retrieve_from(url: String) -> String {
    reqwest::get(&url).await.unwrap().text().await.unwrap()
}

#[uniffi::export]
fn hash_string(s: String) -> String {
    blake3::hash(s.as_bytes()).to_string()
}

uniffi::setup_scaffolding!();
