package api.dto;


import com.fasterxml.jackson.annotation.JsonProperty;

public record AuthRequest(@JsonProperty("Username") String username, @JsonProperty("Pw") String password) {}
