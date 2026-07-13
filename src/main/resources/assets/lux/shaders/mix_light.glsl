vec3 mixLights(int colMix, vec3 mcLight, vec3 luxLight) {
//    vec3 outLight = mcLight;

    switch (colMix) {
        case 2:
            return dot(mcLight, vec3(0.299, 0.587, 0.114)) * luxLight;
        case 1:
            return mcLight + luxLight;
        default:
            return max(mcLight, luxLight);
    }

//    if (colMix == 2) outLight = mcLight * luxLight;
//    else if (colMix == 1) outLight = mcLight + luxLight; // More washed-out, but more physically correct
//    else outLight = max(mcLight, luxLight); // Vivid but unrealistic
//    return outLight;
}