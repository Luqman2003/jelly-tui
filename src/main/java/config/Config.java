package config;

import com.fasterxml.jackson.annotation.JsonProperty;

public record Config(@JsonProperty("userId") String userId,
                     @JsonProperty("username") String username,
                     @JsonProperty("accessToken") String accessToken,
                     @JsonProperty("serverUrl") String serverUrl
) {}
