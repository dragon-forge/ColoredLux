vec3 mixLights(int colMix, vec3 mcLight, vec3 luxLight) {
//    if (colMix == 2) outLight = mcLight * luxLight;
//    else if (colMix == 1) outLight = mcLight + luxLight; // More washed-out, but more physically correct
//    else outLight = max(mcLight, luxLight); // Vivid but unrealistic
//    return outLight;
    return mcLight + luxLight;
}