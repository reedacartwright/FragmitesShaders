## Notice

This repository is intended for educational use as its contents have been
gathered from various locations and authors, including myself. The MIT License
only covers my contributions to the files.

- vanilla: these are an approximation of the shaders used by vanilla
  bedrock edition. The source code was initially extracted from 1.21.31 and
  manually updated by myself to match more recent versions of the game.

- useless: these are a port and update of OEOTYAN's [useless shaders](https://github.com/OEOTYAN/useless-shaders). Supports chunk border visualization,
redstone levels, light levels, and fog. 

## Building

These shaders are built using [Lazurite](https://veka0.github.io/lazurite/).
Before building you will need to create a folder called `data/base` and
place in there copies of the material.bin shaders that are shipped with Bedrock.
See [vanilla/build.sh](vanilla/build.sh) for an example of compiling on Linux.
