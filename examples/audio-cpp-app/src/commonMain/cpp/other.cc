/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

#include <cstddef>

struct WaveStream {
  void *inner;
};

extern "C" WaveStream startWave(const float *wave, size_t waveLength,
                                size_t waveSamplingRate) {
  (void)wave;
  (void)waveLength;
  (void)waveSamplingRate;
  return WaveStream{nullptr};
}

extern "C" void stopWave(WaveStream stream) { (void)stream; }
