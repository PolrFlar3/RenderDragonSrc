$input v_color0
#if defined(GEOMETRY_PREPASS)
    $input v_texcoord0, v_normal, v_worldPos
#endif

#include <bgfx_shader.sh>

void main() {
#if defined(OPAQUE)
    //Opaque
    gl_FragColor = v_color0;
#elif defined(GEOMETRY_PREPASS)
    //GeometryPrepass
    vec3 GNormal = vec3(0.0, 1.0, 0.0);
    float rGNormalManhattanLength = 1.0f / (abs(GNormal.x) + abs(GNormal.y) + abs(GNormal.z));
    float NX = rGNormalManhattanLength * GNormal.x;
    float NY = rGNormalManhattanLength * GNormal.y;
    bool isDownFace = GNormal.z < 0.0;
    ivec3 intGNormal = ivec3(fma(GNormal, vec3(0.5, 0.5, 0.5), vec3(0.5, 0.5, 0.5)) * 1023.0) & ivec3(1023, 1023, 1023);

    gl_FragData[0].xyz = sqrt(v_color0.xyz);
    gl_FragData[0].w = 0.0;

    gl_FragData[1].x = isDownFace ? ((1.0f - abs(NY)) * ((NX >= 0.0f) ? 1.0f : (-1.0f))) : NX;
    gl_FragData[1].y = isDownFace ? ((1.0f - abs(NX)) * ((NY >= 0.0f) ? 1.0f : (-1.0f))) : NY;
    gl_FragData[1].zw = 0.0f;

    gl_FragData[2] = vec4(1.0, 1.0, 0.0, 0.5);

    gl_FragData[3].x = intBitsToFloat((((0 ^ (intGNormal.x << 22)) ^ (intGNormal.y << 18)) ^ (intGNormal.z << 14)) + int(dot(GNormal, v_worldPos)));
    gl_FragData[3].yzw = 0.0f;

    gl_FragData[4].xyz = v_worldPos;
    gl_FragData[4].w = length(-v_worldPos);

    gl_FragData[5].xyz = v_worldPos;
    gl_FragData[5].w = length(-v_worldPos);

    gl_FragData[6].xyz = normalize(v_worldPos);
    gl_FragData[6].w = float(v_color0.a < 0.8);
#else
    //Fallback
    gl_FragColor = vec4(0.0, 0.0, 0.0, 0.0);
#endif
}