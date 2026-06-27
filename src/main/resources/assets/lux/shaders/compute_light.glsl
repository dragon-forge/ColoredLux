void computeColor(vec3 position, vec3 norm) {
    vec3 accum = vec3(0.0);

    float total = 0.0;
    float maxI = 0.0;

    for (int i = 0; i < LIGHT_COUNT; ++i) {
        LIGHT_STRUCT l = getLight(i);

        vec3 toLight = l.position - position;
        float dist = length(toLight);

        vec3 lightDir = toLight / dist;

        // Lambert diffuse term
        float NdotL = max(dot(norm, lightDir), 0.25);

        // Distance attenuation
        float t = 1.0 - dist / l.radius;
        float intensity = t * t * NdotL;

        accum += l.color.rgb * intensity;
        total += intensity;
        maxI = max(maxI, intensity);
    }

    if (total > 0.0) {
        accum /= total;
    }

    LIGHT_COLOR = vec4(accum, 1.0);
    LIGHT_INTENSITY = min(1.0, maxI);
}

void computeColor(vec3 position) {
    vec3 accum = vec3(0.0);
    float total = 0.0;
    float maxI = 0.0;
    for (int i = 0; i < LIGHT_COUNT; ++i) {
        LIGHT_STRUCT l = getLight(i);
        float dist = distance(l.position, position);
        float t = max(0.0, 1.0 - dist / l.radius);
        float intensity = t * t;
        accum += l.color.rgb * intensity;
        total += intensity;
        maxI = max(maxI, intensity);
    }

    if (total > 0.0) {
        accum /= total;
    }

    LIGHT_COLOR = vec4(accum, 1.0);
    LIGHT_INTENSITY = min(1.0, maxI);
}

void computeColor_legacy(vec3 position) {
    float sumR = 0;
    float sumG = 0;
    float sumB = 0;
    float count = 0;
    float maxIntens = 0;
    float totalIntens = 0;

    for (int i = 0; i < LIGHT_COUNT; i++)
    {
        LIGHT_STRUCT l = getLight(i);
        float intensity = pow(max(0, 1.0f - distance(l.position, position) / l.radius), 2);
        totalIntens += intensity;
        maxIntens = max(maxIntens, intensity);
    }

    for (int i = 0; i < LIGHT_COUNT; i++)
    {
        LIGHT_STRUCT l = getLight(i);
        float intensity = pow(max(0, 1.0f - distance(l.position, position) / l.radius), 2);
        sumR += l.color.r * (intensity / totalIntens);
        sumG += l.color.g * (intensity / totalIntens);
        sumB += l.color.b * (intensity / totalIntens);
    }

    vec3 accum = max(vec3(sumR, sumG, sumB), 0.0);

    LIGHT_COLOR = vec4(accum, 1.0);
    LIGHT_INTENSITY = min(1.0, maxIntens);
}