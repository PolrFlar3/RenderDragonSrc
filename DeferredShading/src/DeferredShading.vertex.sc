$input a_color0, a_normal, a_position, a_texcoord0
$output v_color0, v_projPosition, v_texcoord0, v_viewSpaceNormal

#include <bgfx_shader.sh>

void main() {
    vec2 pos = (a_position.xy * 2.0) - 1.0;

    gl_Position = vec4(pos, a_position.z, 1.0);
    v_projPosition = vec3(pos, a_position.z);
    v_color0 = a_color0;
    v_texcoord0 = a_texcoord0;
    v_viewSpaceNormal = mul(u_modelView, vec4(a_normal.xyz, 0.0)).xyz;
}
