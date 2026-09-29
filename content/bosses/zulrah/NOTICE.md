# Zulrah encounter reference data

The four rotation action tables in `ZulrahRotations.kt` are adapted from
PufferAI/PufferLib, `ocean/osrs/encounters/encounter_zulrah.h`, commit
`6ffa5b10dbbbe4d1e8288367c7d9d3acd3bad4a2`:

https://github.com/PufferAI/PufferLib/blob/6ffa5b10dbbbe4d1e8288367c7d9d3acd3bad4a2/ocean/osrs/encounters/encounter_zulrah.h

They provide a complete, independently implemented encounter schedule. They are
not recovered Alora server code. The supplied Alora recording covers only part
of one fight and cannot establish all rotations, random branches or drop rates.

`durationTicks` preserves the reference simulator's phase duration as a timing
target. The runtime must allow every scheduled action and its resolution to
finish before diving; the reference's final reset phase has more actions than
its duration permits at a three-tick action rate. Reference timings and hazard
behaviour remain implementation choices, not measurements from the recording.

The first phase is the cloud-only opening. The final phase is a reset with five
attacks and four cloud barrages. When continuing into a new rotation after that
reset, skip the cloud-only opening (start at index 1).

`safeOffsets` preserves the same source's stand/stall centers, relative to its
north spawn's southwest anchor `(10, 12)`. Positive z is north. Each center
reserves a three-by-three area from cloud placement. These are reference arena
coordinates, not positions established by the Alora recording; placement must
also respect the native map's collision and usable land.

## Reference license

MIT License

Copyright (c) 2022 PufferAI

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
