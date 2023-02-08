$input v_color0, v_projPosition, v_texcoord0, v_viewSpaceNormal

#include <bgfx_shader.sh>
#include <bgfx_compute.sh>

struct LightCluster {
    int start;
    int count;
};

struct Light {
    vec4 position;
    vec4 color;
};

struct LightData {
    float lookup;
};

struct LightSourceWorldInfo {
    vec4 worldSpaceDirection;
    vec4 diffuseColorAndIntensity;
};

uniform mat4 SunShadowProj0;
uniform mat4 SunShadowProj1;
uniform mat4 SunShadowProj2;
uniform mat4 SunShadowProj3;
uniform mat4 MoonShadowProj0;
uniform mat4 MoonShadowProj1;
uniform mat4 MoonShadowProj2;
uniform mat4 MoonShadowProj3;
uniform vec4 ShadowResolutions;
uniform vec4 ShadowBias;
uniform vec4 ShadowSlopeBias;
uniform vec4 ShadowPCFWidth;
uniform vec4 ShadowParams;
uniform vec4 ClusterNearFar;
uniform vec4 ClusterDimensions;
uniform vec4 AmbientLightEnabled;
uniform vec4 AmbientLightColorIntensity;
uniform vec4 AmbientLightContributionMultiplier;
uniform vec4 DirectionalShadowsEnabled;
uniform vec4 DirectionalLightCount;
uniform vec4 EmissiveLightEnabled;
uniform vec4 EmissiveMultiplier;
uniform vec4 EmissiveDesaturation;
uniform vec4 SpecularLightEnabled;
uniform vec4 DiffuseLightEnabled;
uniform vec4 PointLightContributionEnabled;

SAMPLER2DARRAYSHADOW(s_SunShadowCascades, 0);
SAMPLER2DARRAYSHADOW(s_MoonShadowCascades, 2);
SAMPLER2D(s_ColorMetalness, 3);
SAMPLER2D(s_Normal, 4);
SAMPLER2D(s_EmissiveAmbientLinearRoughness, 5);
SAMPLER2D(s_SceneDepth, 6);
BUFFER_RO(s_LightClusters, LightCluster, 1);
BUFFER_RO(s_Lights, Light, 8);
BUFFER_RO(s_LightLookupArray, LightData, 9);
BUFFER_RO(s_DirectionalLightSources, LightSourceWorldInfo, 10);

void main() {
#if !defined(FALLBACK) && (BGFX_SHADER_LANGUAGE_HLSL > 400 || BGFX_SHADER_LANGUAGE_PSSL || BGFX_SHADER_LANGUAGE_SPIRV || BGFX_SHADER_LANGUAGE_METAL)
    float directionalShadowsEnabled          = DirectionalShadowsEnabled.x;
    float pointLightContributionEnabled      = PointLightContributionEnabled.x;
    float ambientLightEnabled                = AmbientLightEnabled.x;
    float specularLightEnabled               = SpecularLightEnabled.x;
    float diffuseLightEnabled                = DiffuseLightEnabled.x;
    float emissiveLightEnabled               = EmissiveLightEnabled.x;
    int   directionalLightCount              = int(DirectionalLightCount.x);
    float emissiveMultiplier                 = EmissiveMultiplier.x;
    float emissiveDesaturation               = EmissiveDesaturation.x;
    float ambientLightContributionMultiplier = AmbientLightContributionMultiplier.x;

    vec4 colorMetalness = texture2D(s_ColorMetalness, v_texcoord0);
    vec3 _160_161_162 = colorMetalness.rgb * colorMetalness.rgb;

    vec4 normal = texture2D(s_Normal, v_texcoord0);
    float _170 = 1.0 - abs(normal.x);
    float _172 = abs(normal.y);
    float _173 = _170 - _172;
    bool is_negative = _173 < 0.0;
    float _180 = ((normal.x >= 0.0) && (normal.y >= 0.0)) ? 1.0 : (-1.0);
    float _184 = is_negative ? ((1.0 - _172) * _180) : normal.x;
    float _185 = is_negative ? (_170 * _180) : normal.y;

    vec3 _184_185_173 = vec3(_184, _185, _173);
    vec3 _190_191_192 = normalize(_184_185_173);
    vec3 _197_198_199 = normalize(_190_191_192);
    vec3 _202_205_208 = mul(u_view, vec4(_197_198_199, 0)).xyz;
    vec3 _213_214_215 = normalize(_202_205_208);

    vec4 emissiveAmbientLinearRoughness = texture2D(s_EmissiveAmbientLinearRoughness, v_texcoord0);
    vec4 sceneDepth = texture2D(s_SceneDepth, v_texcoord0);
    
    float tmp1 = mul(u_invProj, vec4(v_projPosition.x, 0.0, 0.0, 0.0)).x;
    float tmp2 = mul(u_invProj, vec4(0.0, v_projPosition.y, 0.0, 0.0)).y;
    float tmp3 = mul(u_invProj, vec4(0.0, 0.0, 0.0, 1.0)).z;
    float tmp4 = mul(u_invProj, vec4(0.0, 0.0, sceneDepth.x, 1.0)).w;
    vec3 tmp1234 = vec3(tmp1, tmp2, tmp3) / tmp4;

    vec3 _239_243_247 = mul(u_invView, vec4(tmp1234, 1)).xyz;
    vec3 _231_234_248 = -(tmp1234);
    vec3 _253_254_255 = normalize(_231_234_248);

    float ambientIntensity = AmbientLightColorIntensity.x * AmbientLightColorIntensity.w * emissiveAmbientLinearRoughness.z * ambientLightContributionMultiplier * ambientLightEnabled;
    vec3 _273_274_275 = ambientIntensity * _160_161_162;
    
    vec3 finalColor;
    if (emissiveAmbientLinearRoughness.x < 0.25) {
        float _296 = log2(ClusterNearFar.y / ClusterNearFar.x) * 0.69314718;
        int clusterIndex = int(floor((((floor(((log2(_231_234_248.z) * 0.69314718) * (ClusterDimensions.z / _296)) - (((ClusterDimensions.z * 0.69314718) * log2(ClusterNearFar.x)) / _296)) * ClusterDimensions.y) + floor(ClusterDimensions.y * (1.0 - v_texcoord0.y))) * ClusterDimensions.x) + floor(ClusterDimensions.x * v_texcoord0.x)));

        vec3 _363_365_367 = vec3(0.0, 0.0, 0.0);
        if (clusterIndex < floor(ClusterDimensions.x * ClusterDimensions.y * ClusterDimensions.z)) {
            LightCluster lightCluster = s_LightClusters[clusterIndex];
            if (lightCluster.count > 0) {
                vec3 _462_463_464 = vec3(0.0, 0.0, 0.0);
                vec3 _364_366_368;
                for (int i = lightCluster.start; i < lightCluster.count + lightCluster.start; i++) {
                    int lookup = int(s_LightLookupArray[i].lookup);
                    Light light = s_Lights[lookup];

                    vec3 _515_517_519 = mul(u_view, vec4(light.position.xyz, 1)).xyz - tmp1234;
                    vec3 _524_525_526 = normalize(_515_517_519);
                    float _532 = length(_515_517_519);
                    float _534 = light.color.w / (_532 * _532);
                    vec3 _542_543_544 = normalize(_524_525_526 + _253_254_255);
                    float _545 = emissiveAmbientLinearRoughness.w * emissiveAmbientLinearRoughness.w;
                    float _415 = _545 * _545;
                    float _550 = max(dot(_213_214_215, _542_543_544), 0.0);
                    float _554 = ((_550 * _550) * (_415 - 1.0)) + 1.0;
                    float _562 = max(dot(_213_214_215, _253_254_255), 0.0);
                    float _566 = max(dot(_213_214_215, _524_525_526), 0.0);
                    float _567 = _545 * 0.5;
                    float _569 = 1.0 - _567;
                    float _571 = _567 + 9.9999997473787516355514526367188e-05;
                    float _578 = dot(_253_254_255, _542_543_544);
                    vec3 _585_586_587 = (_160_161_162 - 0.039999999105930328369140625) * colorMetalness.w;
                    float _594 = 1.0 - max(_578, 0.0);
                    float _595 = max(_594, 0.0);
                    float _600 = exp2(log2(min(_595, 1.0)) * 5.0);
                    vec3 _604_605_606 = (_585_586_587 + 0.039999999105930328369140625) + (_600 * (0.959999978542327880859375 - _585_586_587));
                    float _607 = ((_566 / (_571 + (_566 * _569))) * (_562 / (_571 + (_562 * _569)))) * (_415 / ((_554 * _554) * 3.14159265));
                    float _613 = ((_562 * 4.0) * _566) + 9.9999997473787516355514526367188e-05;
                    float _630 = (1.0 - colorMetalness.w) * 0.3183098733425140380859375;
                    
                    _364_366_368 = ((_534 * light.color.xyz) * ((((_630 * _160_161_162) * (1.0 - _604_605_606)) * diffuseLightEnabled) + (specularLightEnabled * ((_607 * _604_605_606) / _613)))) + _462_463_464;
                    _462_463_464 = _364_366_368;
                }
                _363_365_367 = _364_366_368;
            }
        }

        vec3 _388_390_392;
        if (directionalLightCount > 0) {
            vec3 _389_391_393;
            vec3 _401_402_403 = _273_274_275;
            for (int _404 = 0; _404 != directionalLightCount; _404++) {
                float _545 = emissiveAmbientLinearRoughness.w * emissiveAmbientLinearRoughness.w;
                float _415 = _545 * _545;
                float _416 = _415 - 1.0;
                float _420 = max(dot(_213_214_215, _253_254_255), 0.0);
                float _421 = emissiveAmbientLinearRoughness.w + 1.0;
                float _423 = (_421 * _421) * 0.125;
                float _425 = 1.0 - _423;
                float _427 = _423 + 9.9999997473787516355514526367188e-05;
                float _430 = _420 / (_427 + (_420 * _425));
                vec3 _435_436_437 = (_160_161_162 + (-0.039999999105930328369140625)) * colorMetalness.w;
                vec3 _438_440_441 = _435_436_437 + 0.039999999105930328369140625;
                vec3 _442_444_445 = 0.959999978542327880859375 - _435_436_437;
                float _446 = _420 * 4.0;
                float _457 = (1.0 - colorMetalness.w) * 0.3183098733425140380859375;
                vec3 _459_460_461 = _457 * _160_161_162;

                LightSourceWorldInfo lightSourceWorldInfo = s_DirectionalLightSources[_404];
                vec3 frontier_phi_16_pred_1;
                if (directionalShadowsEnabled >= 2.0) {
                    vec3 _671_675_679 = mul(u_view, lightSourceWorldInfo.worldSpaceDirection).xyz;
                    vec3 _684_685_686 = normalize(_671_675_679);
                    float _706 = dot(_213_214_215, _684_685_686);
                    float _709 = max(_706, 0.0);
                    vec3 _710_711_712 = _684_685_686 + _253_254_255;
                    vec3 _717_718_719 = normalize(_710_711_712);
                    float _720 = dot(_213_214_215, _717_718_719);
                    float _723 = max(_720, 0.0);
                    float _726 = (_723 * _723 * _416) + 1.0;
                    float _734 = dot(_253_254_255, _717_718_719);
                    float _738 = 1.0 - max(_734, 0.0);
                    float _739 = max(_738, 0.0);
                    float _743 = exp2(log2(min(_739, 1.0)) * 5.0);
                    vec3 _747_748_749 = _438_440_441 + (_743 * _442_444_445);
                    float _750 = ((_709 / (_427 + (_709 * _425))) * _430) * (_415 / ((_726 * _726) * 3.14159265));
                    float _755 = (_446 * _709) + 9.9999997473787516355514526367188e-05;

                    vec3 _1084_1085_1086;
                    int sunShadowMapLevel;
                    for (int i1 = 0; i1 < 4; i1++) {
                        mat4 sunShadowProj;
                        switch (i1) {
                            default:
                            case 0:
                                sunShadowProj = SunShadowProj0;
                                break;
                            case 1:
                                sunShadowProj = SunShadowProj1;
                                break;
                            case 2:
                                sunShadowProj = SunShadowProj2;
                                break;
                            case 3:
                                sunShadowProj = SunShadowProj3;
                                break;
                        }

                        vec4 tmp = mul(sunShadowProj, vec4(_239_243_247, 1));
                        _1084_1085_1086 = tmp.xyz / tmp.w;

                        vec3 _1093_1094_1095 = clamp(_1084_1085_1086, -1.0, 1.0) - _1084_1085_1086;
                        if (length(_1093_1094_1095) == 0.0) {
                            sunShadowMapLevel = i1;
                            break;
                        } else if (i1 >= 3) {
                            sunShadowMapLevel = -1;
                            break;
                        }
                    }

                    vec3 _1276_1277_1278;
                    int moonShadowMapLevel;
                    for (int i2 = 0; i2 < 4; i2++) {
                        mat4 moonShadowProj;
                        switch (i2) {
                            default:
                            case 0:
                                moonShadowProj = MoonShadowProj0;
                                break;
                            case 1:
                                moonShadowProj = MoonShadowProj1;
                                break;
                            case 2:
                                moonShadowProj = MoonShadowProj2;
                                break;
                            case 3:
                                moonShadowProj = MoonShadowProj3;
                                break;
                        }

                        vec4 tmp = mul(moonShadowProj, vec4(_239_243_247, 1));
                        _1276_1277_1278 = tmp.xyz / tmp.w;

                        vec3 _1285_1286_1287 = clamp(_1276_1277_1278, -1.0, 1.0) - _1276_1277_1278;
                        if (length(_1285_1286_1287) == 0.0) {
                            moonShadowMapLevel = i2;
                            break;
                        } else if (i2 >= 3) {
                            moonShadowMapLevel = -1;
                            break;
                        }
                    }

                    float _1361 = 0.0;
                    if (sunShadowMapLevel != -1 && ShadowParams.z > 0.0) {
                        float _1332 = tan(acos(_709));
                        float _1333 = max(_1332, 0.0);
                        float _1340 = (_1084_1085_1086.x * 0.5) + 0.5;
                        float _1341 = 0.5 - (_1084_1085_1086.y * 0.5);
                        float _1337 = (_1084_1085_1086.z - ShadowBias[sunShadowMapLevel]) - (min(_1333, 1.0) * ShadowSlopeBias[sunShadowMapLevel]);
                        float pcfWidth = clamp(ShadowPCFWidth[sunShadowMapLevel] + 0.5, 1, 9);
                        float pcfHalfWidth = pcfWidth / 2;
                        float sum = 0.0;
                        for (int i = 0; i < int(pcfWidth) && i < 9; i++) {
                            for (int j = 0; j < int(pcfWidth) && j < 9; j++) {
                                sum += shadow2DArray(s_SunShadowCascades, vec4((_1340 + ((j - pcfHalfWidth + 0.5) * ShadowParams.x)) * ShadowResolutions[sunShadowMapLevel], (_1341 + (ShadowParams.x * (i - pcfHalfWidth + 0.5))) * ShadowResolutions[sunShadowMapLevel], sunShadowMapLevel, _1337)).x;
                            }
                        }
                        _1361 += sum / float(pcfWidth * pcfWidth);
                    }

                    float _1426 = _1361;
                    if (moonShadowMapLevel != -1 && ShadowParams.w > 0.0)
                    {
                        float _1398 = tan(acos(_709));
                        float _1399 = max(_1398, 0.0);
                        float _1406 = (_1276_1277_1278.x * 0.5) + 0.5;
                        float _1407 = 0.5 - (_1276_1277_1278.y * 0.5);
                        float _1403 = (_1276_1277_1278.z - ShadowBias[moonShadowMapLevel]) - (((min(_1399, 1.0))) * ShadowSlopeBias[moonShadowMapLevel]);
                        float pcfWidth = clamp(ShadowPCFWidth[moonShadowMapLevel] + 0.5, 1, 9);
                        float pcfHalfWidth = pcfWidth / 2;
                        float sum = 0.0;
                        for (int i = 0; i < int(pcfWidth) && i < 9; i++) {
                            for (int j = 0; j < int(pcfWidth) && j < 9; j++) {
                                sum += (ShadowParams.w * shadow2DArray(s_MoonShadowCascades, vec4((_1406 + ((j - pcfHalfWidth + 0.5) * ShadowParams.x)) * ShadowResolutions[moonShadowMapLevel], (_1407 + (ShadowParams.x * (i - pcfHalfWidth + 0.5))) * ShadowResolutions[moonShadowMapLevel], moonShadowMapLevel, _1403)).x);
                            }
                        }
                        _1426 += sum / float(pcfWidth * pcfWidth);
                    }

                    float _1431 = ShadowParams.y - 8.0;
                    float _1433 = max(0.0, _1431);
                    float _1436 = (_231_234_248.z - _1433) / (ShadowParams.y - _1433);
                    float _1767 = max(_1436, 0.0);
                    float _1437 = min(_1767, 1.0);
                    float _1445 = (((_1437 * _1437) * (1.0 - _1426)) * (3.0 - (_1437 * 2.0))) + _1426;
                    
                    frontier_phi_16_pred_1 = ((lightSourceWorldInfo.diffuseColorAndIntensity.w * lightSourceWorldInfo.diffuseColorAndIntensity.x) * (((_459_460_461 * (1.0 - _747_748_749)) * diffuseLightEnabled) + (specularLightEnabled * ((_750 * _747_748_749) / _755)))) * _1445;
                } else {
                    vec3 _795_799_803 = mul(u_view, lightSourceWorldInfo.worldSpaceDirection).xyz;
                    vec3 _808_809_810 = normalize(_795_799_803);
                    float _830 = dot(_213_214_215, _808_809_810);
                    float _833 = max(_830, 0.0);
                    vec3 _542_543_544 = normalize(_808_809_810 + _253_254_255);
                    float _844 = dot(_213_214_215, _542_543_544);
                    float _847 = max(_844, 0.0);
                    float _850 = ((_847 * _847) * _416) + 1.0;
                    float _858 = dot(_253_254_255, _542_543_544);
                    float _862 = 1.0 - max(_858, 0.0);
                    float _863 = max(_862, 0.0);
                    float _867 = exp2(log2(min(_863, 1.0)) * 5.0);
                    vec3 _871_872_873 = _438_440_441 + (_867 * _442_444_445);
                    float _874 = ((_833 / (_427 + (_833 * _425))) * _430) * (_415 / ((_850 * _850) * 3.14159265));
                    float _879 = (_446 * _833) + 9.9999997473787516355514526367188e-05;

                    frontier_phi_16_pred_1 = ((lightSourceWorldInfo.diffuseColorAndIntensity.w * lightSourceWorldInfo.diffuseColorAndIntensity.x) * _833) * (((_459_460_461 * (1.0 - _871_872_873)) * diffuseLightEnabled) + (specularLightEnabled * ((_874 * _871_872_873) / _879)));
                }
                _389_391_393 = frontier_phi_16_pred_1 + _401_402_403;
                _401_402_403 = _389_391_393;
            }
            _388_390_392 = _389_391_393;
        } else {
            _388_390_392 = _273_274_275;
        }
        finalColor = (pointLightContributionEnabled * _363_365_367) + _388_390_392;
    } else {
        float _327 = dot(_160_161_162, vec3(0.2125999927520751953125, 0.715200006961822509765625, 0.072200000286102294921875));
        float _347 = emissiveAmbientLinearRoughness.x * emissiveLightEnabled * emissiveMultiplier;
        finalColor = (_347 * (((_327 - _160_161_162) * emissiveDesaturation) + _160_161_162)) + _273_274_275;
    }
    gl_FragColor = vec4(finalColor, 1.0);
#else
    gl_FragColor = vec4(0.0, 0.0, 0.0, 0.0);
#endif
}