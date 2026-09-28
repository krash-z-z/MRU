# MRU (2drendermcplus)

High-performance GPU-accelerated 2D rendering engine for Minecraft Fabric featuring MSDF distance-field typography, continuous-curvature SDF squircles, and multi-pass post-processing blur.

---

## Features

- **MSDF Font Engine**: Ultra-crisp vector font rendering powered by Multi-channel Signed Distance Fields with $O(1)$ ASCII metric tables and LRU width caching.
- **Continuous-Curvature Squircles**: Superellipse and squircle SDF rendering with variable corner radii, corner smoothing, and anti-aliased geometry.
- **Post-Processing Pipeline**: Fullscreen and scissored multi-pass Gaussian blur, Acrylic blur, and Liquid Glass optical dispersion shaders.
- **Unified Color Architecture**: High-speed packed integer + floating-point color structure with zero-overhead conversions.
- **Blaze3D Integration**: Native batching and layer extraction compatible with modern Minecraft GUI render graphs.

---

## Building

```bash
./gradlew build
```

The compiled mod JAR will be located in `build/libs/`.
