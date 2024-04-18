/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

use std::time::Duration;

use tokio::time::sleep;

#[repr(C)]
struct WaveStream(*const ());

impl WaveStream {
    fn is_err(&self) -> bool {
        self.0.is_null()
    }
}

unsafe impl Send for WaveStream {}

extern "C" {
    #[link_name = "startWave"]
    fn start_wave(wave: *const f32, wave_length: usize, wave_sampling_rate: usize) -> WaveStream;
    #[link_name = "stopWave"]
    fn stop_wave(thread: WaveStream);
}

#[derive(Debug, thiserror::Error, uniffi::Error)]
enum PlaySoundError {
    #[error("failed to start wave")]
    StartWaveFailed,
}

#[uniffi::export(async_runtime = "tokio")]
async fn play_sound(frequency: f32) -> Result<(), PlaySoundError> {
    const SAMPLING_RATE: usize = 44100;
    const WAVE_LENGTH_IN_SECONDS: usize = 2;

    let wave = (0..SAMPLING_RATE * WAVE_LENGTH_IN_SECONDS)
        .map(|idx| {
            (idx as f32 / SAMPLING_RATE as f32 * frequency * 2.0 * std::f32::consts::PI).sin()
        })
        .collect::<Vec<_>>();

    let stream = unsafe { start_wave(wave.as_ptr(), wave.len(), SAMPLING_RATE) };
    if stream.is_err() {
        return Err(PlaySoundError::StartWaveFailed);
    }

    sleep(Duration::from_secs(WAVE_LENGTH_IN_SECONDS as u64)).await;
    unsafe { stop_wave(stream) };

    Ok(())
}

uniffi::setup_scaffolding!();
