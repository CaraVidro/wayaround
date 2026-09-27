#version 150
uniform sampler2D SceneSampler;
uniform vec2 ScreenSize;
uniform float Time;
uniform float Strength;
in vec2 lensUV;
out vec4 fragColor;
void main() {
    vec2 p = lensUV * 2.0 - 1.0;
    float radius = length(p);
    if (radius >= 1.0) discard;
    float envelope = (1.0 - smoothstep(0.72, 1.0, radius)) * Strength;
    vec2 uv = gl_FragCoord.xy / ScreenSize;
    float t = Time * 0.07;
    vec2 ripple = vec2(sin(p.y * 29.0 - t * 1.7 + sin(p.x * 13.0 + t)),
                       cos(p.x * 23.0 + t * 1.3 + sin(p.y * 17.0 - t)));
    vec2 offset = ripple * envelope * 2.5 / ScreenSize;
    fragColor = vec4(texture(SceneSampler, clamp(uv + offset, vec2(0.001), vec2(0.999))).rgb, 1.0);
}
