/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

#include <aaudio/AAudio.h>
#include <algorithm>
#include <cstddef>
#include <memory>

struct WaveStream {
  AAudioStream *inner;
};

struct WaveStreamCallbackData {
  const float *wave;
  size_t waveLength;
  size_t numFramesWritten;
};

static aaudio_data_callback_result_t audioCallback(AAudioStream *stream,
                                                   void *userData,
                                                   void *audioData,
                                                   int32_t numFrames) {
  WaveStreamCallbackData *callbackData =
      static_cast<WaveStreamCallbackData *>(userData);

  size_t framesToWrite =
      std::min(callbackData->waveLength - callbackData->numFramesWritten,
               static_cast<size_t>(numFrames));

  if (framesToWrite > 0) {
    memcpy(audioData, callbackData->wave + callbackData->numFramesWritten,
           framesToWrite * sizeof(float));
    callbackData->numFramesWritten += framesToWrite;
  }

  if (callbackData->numFramesWritten >= callbackData->waveLength) {
    delete callbackData;
    return AAUDIO_CALLBACK_RESULT_STOP;
  }

  return AAUDIO_CALLBACK_RESULT_CONTINUE;
}

extern "C" WaveStream startWave(const float *wave, size_t waveLength,
                                size_t waveSamplingRate) {
  AAudioStreamBuilder *builder;

  AAudio_createStreamBuilder(&builder);
  AAudioStreamBuilder_setFormat(builder, AAUDIO_FORMAT_PCM_FLOAT);
  AAudioStreamBuilder_setSampleRate(builder,
                                    static_cast<int32_t>(waveSamplingRate));
  AAudioStreamBuilder_setBufferCapacityInFrames(
      builder, static_cast<int32_t>(waveLength));
  AAudioStreamBuilder_setSharingMode(builder, AAUDIO_SHARING_MODE_EXCLUSIVE);
  AAudioStreamBuilder_setChannelCount(builder, 1);

  WaveStreamCallbackData *callbackData =
      new WaveStreamCallbackData{wave, waveLength, 0};
  AAudioStreamBuilder_setDataCallback(builder, audioCallback, callbackData);

  AAudioStream *stream;
  aaudio_result_t result = AAudioStreamBuilder_openStream(builder, &stream);
  AAudioStreamBuilder_delete(builder);

  if (result != AAUDIO_OK) {
    delete callbackData;
    return WaveStream{nullptr};
  }

  result = AAudioStream_requestStart(stream);
  if (result != AAUDIO_OK) {
    delete callbackData;
    AAudioStream_close(stream);
    return WaveStream{nullptr};
  }

  return WaveStream{stream};
}

extern "C" void stopWave(WaveStream stream) {
  if (stream.inner != nullptr) {
    AAudioStream_requestStop(stream.inner);
    AAudioStream_close(stream.inner);
  }
}
