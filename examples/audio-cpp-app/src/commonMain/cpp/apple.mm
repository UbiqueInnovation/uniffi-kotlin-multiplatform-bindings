/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

#import <AVFoundation/AVFoundation.h>
#import <cstddef>

struct WaveStreamContext {
  AVAudioEngine *audioEngine;
  AVAudioPlayerNode *playerNode;
  AVAudioPCMBuffer *pcmBuffer;
};

struct WaveStream {
  WaveStreamContext *context;
};

extern "C" WaveStream startWave(const float *wave, size_t waveLength,
                                size_t waveSamplingRate) {
  AVAudioEngine *audioEngine = [[AVAudioEngine alloc] init];
  AVAudioPlayerNode *playerNode = [[AVAudioPlayerNode alloc] init];
  [audioEngine attachNode:playerNode];
  AVAudioFormat *format =
      [[AVAudioFormat alloc] initWithCommonFormat:AVAudioPCMFormatFloat32
                                       sampleRate:(double)waveSamplingRate
                                         channels:1
                                      interleaved:NO];

  AVAudioPCMBuffer *pcmBuffer = [[AVAudioPCMBuffer alloc]
      initWithPCMFormat:format
          frameCapacity:(AVAudioFrameCount)waveLength];
  memcpy(pcmBuffer.floatChannelData[0], wave, waveLength * sizeof(float));
  pcmBuffer.frameLength = waveLength;

  AVAudioMixerNode *mixer = audioEngine.mainMixerNode;
  [audioEngine connect:playerNode to:mixer format:format];

  NSError *error = nil;
  if (![audioEngine startAndReturnError:&error]) {
    [playerNode release];
    [audioEngine release];
    return WaveStream{nullptr};
  }

  [playerNode scheduleBuffer:pcmBuffer
           completionHandler:^{
           }];
  [playerNode play];

  return WaveStream{new WaveStreamContext{audioEngine, playerNode, pcmBuffer}};
}

extern "C" void stopWave(WaveStream stream) {
  if (stream.context != nullptr) {
    [stream.context->playerNode stop];
    [stream.context->audioEngine stop];
    [stream.context->audioEngine reset];
    [stream.context->playerNode release];
    [stream.context->audioEngine release];
    [stream.context->pcmBuffer release];
    delete stream.context;
  }
}
