$input v_color0, v_fog, v_texcoord0, v_lightmapUV, v_normal, v_tangent, v_bitangent, v_worldPos
#if defined(GEOMETRY_PREPASS) || defined(GEOMETRY_PREPASS_ALPHA_TEST)
    $input v_pbrTextureId
#endif

#include <bgfx_shader.sh>
#include <bgfx_compute.sh>

struct PBRTextureData {
    float colourToMaterialUvScale0;
    float colourToMaterialUvScale1;
    float colourToMaterialUvBias0;
    float colourToMaterialUvBias1;
    float colourToNormalUvScale0;
    float colourToNormalUvScale1;
    float colourToNormalUvBias0;
    float colourToNormalUvBias1;
    int flags;
    float uniformRoughness;
    float uniformEmissive;
    float uniformMetalness;
    float maxMipColour;
    float maxMipMer;
    float maxMipNormal;
    float pad;
};

uniform vec4 LightWorldSpaceDirection;
uniform vec4 GlobalRoughness;
uniform vec4 FogColor;
uniform vec4 FogAndDistanceControl;
uniform vec4 LightDiffuseColorAndIntensity;
uniform vec4 ViewPositionAndTime;
uniform vec4 RenderChunkFogAlpha;
uniform vec4 BlockSkyAmbientContribution;

SAMPLER2D(s_MatTexture, 0);
SAMPLER2D(s_LightMapTexture, 1);
SAMPLER2D(s_SeasonsTexture, 2);
BUFFER_RO(s_PBRData, PBRTextureData, 3);

CONST(int) kPBRTextureDataFlagHasMaterialTexture  = (1 << 0);
// These two are mutually exclusive
CONST(int) kPBRTextureDataFlagHasNormalTexture    = (1 << 1);
CONST(int) kPBRTextureDataFlagHasHeightMapTexture = (1 << 2);

void main() {
    vec4 albedo = texture2D(s_MatTexture, v_texcoord0);

#if defined(ALPHA_TEST) || defined(GEOMETRY_PREPASS_ALPHA_TEST) || defined(DEPTH_ONLY)
    if (albedo.a < 0.5) {
        discard;
    }
#endif

#if defined(SEASONS) && !defined(TRANSPARENT)
    albedo.rgb *= mix(vec3(1.0, 1.0, 1.0), texture2D(s_SeasonsTexture, v_color0.xy).rgb * 2.0, v_color0.b);
    albedo.rgb *= v_color0.a;
    albedo.a = 1.0;
#else
    albedo.rgb *= v_color0.rgb;
    albedo.a *= v_color0.a;
#endif

#if defined(GEOMETRY_PREPASS) || defined(GEOMETRY_PREPASS_ALPHA_TEST)
    PBRTextureData pbrData = s_PBRData[v_pbrTextureId];
    vec2 colourToMaterialUvScale = vec2(pbrData.colourToMaterialUvScale0, pbrData.colourToMaterialUvScale1);
    vec2 colourToMaterialUvBias = vec2(pbrData.colourToMaterialUvBias0, pbrData.colourToMaterialUvBias1);
    vec2 colourToNormalUvScale = vec2(pbrData.colourToNormalUvScale0, pbrData.colourToNormalUvScale1);
    vec2 colourToNormalUvBias = vec2(pbrData.colourToNormalUvBias0, pbrData.colourToNormalUvBias1);

    vec3 normal = vec3(0.0, 0.0, 1.0);
    if ((pbrData.flags & kPBRTextureDataFlagHasNormalTexture) == kPBRTextureDataFlagHasNormalTexture) {
        normal = texture2D(s_MatTexture, fma(v_texcoord0, colourToNormalUvScale, colourToNormalUvBias)).xyz * vec3(2.0, 2.0, 2.0) - vec3(1.0, 1.0, 1.0);
    } else if ((pbrData.flags & kPBRTextureDataFlagHasHeightMapTexture) == kPBRTextureDataFlagHasHeightMapTexture) {
        vec2 texCoord = fma(v_texcoord0, colourToNormalUvScale, colourToNormalUvBias);
        float _2030 = clamp(fma(min(pbrData.maxMipNormal - pbrData.maxMipColour, pbrData.maxMipNormal), -1.0, 2.0), 0.0, 1.0);
        if (_2030 > 0.0) {
            vec2 matTextureSize = vec2(textureSize(s_MatTexture, 0));
            vec2 _1796 = texCoord * matTextureSize;
            vec2 _1702 = fract(_1796);
            if (abs(_1702.x - 0.5) < 0.0625) {
                texCoord.x += ((_1702.x > 0.5) ? 3.814697265625e-06 : (-3.814697265625e-06));
            }
            if (abs(_1702.y - 0.5) < 0.0625) {
                texCoord.y += ((_1702.y > 0.5) ? 3.814697265625e-06 : (-3.814697265625e-06));
            }
            vec4 _2062 = textureGather(s_MatTexture, texCoord, 0);

            vec2 _1707 = fract(_1796 + vec2(0.5, 0.5));
            bool b1 = _1707.y > 0.5;
            vec2 _1708 = vec2(b1 ? _2062.x : _2062.w, 
                              b1 ? _2062.y : _2062.z);
            ivec2 _1710 = ivec2(clamp(vec2(_1707.x - 0.083333335, _1707.x + 0.083333335) * 2.0, vec2(0.0, 0.0), vec2(1.0, 1.0)));
            normal.x = _1708[_1710.x] - _1708[_1710.y];

            bool b2 = _1707.x > 0.5;
            _1708 = vec2(b2 ? _2062.z : _2062.w, 
                         b2 ? _2062.y : _2062.x);
            _1710 = ivec2(clamp(vec2(_1707.y - 0.083333335, _1707.y + 0.083333335) * 2.0, vec2(0.0, 0.0), vec2(1.0, 1.0)));
            normal.y = _1708[_1710.x] - _1708[_1710.y];

            normal.z = 0.25;

            normal = normalize(normal);
            normal.xy *= _2030;
        }
    }

    vec4 vanillaLight = texture2D(s_LightMapTexture, min(BlockSkyAmbientContribution.xy * v_lightmapUV.xy, vec2(1.0, 1.0)));
    float metallic;
    float emissive;
    float roughness;
    if ((pbrData.flags & kPBRTextureDataFlagHasMaterialTexture) == kPBRTextureDataFlagHasMaterialTexture) {
        vec4 merTex = texture2D(s_MatTexture, fma(v_texcoord0, colourToMaterialUvScale, colourToMaterialUvBias));
        metallic = merTex.x;
        emissive = merTex.y;
        roughness = merTex.z;
    } else {
        metallic = pbrData.uniformMetalness;
        emissive = pbrData.uniformEmissive;
        roughness = pbrData.uniformRoughness;
    }

    vec3 worldPos = v_worldPos.xyz;
    mat3 tbn = mtxFromCols(normalize(v_tangent), normalize(v_bitangent), normalize(v_normal));
    vec3 GNormal = normalize(mul(tbn, normal));
    float rGNormalManhattanLength = 1.0f / (abs(GNormal.x) + abs(GNormal.y) + abs(GNormal.z));
    float NX = rGNormalManhattanLength * GNormal.x;
    float NY = rGNormalManhattanLength * GNormal.y;
    bool isDownFace = GNormal.z < 0.0;
    vec3 modelCamPos = ViewPositionAndTime.xyz - worldPos;
    float camDis = length(modelCamPos);
    vec3 viewDir = normalize(-modelCamPos);

    ivec3 intGNormal = ivec3(fma(GNormal, vec3(0.5, 0.5, 0.5), vec3(0.5, 0.5, 0.5)) * 1023.0) & ivec3(1023, 1023, 1023);

    gl_FragData[0].xyz = sqrt(albedo.xyz);  // FUCK YOU MOJANG gamma 2.0
    gl_FragData[0].w = metallic;

    gl_FragData[1].x = isDownFace ? ((1.0f - abs(NY)) * ((NX >= 0.0f) ? 1.0f : (-1.0f))) : NX;
    gl_FragData[1].y = isDownFace ? ((1.0f - abs(NX)) * ((NY >= 0.0f) ? 1.0f : (-1.0f))) : NY;
    gl_FragData[1].zw = 0.0f;

    gl_FragData[2].xy = emissive;
    gl_FragData[2].z = dot(vec3(0.299, 0.587, 0.114), vec3(vanillaLight.xyz));
    gl_FragData[2].w = roughness;

    gl_FragData[3].x = intBitsToFloat((((0 ^ (intGNormal.x << 22)) ^ (intGNormal.y << 18)) ^ (intGNormal.z << 14)) + int(dot(GNormal, worldPos)));
    gl_FragData[3].yzw = 0.0f;

    gl_FragData[4].xyz = worldPos;
    gl_FragData[4].w = camDis;

    gl_FragData[5].xyz = worldPos;
    gl_FragData[5].w = camDis;

    gl_FragData[6].xyz = viewDir;
    gl_FragData[6].w = float(
#ifndef SEASONS
        (albedo.a < 0.8) ||
#endif
        ((metallic == 1.0) && (roughness < 0.01)));  // is specular

#else

    #if defined(DEPTH_ONLY) || defined(DEPTH_ONLY_OPAQUE)
        albedo = vec4(1.0, 1.0, 1.0, 1.0);
    #endif

    gl_FragData[0].rgb = mix(albedo.rgb, FogColor.rgb, v_fog.a);
    gl_FragData[0].a = albedo.a;
    gl_FragData[1] = vec4(0.0, 0.0, 0.0, 0.0);
    gl_FragData[2] = vec4(0.0, 0.0, 0.0, 0.0);
    gl_FragData[3] = vec4(0.0, 0.0, 0.0, 0.0);
    gl_FragData[4] = vec4(0.0, 0.0, 0.0, 0.0);
    gl_FragData[5] = vec4(0.0, 0.0, 0.0, 0.0);
    gl_FragData[6] = vec4(0.0, 0.0, 0.0, 0.0);
#endif
}
