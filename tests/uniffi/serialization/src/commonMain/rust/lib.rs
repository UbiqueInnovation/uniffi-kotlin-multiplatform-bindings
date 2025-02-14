/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

#[derive(uniffi::Enum)]
pub enum Difficulty {
    Easy,
    Middle,
    Hard,
}

#[derive(uniffi::Record)]
pub struct Values {
    a: i64,
    b: i64,
}

#[derive(uniffi::Record)]
pub struct ValuesOptional {
    a: Option<i64>,
    b: Option<i64>,
}

// TODO: Figure out how to serialize/deserialize something like this
//       tagged vs untagged, should "Add" be represented as an array
//       how would you represent a null value, ...
#[derive(uniffi::Enum)]
pub enum Calculation {
    Val(i64),
    Neg { value: i64 },
    Add(i64, i64),
    Sub(Values),
}

uniffi::setup_scaffolding!("serialization");
