// clang-format off
$input v_clipPosition, v_color0, v_ditheringAndMaskTinting, v_fog, v_lightmapUV
$input v_texcoord0, v_worldPos, v_worldPosition, v_origPosition
// clang-format on


#include "bgfx_shader.sh"

SAMPLER2D_AUTOREG(s_LightMapTexture);
SAMPLER2D_AUTOREG(s_MatTexture);
SAMPLER2D_AUTOREG(s_SeasonsTexture);

uniform vec4 DitherParams;
uniform vec4 DitherParams2[3];
uniform vec4 ViewPositionAndTime;
uniform vec4 FogColor;

bool shouldDither(vec2 ditheringAndMaskTinting, vec4 clipPosition, vec4 worldPosition) {
#if defined(DITHERING__ON) && (defined(ALPHA_TEST_PASS) || defined(TRANSPARENT_PASS))
  if (ditheringAndMaskTinting.x > 0.5) {
    vec3 ndc = clipPosition.xyz / clipPosition.w;
    vec2 screenUV = ndc.xy * 0.5 + 0.5;
    vec2 ditherRange = DitherParams2[2].xy;
    float ditherBlockSize = DitherParams2[2].z;

    vec2 pixelCoords = screenUV * DitherParams.xy;
    vec2 ditherBlock = floor(pixelCoords / ditherBlockSize) * ditherBlockSize;

    vec2 block4 = floor(ditherBlock * 0.25);
    vec2 block2 = floor(ditherBlock * 0.5);
    vec2 block1 = floor(ditherBlock);

    vec3 forward = -normalize(vec3(u_view[0].z, u_view[1].z, u_view[2].z));
    vec3 fragment = worldPosition.xyz - ViewPositionAndTime.xyz;
    float viewDot = dot(forward, fragment);

    float fadeValue = smoothstep(ditherRange.x, ditherRange.y, viewDot);

    float hash1 = fract(block4.x * 0.5 + block4.y * block4.y * 0.75);
    float hash2 = fract(block2.x * 0.5 + block2.y * block2.y * 0.75);
    float hash3 = fract(block1.x * 0.5 + block1.y * block1.y * 0.75);

    float hash = ((hash1 * 0.25 + hash2) * 0.25 + hash3) * 64.0 + 0.5;
    float ditherValue = hash * 0.015625;

    return fadeValue <= ditherValue;
  }
#endif

  return false;
}

vec4 applyLightingAndFog(vec4 color, vec2 lightmapUV, float fogIntensity) {
  vec3 litColor = texture2D(s_LightMapTexture, lightmapUV).xyz * color.xyz;
  return vec4(mix(litColor, FogColor.xyz, vec3_splat(fogIntensity)), color.a);
}

vec4 applySeasons(vec4 materialColor, vec4 vertexColor) {
  vec3 seasonColor = texture2D(s_SeasonsTexture, vertexColor.xy).xyz * 2.0;
  vec3 tint = mix(vec3_splat(1.0), seasonColor, vec3_splat(vertexColor.z));
  return vec4(materialColor.xyz * tint * vertexColor.a, 1.0);
}

vec4 applyMaskTinting(vec4 materialColor, vec4 vertexColor) {
  vec3 tinted = materialColor.xyz * vertexColor.xyz;
  vec3 color = mix(materialColor.xyz, tinted, vec3_splat(materialColor.a)) * vertexColor.a;
  return vec4(color, 1.0);
}

vec4 shadeMaterial(vec4 materialColor, vec4 vertexColor, vec2 ditheringAndMaskTinting) {
  vec4 color = vec4(materialColor.xyz * vertexColor.xyz, materialColor.a);

#if defined(TRANSPARENT_PASS)
  color.a *= vertexColor.a;
#elif defined(OPAQUE_PASS)
  color.a = vertexColor.a;
#endif

#if defined(TRANSPARENT_PASS) || defined(OPAQUE_PASS)
  if (ditheringAndMaskTinting.y > 0.5) {
    color = applyMaskTinting(materialColor, vertexColor);
  }
#endif

#if defined(SEASONS__ON) && (defined(ALPHA_TEST_PASS) || defined(OPAQUE_PASS))
  color = applySeasons(materialColor, vertexColor);
#endif

  return color;
}

void main() {
  vec4 materialColor = texture2D(s_MatTexture, v_texcoord0);
  bool bDither = shouldDither(v_ditheringAndMaskTinting,
    v_clipPosition, v_worldPosition);
  vec4 color = vec4_splat(1.0);

#if defined(ALPHA_TEST_PASS) || defined(DEPTH_ONLY_PASS)
  if (materialColor.a < 0.5 || bDither) {
    discard;
  }
#endif

#if defined(TRANSPARENT_PASS)
  color = shadeMaterial(materialColor, v_color0, v_ditheringAndMaskTinting);
  if (bDither) {
    color.a = 0.0;
  }
#elif defined(OPAQUE_PASS) || defined(ALPHA_TEST_PASS)
  color = shadeMaterial(materialColor, v_color0, v_ditheringAndMaskTinting);
#endif

  gl_FragColor = applyLightingAndFog(color, v_lightmapUV, v_fog.a);
}
